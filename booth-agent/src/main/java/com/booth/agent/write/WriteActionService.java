package com.booth.agent.write;

import com.fasterxml.jackson.databind.JsonNode;
import com.booth.agent.chat.ChatResponse;
import com.booth.agent.common.ApiException;
import com.booth.agent.common.Tokens;
import com.booth.agent.common.ToolErrorKind;
import com.booth.agent.common.ToolExecutionException;
import com.booth.agent.config.BoothAgentProperties;
import com.booth.agent.confirm.ConfirmService;
import com.booth.agent.confirm.ConfirmTicket;
import com.booth.agent.confirm.ParamFingerprints;
import com.booth.agent.intent.IntentType;
import com.booth.agent.store.StoreApi;
import com.booth.agent.session.SessionService;
import com.booth.agent.trace.TraceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 写操作统一出口：指纹去重 → 落 outbox → 调商城；成功可按指纹幂等回放。
 * 主路径为对话内直写；{@link #confirm} 仅保留给已废弃确认接口的兼容实现。
 */
@Slf4j
@Service
public class WriteActionService {

    private static final int MAX_ATTEMPTS = 3;

    private final ConfirmService confirmService;
    private final WriteActionRepository repository;
    private final StoreApi storeApi;
    private final SessionService sessionService;
    private final TraceService traceService;
    private final BoothAgentProperties properties;

    public WriteActionService(ConfirmService confirmService, WriteActionRepository repository, StoreApi storeApi,
                              SessionService sessionService, TraceService traceService,
                              BoothAgentProperties properties) {
        this.confirmService = confirmService;
        this.repository = repository;
        this.storeApi = storeApi;
        this.sessionService = sessionService;
        this.traceService = traceService;
        this.properties = properties;
    }

    /**
     * 对话内直写：校验已在工具层完成；此处负责熔断、指纹幂等与 outbox。
     */
    public DirectWriteResult executeDirect(String memberToken, String action, String orderSn, Long orderItemId,
                                           String reason, String description, String conversationId) {
        if (properties.agent().governance().writeCircuitOpen()) {
            throw ToolExecutionException.ready(ToolErrorKind.REJECTED, 50301,
                    "写操作暂时关闭（熔断），请稍后再试或致电店主");
        }
        String token = memberToken;
        String memberHash = Tokens.fingerprint(token);
        String fingerprint = ParamFingerprints.of(action, orderSn, orderItemId, reason, description);
        long started = System.nanoTime();

        Optional<WriteActionRecord> priorSuccess = repository.findSuccessByFingerprint(memberHash, fingerprint);
        if (priorSuccess.isPresent()) {
            DirectWriteResult replay = toDirectResult(priorSuccess.get(), true);
            traceService.recordConfirm(token, conversationId, argumentSummary(action, orderSn),
                    true, null, started);
            return replay;
        }

        String ticketId = "direct-" + UUID.randomUUID().toString().replace("-", "");
        WriteActionRecord row;
        try {
            row = repository.insertPending(ticketId, memberHash, action, fingerprint,
                    orderSn, orderItemId, conversationId, MAX_ATTEMPTS);
        } catch (DuplicateKeyException duplicate) {
            Optional<WriteActionRecord> raced = repository.findSuccessByFingerprint(memberHash, fingerprint);
            if (raced.isPresent()) {
                return toDirectResult(raced.get(), true);
            }
            throw duplicate;
        }

        WriteCommand command = new WriteCommand(ticketId, memberHash, action, orderSn, orderItemId,
                reason, description, conversationId, fingerprint);
        return executeDirectBody(token, command, row, started);
    }

    public ChatResponse confirm(String bearerToken, String ticketId) {
        if (properties.agent().governance().writeCircuitOpen()) {
            throw new ApiException(503, 50301, "写操作暂时关闭（熔断），请稍后再试或致电店主");
        }
        String token = Tokens.bearer(bearerToken);
        long started = System.nanoTime();

        Optional<WriteActionRecord> existing = repository.findByTicketId(ticketId);
        if (existing.isPresent() && existing.get().status() == WriteActionStatus.SUCCESS) {
            return successResponse(existing.get());
        }

        ConfirmTicket peeked;
        try {
            peeked = confirmService.require(token, ticketId);
        } catch (ApiException exception) {
            Optional<WriteActionRecord> after = repository.findByTicketId(ticketId);
            if (after.isPresent() && after.get().status() == WriteActionStatus.SUCCESS) {
                return successResponse(after.get());
            }
            log.info("confirm audit ticketId={} success=false code={} message={}",
                    ticketId, exception.getCode(), exception.getMessage());
            sessionService.recordAudit(token, ticketId, null, false, exception.getCode(), null, exception.getMessage());
            traceService.recordConfirm(token, conversationOf(ticketId), "ticketId=" + ticketId,
                    false, String.valueOf(exception.getCode()), started);
            throw exception;
        }

        String fingerprint = StringUtils.hasText(peeked.paramFingerprint())
                ? peeked.paramFingerprint()
                : ParamFingerprints.of(peeked.action(), peeked.orderSn(), peeked.orderItemId(),
                peeked.reason(), peeked.description());

        Optional<WriteActionRecord> priorSuccess =
                repository.findSuccessByFingerprint(peeked.memberTokenHash(), fingerprint);
        if (priorSuccess.isPresent()) {
            try {
                confirmService.consume(token, ticketId);
            } catch (ApiException ignored) {
                // 并发下可能已被核销
            }
            return successResponse(priorSuccess.get());
        }

        ConfirmTicket ticket;
        try {
            ticket = confirmService.consume(token, ticketId);
        } catch (ApiException exception) {
            Optional<WriteActionRecord> after = repository.findByTicketId(ticketId);
            if (after.isPresent() && after.get().status() == WriteActionStatus.SUCCESS) {
                return successResponse(after.get());
            }
            Optional<WriteActionRecord> again =
                    repository.findSuccessByFingerprint(peeked.memberTokenHash(), fingerprint);
            if (again.isPresent()) {
                return successResponse(again.get());
            }
            throw exception;
        }

        WriteActionRecord row;
        try {
            row = repository.insertPending(ticket.ticketId(), ticket.memberTokenHash(), ticket.action(), fingerprint,
                    ticket.orderSn(), ticket.orderItemId(), ticket.conversationId(), MAX_ATTEMPTS);
        } catch (DuplicateKeyException duplicate) {
            Optional<WriteActionRecord> raced = repository.findByTicketId(ticket.ticketId());
            if (raced.isPresent() && raced.get().status() == WriteActionStatus.SUCCESS) {
                return successResponse(raced.get());
            }
            row = raced.orElseThrow(() -> duplicate);
        }

        WriteCommand command = WriteCommand.fromTicket(ticket, fingerprint);
        try {
            DirectWriteResult result = executeDirectBody(token, command, row, started);
            return successChatFromDirect(command.action(), command.orderSn(), result);
        } catch (ToolExecutionException exception) {
            int http = exception.kind() == ToolErrorKind.AUTH ? 403 : 502;
            throw new ApiException(http, exception.code(), exception.getMessage());
        }
    }

    /** 待人工/自动对账的条数，供运营面板展示。 */
    public int needReconcileCount() {
        return (int) repository.findRetryable(100).stream()
                .filter(row -> row.status() == WriteActionStatus.NEED_RECONCILE
                        || row.status() == WriteActionStatus.FAILED)
                .count();
    }

    private DirectWriteResult executeDirectBody(String token, WriteCommand command, WriteActionRecord row,
                                                long started) {
        try {
            if ("APPLY_RETURN".equals(command.action())) {
                JsonNode data = storeApi.applyReturn(token, command.orderSn(), Map.of(
                        "orderItemId", command.orderItemId(),
                        "reason", command.reason(),
                        "description", command.description() == null ? "" : command.description()
                ));
                String returnId = returnIdOf(data);
                repository.markSuccess(row.id(), returnId);
                log.info("write audit action=APPLY_RETURN member={} orderSn={} ticketId={} success=true returnId={}",
                        Tokens.fingerprint(token), command.orderSn(), command.ticketId(), returnId);
                sessionService.recordAudit(token, command.ticketId(), command.asTicket(), true, 0, returnId, "售后已提交");
                sessionService.clearPending(token, command.conversationId());
                traceService.recordConfirm(token, command.conversationId(), "orderSn=" + command.orderSn(),
                        true, null, started);
                String reply = "售后已提交：订单 " + command.orderSn()
                        + (command.orderItemId() == null ? "" : " 明细 " + command.orderItemId())
                        + "，原因「" + (command.reason() == null ? "" : command.reason())
                        + "」，售后单号 " + returnId + "。可在「我的售后」查看进度。";
                return new DirectWriteResult(reply, false, returnId, row.id());
            }
            if ("CANCEL_ORDER".equals(command.action())) {
                storeApi.cancelOrder(token, command.orderSn());
                repository.markSuccess(row.id(), command.orderSn());
                log.info("write audit action=CANCEL_ORDER member={} orderSn={} ticketId={} success=true",
                        Tokens.fingerprint(token), command.orderSn(), command.ticketId());
                sessionService.recordAudit(token, command.ticketId(), command.asTicket(), true, 0, null, "订单已取消");
                sessionService.clearPending(token, command.conversationId());
                traceService.recordConfirm(token, command.conversationId(),
                        "orderSn=" + command.orderSn() + ",action=cancel", true, null, started);
                return new DirectWriteResult("订单 " + command.orderSn() + " 已取消。", false, command.orderSn(), row.id());
            }
            if ("CHANGE_ADDRESS".equals(command.action())) {
                Map<String, Object> body = parseReceiver(command.description());
                storeApi.updateReceiver(token, command.orderSn(), body);
                repository.markSuccess(row.id(), command.orderSn());
                log.info("write audit action=CHANGE_ADDRESS member={} orderSn={} ticketId={} success=true",
                        Tokens.fingerprint(token), command.orderSn(), command.ticketId());
                sessionService.recordAudit(token, command.ticketId(), command.asTicket(), true, 0, null, "收货地址已修改");
                sessionService.clearPending(token, command.conversationId());
                traceService.recordConfirm(token, command.conversationId(),
                        "orderSn=" + command.orderSn() + ",action=change_address", true, null, started);
                String name = String.valueOf(body.getOrDefault("receiverName", ""));
                String phone = String.valueOf(body.getOrDefault("receiverPhone", ""));
                String addr = body.getOrDefault("receiverProvince", "")
                        + String.valueOf(body.getOrDefault("receiverCity", ""))
                        + body.getOrDefault("receiverRegion", "")
                        + body.getOrDefault("receiverDetailAddress", "");
                String reply = "订单 " + command.orderSn() + " 的收货地址已更新为：" + name + " " + phone + " " + addr + "。";
                return new DirectWriteResult(reply, false, command.orderSn(), row.id());
            }
            repository.markFailed(row.id(), "不支持的写动作", false);
            throw ToolExecutionException.ready(ToolErrorKind.PARAM, 40003, "不支持的写动作");
        } catch (ToolExecutionException exception) {
            boolean needReconcile = exception.kind() == ToolErrorKind.TIMEOUT;
            repository.markFailed(row.id(), exception.getMessage(), needReconcile);
            log.info("write audit action={} member={} orderSn={} ticketId={} success=false code={} message={}",
                    command.action(), Tokens.fingerprint(token), command.orderSn(), command.ticketId(),
                    exception.code(), exception.getMessage());
            sessionService.recordAudit(token, command.ticketId(), command.asTicket(), false, exception.code(), null,
                    exception.getMessage());
            sessionService.clearPending(token, command.conversationId());
            traceService.recordConfirm(token, command.conversationId(), argumentSummary(command.action(), command.orderSn()),
                    false, String.valueOf(exception.code()), started);
            String verb = verbOf(command.action());
            String message = exception.replyReady()
                    ? exception.getMessage()
                    : verb + "失败：" + exception.getMessage();
            if (needReconcile) {
                message = message + "（已进入对账，请稍后在售后/订单中核对）";
            }
            throw ToolExecutionException.ready(exception.kind(), exception.code(), message);
        }
    }

    private static ChatResponse successChatFromDirect(String action, String orderSn, DirectWriteResult result) {
        if ("CANCEL_ORDER".equals(action)) {
            return new ChatResponse(result.reply(), IntentType.CANCEL_ORDER.name(), null, null, List.of(),
                    Map.of("orderSn", orderSn, "cancelled", true, "outboxId", result.outboxId()));
        }
        if ("CHANGE_ADDRESS".equals(action)) {
            return new ChatResponse(result.reply(), IntentType.CHANGE_ADDRESS.name(), null, null, List.of(),
                    Map.of("orderSn", orderSn, "addressUpdated", true, "outboxId", result.outboxId()));
        }
        return new ChatResponse(
                "【售后·完成】售后已提交，单号：" + result.resultRef() + "。可在「我的售后」中查看进度。",
                IntentType.AFTER_SALE.name(),
                null,
                null,
                List.of(),
                Map.of("returnId", result.resultRef() == null ? "-" : result.resultRef(),
                        "orderSn", orderSn, "outboxId", result.outboxId())
        );
    }

    private DirectWriteResult toDirectResult(WriteActionRecord row, boolean replay) {
        String reply;
        if ("CANCEL_ORDER".equals(row.action())) {
            reply = "订单 " + row.orderSn() + " 已取消。";
        } else if ("CHANGE_ADDRESS".equals(row.action())) {
            reply = "订单 " + row.orderSn() + " 的收货地址已更新。";
        } else {
            String returnId = row.resultRef() == null ? "-" : row.resultRef();
            reply = "售后已提交：订单 " + row.orderSn()
                    + (row.orderItemId() == null ? "" : " 明细 " + row.orderItemId())
                    + "，售后单号 " + returnId + "。可在「我的售后」查看进度。";
            if (replay) {
                reply = reply + "（与此前提交相同，未重复建单）";
            }
        }
        if (replay && !"APPLY_RETURN".equals(row.action())) {
            reply = reply + "（与此前提交相同，未重复办理）";
        }
        return new DirectWriteResult(reply, replay, row.resultRef(), row.id());
    }

    private ChatResponse successResponse(WriteActionRecord row) {
        DirectWriteResult direct = toDirectResult(row, true);
        return successChatFromDirect(row.action(), row.orderSn(), direct);
    }

    private String conversationOf(String ticketId) {
        var audit = sessionService.findAudit(ticketId);
        if (audit == null || audit.conversationId() == null) {
            return ticketId;
        }
        return audit.conversationId();
    }

    private static String argumentSummary(String action, String orderSn) {
        if ("CANCEL_ORDER".equals(action)) {
            return "orderSn=" + orderSn + ",action=cancel";
        }
        if ("CHANGE_ADDRESS".equals(action)) {
            return "orderSn=" + orderSn + ",action=change_address";
        }
        return "orderSn=" + orderSn;
    }

    private static String verbOf(String action) {
        return switch (action == null ? "" : action) {
            case "CANCEL_ORDER" -> "取消订单";
            case "CHANGE_ADDRESS" -> "修改地址";
            default -> "提交售后";
        };
    }

    /** description 存 name|phone|province|city|region|detail */
    static Map<String, Object> parseReceiver(String description) {
        String raw = description == null ? "" : description;
        String[] parts = raw.split("\\|", -1);
        if (parts.length < 6) {
            throw ToolExecutionException.ready(ToolErrorKind.PARAM, 400, "地址信息不完整，请重新发起改地址");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("receiverName", parts[0]);
        body.put("receiverPhone", parts[1]);
        body.put("receiverProvince", parts[2]);
        body.put("receiverCity", parts[3]);
        body.put("receiverRegion", parts[4]);
        body.put("receiverDetailAddress", parts[5]);
        return body;
    }

    private static String returnIdOf(JsonNode data) {
        if (data == null || data.isNull()) {
            return "unknown";
        }
        if (data.isNumber() || data.isTextual()) {
            return data.asText();
        }
        if (data.isObject() && data.path("id").canConvertToLong()) {
            return data.path("id").asText();
        }
        return "unknown";
    }

    /** 内部写命令；asTicket 仅用于审计落库兼容。 */
    private record WriteCommand(
            String ticketId,
            String memberHash,
            String action,
            String orderSn,
            Long orderItemId,
            String reason,
            String description,
            String conversationId,
            String fingerprint
    ) {
        static WriteCommand fromTicket(ConfirmTicket ticket, String fingerprint) {
            return new WriteCommand(ticket.ticketId(), ticket.memberTokenHash(), ticket.action(), ticket.orderSn(),
                    ticket.orderItemId(), ticket.reason(), ticket.description(), ticket.conversationId(), fingerprint);
        }

        ConfirmTicket asTicket() {
            return new ConfirmTicket(ticketId, memberHash, action, orderSn, orderItemId, reason, description,
                    null, conversationId, fingerprint);
        }
    }
}
