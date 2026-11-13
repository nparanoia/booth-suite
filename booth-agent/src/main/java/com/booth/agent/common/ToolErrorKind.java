package com.booth.agent.common;

/**
 * 工具失败分类。决定这一轮还能不能再调。
 * PARAM 允许换参数再试一次；AUTH / REJECTED 立即停；TIMEOUT 额外重试有限次。
 */
public enum ToolErrorKind {
    PARAM,
    AUTH,
    REJECTED,
    TIMEOUT,
    NOT_ALLOWED,
    SAME_ARGS,
    MAX_STEPS
}
