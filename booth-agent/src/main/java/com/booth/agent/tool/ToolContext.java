package com.booth.agent.tool;

public final class ToolContext {

    private final String memberToken;
    private final String conversationId;
    private final String message;
    private final RunSignal run;
    private final ToolEvents events;

    public ToolContext(String memberToken, String conversationId, String message) {
        this(memberToken, conversationId, message, RunSignal.NONE, ToolEvents.QUIET);
    }

    public ToolContext(String memberToken, String conversationId, String message, RunSignal run, ToolEvents events) {
        this.memberToken = memberToken;
        this.conversationId = conversationId;
        this.message = message;
        this.run = run == null ? RunSignal.NONE : run;
        this.events = events == null ? ToolEvents.QUIET : events;
    }

    public String memberToken() {
        return memberToken;
    }

    public String conversationId() {
        return conversationId;
    }

    public String message() {
        return message;
    }

    public RunSignal run() {
        return run;
    }

    public ToolEvents events() {
        return events;
    }
}
