package com.booth.agent.chat;

import com.booth.agent.common.ApiException;
import com.booth.agent.common.Result;
import com.booth.agent.common.Tokens;
import com.booth.agent.governance.UserRateLimiter;
import com.booth.agent.trace.TraceService;
import com.booth.agent.trace.TurnTrace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@Tag(name = "智能客服")
@RestController
@RequestMapping("/app/chat")
public class ChatController {

    private final ChatOrchestrator chatOrchestrator;
    private final RunRegistry runs;
    private final TraceService traceService;
    private final Executor chatStreamExecutor;
    private final UserRateLimiter rateLimiter;

    public ChatController(ChatOrchestrator chatOrchestrator, RunRegistry runs, TraceService traceService,
                          @Qualifier("chatStreamExecutor") Executor chatStreamExecutor,
                          UserRateLimiter rateLimiter) {
        this.chatOrchestrator = chatOrchestrator;
        this.runs = runs;
        this.traceService = traceService;
        this.chatStreamExecutor = chatStreamExecutor;
        this.rateLimiter = rateLimiter;
    }

    @Operation(summary = "对话", description = "Authorization: Bearer {会员登录 token}，与 booth-cloud 网关同一套令牌")
    @PostMapping
    public Result<ChatResponse> chat(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                     @Valid @RequestBody ChatRequest request) {
        String token = Tokens.bearer(authorization);
        try (UserRateLimiter.Lease ignored = rateLimiter.acquire(token)) {
            return Result.ok(chatOrchestrator.chat("Bearer " + token, request));
        }
    }

    @Operation(summary = "流式对话", description = "SSE。先推 progress（正在查订单 / 正在检索），再推 token，最后 done。断开连接会取消后续生成。")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                             @Valid @RequestBody ChatRequest request) {
        String token = Tokens.bearer(authorization);
        UserRateLimiter.Lease lease = rateLimiter.acquire(token);
        String conversationId = request.conversationId() == null || request.conversationId().isBlank()
                ? java.util.UUID.randomUUID().toString().replace("-", "")
                : request.conversationId();
        ChatRequest fixed = new ChatRequest(request.message(), conversationId,
                request.contextOrderSn(), request.contextHint());
        ChatRun run = runs.start(token, conversationId);
        SseEmitter emitter = new SseEmitter(120_000L);
        emitter.onTimeout(() -> {
            run.cancel();
            lease.close();
        });
        emitter.onError(error -> {
            run.cancel();
            lease.close();
        });
        chatStreamExecutor.execute(() -> {
            SseChatListener listener = new SseChatListener(emitter, run);
            try {
                listener.started();
                ChatResponse response = chatOrchestrator.chat("Bearer " + token, fixed, run, listener);
                if (!run.cancelled()) {
                    listener.done(response);
                }
                completeQuietly(emitter);
            } catch (ApiException exception) {
                listener.failed(exception.getMessage());
                completeQuietly(emitter);
            } catch (Exception exception) {
                listener.failed("系统繁忙");
                completeQuietly(emitter);
            } finally {
                runs.finish(run.id());
                lease.close();
            }
        });
        return emitter;
    }

    @Operation(summary = "取消进行中的流式对话", description = "停止后续生成；已直写成功的商城操作不会回滚")
    @PostMapping("/cancel")
    public Result<Map<String, Object>> cancel(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                              @Valid @RequestBody CancelRequest request) {
        String token = Tokens.bearer(authorization);
        boolean cancelled = runs.cancel(token, request.runId());
        return Result.ok(Map.of("cancelled", cancelled));
    }

    @Operation(summary = "确认写操作（已废弃）", description = "写操作已改为对话内直接办理，无需确认卡")
    @PostMapping("/confirm")
    public Result<ChatResponse> confirm(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                        @Valid @RequestBody ConfirmRequest request) {
        Tokens.bearer(authorization);
        return Result.ok(new ChatResponse(
                "已改为在对话中直接办理退货/取消/改地址，无需再点确认。请重新发送办理意图。",
                "CHITCHAT",
                null,
                null,
                List.of(),
                Map.of("deprecatedConfirm", true, "ticketId", request.ticketId() == null ? "" : request.ticketId())
        ));
    }

    @Operation(summary = "查看会话轨迹", description = "按 conversationId 看这一轮走了哪个工具。参数摘要里的令牌和手机号已打码。")
    @GetMapping("/traces")
    public Result<List<TurnTrace>> traces(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                          @RequestParam String conversationId) {
        return Result.ok(traceService.find(Tokens.bearer(authorization), conversationId));
    }

    private static void completeQuietly(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // 客户端已经断开时，complete 可能再次失败
        }
    }
}
