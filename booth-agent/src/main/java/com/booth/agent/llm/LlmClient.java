package com.booth.agent.llm;

import java.util.List;
import java.util.Map;

/** 自管工具循环用的补全接口（单测 ScriptedChatModel / 兼容客户端）。 */
public interface LlmClient {

    ModelReply complete(List<ModelMessage> messages, List<Map<String, Object>> tools);
}
