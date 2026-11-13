package com.booth.agent.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.booth.agent.audit.ChatTranscriptRepository;
import com.booth.agent.audit.ConfirmAuditRepository;
import com.booth.agent.chat.ChatResponse;
import com.booth.agent.chat.ChatViews;
import com.booth.agent.common.ApiException;
import com.booth.agent.common.Tokens;
import com.booth.agent.config.BoothAgentProperties;
import com.booth.agent.confirm.ConfirmService;
import com.booth.agent.confirm.ConfirmStore;
import com.booth.agent.confirm.ConfirmTicket;
import com.booth.agent.intent.IntentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 最近 10 轮原文和当前步骤放在同一套存储里。更早的轮次压成摘要。测试用内存，运行时是 Redis。
 */
@Slf4j
@Service
public class SessionService {

    public static final int WINDOW = 10;
    private static final Duration AUDIT_TTL = Duration.ofDays(7);
    private static final String SESSION_PREFIX = "booth:agent:session:";
    private static final String AUDIT_PREFIX = "booth:agent:audit:";
    private static final int TEXT_LIMIT = 500;

    private final ConfirmStore store;
    private final ObjectMapper objectMapper;
    private final ConfirmService confirmService;
    private final ConfirmAuditRepository auditRepository;
    private final ChatTranscriptRepository transcriptRepository;
    private final BoothAgentProperties properties;

    public SessionService(ConfirmStore store, ObjectMapper objectMapper, ConfirmService confirmService) {
        this(store, objectMapper, confirmService, null, null, null);
    }

    @Autowired
    public SessionService(ConfirmStore store, ObjectMapper objectMapper, ConfirmService confirmService,
                          ConfirmAuditRepository auditRepository,
                          ChatTranscriptRepository transcriptRepository,
                          BoothAgentProperties properties) {
        this.store = store;
        this.objectMapper = objectMapper;
        this.confirmService = confirmService;
        this.auditRepository = auditRepository;
        this.transcriptRepository = transcriptRepository;
        this.properties = properties;
    }

    public ConversationMemory load(String memberToken, String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return null;
        }
        String json = store.get(sessionKey(memberToken, conversationId));
        if (json == null) {
            return null;
        }
        ConversationMemory memory = read(json, ConversationMemory.class);
        if (!Tokens.fingerprint(memberToken).equals(memory.memberHash())) {
            return null;
        }
        Duration idle = sessionIdle();
        if (memory.touchedAt() != null && memory.touchedAt().plus(idle).isBefore(Instant.now())) {
            ConversationMemory reset = memory.resetAfterIdle();
            store.save(sessionKey(memberToken, conversationId), write(reset), idle);
            return reset;
        }
        return memory;
    }

    public ChatResponse replayPending(String memberToken, ConversationMemory memory) {
        if (memory == null || memory.step() != ConversationStep.AWAIT_CONFIRM || memory.ticketId() == null) {
            return null;
        }
        ConfirmTicket ticket;
        try {
            ticket = confirmService.require(memberToken, memory.ticketId());
        } catch (ApiException exception) {
            if (exception.getCode() == 40001 || exception.getCode() == 40002) {
                return null;
            }
            throw exception;
        }
        String summary;
        String intent;
        if ("CANCEL_ORDER".equals(ticket.action())) {
            summary = "上一张确认卡还在，待付款订单 " + ticket.orderSn()
                    + " 还没有取消。确认后才会提交到商城。";
            intent = IntentType.CANCEL_ORDER.name();
        } else if ("CHANGE_ADDRESS".equals(ticket.action())) {
            summary = "上一张确认卡还在，订单 " + ticket.orderSn()
                    + " 的改地址尚未提交。确认后才会写到商城。";
            intent = IntentType.CHANGE_ADDRESS.name();
        } else {
            summary = "【售后·步骤3/3 待确认】上一张确认卡还在，订单 " + ticket.orderSn() + " 的明细 "
                    + ticket.orderItemId() + " 还没有提交。确认后才会提交到商城。";
            intent = IntentType.AFTER_SALE.name();
        }
        ChatResponse.ConfirmCard card = new ChatResponse.ConfirmCard(
                ticket.ticketId(), ticket.action(), ticket.orderSn(), ticket.orderItemId(), ticket.reason(), summary);
        return ChatViews.build("rule", intent, memory.conversationId(),
                summary + "\n请调用确认接口，ticketId=" + ticket.ticketId(),
                card, List.of(), List.of(), "CONFIRM_REQUIRED");
    }

    public void remember(String memberToken, String conversationId, String userText, ChatResponse response) {
        ConversationMemory current = load(memberToken, conversationId);
        String orderSn = current == null ? null : current.orderSn();
        ConversationStep step = current == null ? ConversationStep.CHAT : current.step();
        String ticketId = current == null ? null : current.ticketId();
        Long orderItemId = current == null ? null : current.orderItemId();
        String reason = current == null ? null : current.reason();
        String learned = orderSnFrom(response);
        if (learned != null) {
            orderSn = learned;
        }
        if (response.confirm() != null) {
            step = ConversationStep.AWAIT_CONFIRM;
            ticketId = response.confirm().ticketId();
            orderItemId = response.confirm().orderItemId();
            reason = response.confirm().reason();
            orderSn = response.confirm().orderSn();
        } else if (waitingForOrderSn(response)) {
            step = ConversationStep.NEED_ORDER_SN;
            ticketId = null;
            orderItemId = null;
            reason = null;
        } else {
            step = ConversationStep.CHAT;
            ticketId = null;
            orderItemId = null;
            reason = null;
        }
        List<StoredTurn> turns = new ArrayList<>(current == null ? List.of() : current.turns());
        turns.add(new StoredTurn("user", clip(userText)));
        turns.add(new StoredTurn("assistant", clip(response.reply())));
        String priorSummary = current == null ? null : current.summary();
        SessionSummarizer.Compact compact = SessionSummarizer.compact(priorSummary, turns, WINDOW * 2);
        Instant now = Instant.now();
        ConversationMemory memory = new ConversationMemory(
                conversationId, Tokens.fingerprint(memberToken), step, orderSn, ticketId, orderItemId, reason,
                compact.summary(), compact.turns(), now);
        store.save(sessionKey(memberToken, conversationId), write(memory), sessionIdle());
        persistTranscript(memberToken, conversationId, userText, response, orderSn, now);
    }

    public void recordAudit(String memberToken, String ticketId, ConfirmTicket ticket, boolean success,
                            int code, String returnId, String message) {
        ActionLog existing = findAudit(ticketId);
        if (existing != null && existing.success() && !success) {
            return;
        }
        Instant now = Instant.now();
        ActionLog log = new ActionLog(
                ticketId,
                Tokens.fingerprint(memberToken),
                ticket == null ? "APPLY_RETURN" : ticket.action(),
                ticket == null ? (existing == null ? null : existing.orderSn()) : ticket.orderSn(),
                ticket == null ? (existing == null ? null : existing.orderItemId()) : ticket.orderItemId(),
                ticket == null ? (existing == null ? null : existing.conversationId()) : ticket.conversationId(),
                success,
                code,
                returnId,
                message,
                now);
        store.save(AUDIT_PREFIX + ticketId, write(log), AUDIT_TTL);
        if (auditRepository != null) {
            auditRepository.upsert(
                    log.ticketId(), log.member(), log.action(), log.orderSn(), log.orderItemId(),
                    log.conversationId(), log.success(), log.code(), log.returnId(), log.message(), now);
        }
    }

    public ActionLog findAudit(String ticketId) {
        String json = store.get(AUDIT_PREFIX + ticketId);
        if (json == null) {
            return null;
        }
        return read(json, ActionLog.class);
    }

    public void clearPending(String memberToken, String conversationId) {
        ConversationMemory memory = load(memberToken, conversationId);
        if (memory == null || memory.step() != ConversationStep.AWAIT_CONFIRM) {
            return;
        }
        store.save(sessionKey(memberToken, conversationId), write(memory.clearedPending()), sessionIdle());
    }

    /**
     * 入口带单：覆盖当前会话订单号，并清掉未确认票（换单不应沿用旧确认卡）。
     */
    public void bindOrderSn(String memberToken, String conversationId, String orderSn) {
        if (conversationId == null || conversationId.isBlank() || orderSn == null || orderSn.isBlank()) {
            return;
        }
        ConversationMemory current = load(memberToken, conversationId);
        ConversationMemory memory = new ConversationMemory(
                conversationId,
                Tokens.fingerprint(memberToken),
                ConversationStep.CHAT,
                orderSn.trim(),
                null,
                null,
                null,
                current == null ? null : current.summary(),
                current == null ? List.of() : current.turns(),
                Instant.now());
        store.save(sessionKey(memberToken, conversationId), write(memory), sessionIdle());
    }

    private void persistTranscript(String memberToken, String conversationId, String userText,
                                   ChatResponse response, String orderSn, Instant at) {
        if (transcriptRepository == null) {
            return;
        }
        try {
            String hash = Tokens.fingerprint(memberToken);
            String tools = toolNames(response);
            transcriptRepository.append(conversationId, hash, "user", userText, response.intent(), orderSn, null, at);
            transcriptRepository.append(conversationId, hash, "assistant",
                    response.reply() == null ? "" : response.reply(),
                    response.intent(), orderSn, tools, at);
        } catch (RuntimeException exception) {
            log.warn("chat transcript persist failed conversationId={}: {}",
                    conversationId, exception.getMessage());
        }
    }

    private static String toolNames(ChatResponse response) {
        if (response.debug() == null) {
            return null;
        }
        Object calls = response.debug().get("toolCalls");
        if (!(calls instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map && map.get("name") != null) {
                if (!builder.isEmpty()) {
                    builder.append(',');
                }
                builder.append(map.get("name"));
            }
        }
        return builder.isEmpty() ? null : builder.toString();
    }

    private Duration sessionIdle() {
        if (properties == null || properties.agent() == null || properties.agent().support() == null) {
            return Duration.ofHours(24);
        }
        return properties.agent().support().sessionIdle();
    }

    private static String sessionKey(String memberToken, String conversationId) {
        return SESSION_PREFIX + Tokens.fingerprint(memberToken) + ":" + conversationId;
    }

    private static String orderSnFrom(ChatResponse response) {
        if (response.confirm() != null && response.confirm().orderSn() != null && !response.confirm().orderSn().isBlank()) {
            return response.confirm().orderSn();
        }
        if (response.debug() == null) {
            return null;
        }
        Object calls = response.debug().get("toolCalls");
        if (!(calls instanceof List<?> list)) {
            return null;
        }
        String found = null;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            Object arguments = map.get("arguments");
            if (!(arguments instanceof Map<?, ?> args)) {
                continue;
            }
            Object orderSn = args.get("orderSn");
            if (orderSn != null && !orderSn.toString().isBlank()) {
                found = orderSn.toString();
            }
        }
        return found;
    }

    private static boolean waitingForOrderSn(ChatResponse response) {
        if (response.intent() == null) {
            return false;
        }
        boolean afterSaleOrAddress = IntentType.AFTER_SALE.name().equals(response.intent())
                || IntentType.CHANGE_ADDRESS.name().equals(response.intent());
        if (!afterSaleOrAddress) {
            return false;
        }
        if (response.reply() != null && response.reply().contains("请提供订单号")) {
            return true;
        }
        if (response.debug() == null) {
            return false;
        }
        Object calls = response.debug().get("toolCalls");
        if (!(calls instanceof List<?> list)) {
            return false;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> map && "PARAM".equals(map.get("errorKind"))) {
                Object message = map.get("message");
                if (message != null && message.toString().contains("请提供订单号")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String clip(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() <= TEXT_LIMIT ? trimmed : trimmed.substring(0, TEXT_LIMIT);
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
