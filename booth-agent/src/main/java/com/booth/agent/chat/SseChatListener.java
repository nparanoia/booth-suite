package com.booth.agent.chat;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.Map;

/** 把进度和正文分片写成 SSE。发送失败就取消这一轮，避免客户端走了还在继续吐字。 */
public final class SseChatListener implements ChatListener {

    private final SseEmitter emitter;
    private final ChatRun run;

    public SseChatListener(SseEmitter emitter, ChatRun run) {
        this.emitter = emitter;
        this.run = run;
    }

    public void started() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("runId", run.id());
        data.put("conversationId", run.conversationId());
        send("started", data);
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public void progress(String text) {
        send("progress", Map.of("text", text));
    }

    @Override
    public void token(String text) {
        send("token", Map.of("text", text));
    }

    @Override
    public void cancelled() {
        send("cancelled", Map.of("text", "已取消"));
    }

    public void done(ChatResponse response) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("conversationId", response.conversationId());
        data.put("reply", response.reply());
        data.put("intent", response.intent());
        data.put("confirm", response.confirm());
        data.put("sources", response.sources());
        send("done", data);
    }

    public void failed(String message) {
        send("error", Map.of("message", message));
    }

    private void send(String event, Object data) {
        if (run.cancelled() && !"cancelled".equals(event) && !"error".equals(event)) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
        } catch (Exception exception) {
            run.cancel();
        }
    }
}
