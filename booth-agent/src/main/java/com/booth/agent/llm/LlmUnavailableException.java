package com.booth.agent.llm;

/** 模型接口超时、鉴权失败或返回无法识别时，改走规则分流。 */
public class LlmUnavailableException extends RuntimeException {

    public LlmUnavailableException(String message) {
        super(message);
    }
}
