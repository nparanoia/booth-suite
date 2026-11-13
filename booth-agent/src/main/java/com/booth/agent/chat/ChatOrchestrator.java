package com.booth.agent.chat;

import com.booth.agent.common.ApiException;
import com.booth.agent.common.Tokens;
import com.booth.agent.config.BoothAgentProperties;
import com.booth.agent.llm.LlmToolLoop;
import com.booth.agent.llm.LlmUnavailableException;
import com.booth.agent.llm.UseRuleFallback;
import com.booth.agent.session.ConversationMemory;
import com.booth.agent.session.SessionService;
import com.booth.agent.session.TurnResolver;
import com.booth.agent.trace.TraceService;
import com.booth.agent.write.WriteActionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ChatOrchestrator {

    private final RuleChatService rules;
    private final LlmToolLoop llmToolLoop;
    private final BoothAgentProperties properties;
    private final SessionService sessionService;
    private final TraceService traceService;
    private final WriteActionService writeActionService;
    private final EntryOrderBinder entryOrderBinder;

    public ChatOrchestrator(RuleChatService rules, LlmToolLoop llmToolLoop,
                            BoothAgentProperties properties, SessionService sessionService,
                            TraceService traceService, WriteActionService writeActionService,
                            EntryOrderBinder entryOrderBinder) {
        this.rules = rules;
        this.llmToolLoop = llmToolLoop;
        this.properties = properties;
        this.sessionService = sessionService;
        this.traceService = traceService;
        this.writeActionService = writeActionService;
        this.entryOrderBinder = entryOrderBinder;
    }

    public ChatResponse chat(String bearerToken, ChatRequest request) {
        return chat(bearerToken, request, ChatRun.detached(), ChatListener.none());
    }

    public ChatResponse chat(String bearerToken, ChatRequest request, ChatRun run, ChatListener listener) {
        String token = Tokens.bearer(bearerToken);
        if (request == null || (!request.hasUserMessage() && !request.hasContextOrderSn())) {
            throw new ApiException(400, 40000, "请输入消息，或从订单页进入以关联订单");
        }
        String conversationId = request.conversationId() == null || request.conversationId().isBlank()
                ? UUID.randomUUID().toString().replace("-", "")
                : request.conversationId();
        ChatRun active = run == null ? ChatRun.detached() : run;
        ChatListener sink = listener == null ? ChatListener.none() : listener;
        long started = System.nanoTime();

        if (request.hasContextOrderSn() && !request.hasUserMessage()) {
            ChatResponse opened = entryOrderBinder.bindAndOpen(
                    token, conversationId, request.contextOrderSn(), request.contextHint());
            return publish(token, conversationId, EntryOrderBinder.BOOTSTRAP_USER_TEXT, active, sink, opened, started);
        }

        if (request.hasContextOrderSn()) {
            ChatResponse denied = entryOrderBinder.bindOnly(token, conversationId, request.contextOrderSn());
            if (denied != null) {
                return publish(token, conversationId, request.message(), active, sink, denied, started);
            }
        }

        ConversationMemory memory = sessionService.load(token, conversationId);
        if (TurnResolver.returnFollowUp(request.message())) {
            ChatResponse replay = sessionService.replayPending(token, memory);
            if (replay != null) {
                return publish(token, conversationId, request.message(), active, sink, replay, started);
            }
        }
        ChatResponse response;
        if (!properties.modelEnabled()) {
            response = rules.chat(token, request, conversationId, active, sink);
        } else {
            try {
                response = llmToolLoop.chat(token, request, conversationId, active, sink);
            } catch (LlmUnavailableException | UseRuleFallback exception) {
                log.warn("改走规则分流: {}", exception.getMessage());
                ChatResponse fallback = rules.chat(token, request, conversationId, active, sink);
                Map<String, Object> debug = new LinkedHashMap<>(fallback.debug());
                debug.put("llmFallback", true);
                debug.put("llmFallbackReason", exception.getMessage());
                response = new ChatResponse(fallback.reply(), fallback.intent(), fallback.conversationId(),
                        fallback.confirm(), fallback.sources(), debug);
            }
        }
        return publish(token, conversationId, request.message(), active, sink, response, started);
    }

    public ChatResponse confirm(String bearerToken, ConfirmRequest request) {
        // 写操作已改为对话内直接办理；保留接口以免旧客户端 404
        return new ChatResponse(
                "已改为在对话中直接办理退货/取消/改地址，无需再点确认。请重新发送办理意图。",
                "CHITCHAT",
                null,
                null,
                List.of(),
                Map.of("deprecatedConfirm", true,
                        "ticketId", request == null || request.ticketId() == null ? "" : request.ticketId())
        );
    }

    private ChatResponse publish(String token, String conversationId, String userText, ChatRun run,
                                 ChatListener listener, ChatResponse response, long startedNanos) {
        if (!listener.enabled()) {
            return finish(token, conversationId, userText, response, startedNanos);
        }
        String reply = response.reply() == null ? "" : response.reply();
        StringBuilder shown = new StringBuilder();
        boolean stopped = run.cancelled();
        if (!stopped) {
            for (String piece : ReplyChunks.split(reply)) {
                if (run.cancelled()) {
                    stopped = true;
                    break;
                }
                listener.token(piece);
                shown.append(piece);
            }
        }
        if (stopped || run.cancelled()) {
            listener.cancelled();
            String visible = shown.isEmpty() ? "已取消，未提交售后。" : shown.toString();
            Map<String, Object> debug = new LinkedHashMap<>();
            if (response.debug() != null) {
                debug.putAll(response.debug());
            }
            debug.put("stoppedReason", "CANCELLED");
            ChatResponse partial = new ChatResponse(visible, response.intent(), conversationId,
                    null, List.of(), debug);
            return finish(token, conversationId, userText, partial, startedNanos);
        }
        return finish(token, conversationId, userText, response, startedNanos);
    }

    private ChatResponse finish(String token, String conversationId, String userText, ChatResponse response,
                                long startedNanos) {
        sessionService.remember(token, conversationId, userText, response);
        ConversationMemory memory = sessionService.load(token, conversationId);
        Map<String, Object> debug = new LinkedHashMap<>();
        if (response.debug() != null) {
            debug.putAll(response.debug());
        }
        if (memory != null) {
            debug.put("sessionStep", memory.step().name());
            debug.put("sessionTurns", memory.turns().size() / 2);
            if (memory.summary() != null && !memory.summary().isBlank()) {
                debug.put("sessionSummary", true);
            }
            if (memory.orderSn() != null) {
                debug.put("rememberedOrderSn", memory.orderSn());
            }
        }
        ChatResponse done = new ChatResponse(response.reply(), response.intent(), conversationId,
                response.confirm(), response.sources(), debug);
        traceService.recordChat(token, done, startedNanos);
        return done;
    }
}
