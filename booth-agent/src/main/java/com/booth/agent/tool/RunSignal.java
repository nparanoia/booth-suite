package com.booth.agent.tool;

/** 这一轮还能不能继续。取消之后不再开始新的写操作。 */
public interface RunSignal {

    boolean cancelled();

    RunSignal NONE = () -> false;
}
