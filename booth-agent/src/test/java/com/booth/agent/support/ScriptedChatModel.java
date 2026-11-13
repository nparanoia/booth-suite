package com.booth.agent.support;

import com.booth.agent.llm.LlmClient;
import com.booth.agent.llm.ModelMessage;
import com.booth.agent.llm.ModelReply;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public final class ScriptedChatModel implements LlmClient {

    public boolean forbid;
    public int calls;
    public final List<List<ModelMessage>> seen = new ArrayList<>();
    private final Queue<ModelReply> replies = new ArrayDeque<>();

    public ScriptedChatModel enqueue(ModelReply reply) {
        replies.add(reply);
        return this;
    }

    @Override
    public ModelReply complete(List<ModelMessage> messages, List<Map<String, Object>> tools) {
        if (forbid) {
            throw new AssertionError("规则模式不应调用模型");
        }
        calls++;
        seen.add(List.copyOf(messages));
        if (replies.isEmpty()) {
            return new ModelReply("模型结束", List.of());
        }
        return replies.remove();
    }
}
