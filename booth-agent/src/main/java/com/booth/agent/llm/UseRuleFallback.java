package com.booth.agent.llm;

/** 业务问题模型没有选工具，交给规则分流，避免模型凭空回答订单或政策。 */
public class UseRuleFallback extends RuntimeException {

    public UseRuleFallback(String message) {
        super(message);
    }
}
