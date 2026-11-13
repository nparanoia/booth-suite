package com.booth.agent.common;

/**
 * 工具或商城调用失败。message 只放给模型/用户看的短句，不带堆栈。
 * replyReady 为 true 时，这句话已经可以直接回复，不再套「商城拒绝」前缀。
 */
public final class ToolExecutionException extends RuntimeException {

    private final ToolErrorKind kind;
    private final int code;
    private final boolean replyReady;

    private ToolExecutionException(ToolErrorKind kind, int code, String message, boolean replyReady) {
        super(message);
        this.kind = kind;
        this.code = code;
        this.replyReady = replyReady;
    }

    public static ToolExecutionException booth(ToolErrorKind kind, int code, String message) {
        return new ToolExecutionException(kind, code, message, false);
    }

    public static ToolExecutionException ready(ToolErrorKind kind, int code, String message) {
        return new ToolExecutionException(kind, code, message, true);
    }

    public ToolErrorKind kind() {
        return kind;
    }

    public int code() {
        return code;
    }

    public boolean replyReady() {
        return replyReady;
    }
}
