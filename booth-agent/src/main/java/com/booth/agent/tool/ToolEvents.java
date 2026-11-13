package com.booth.agent.tool;

/** 工具开始前的进度。正文的分片由对话层负责。 */
public interface ToolEvents {

    void progress(String text);

    ToolEvents QUIET = text -> {
    };
}
