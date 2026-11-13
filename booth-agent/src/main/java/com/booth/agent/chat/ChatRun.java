package com.booth.agent.chat;

import com.booth.agent.common.Tokens;
import com.booth.agent.tool.RunSignal;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** 一次流式对话。同步接口用 {@link #detached()}，取消对它无效。 */
public final class ChatRun implements RunSignal {

    private final String id;
    private final String conversationId;
    private final String memberHash;
    private final boolean detach;
    private final AtomicBoolean cancelled = new AtomicBoolean();

    public ChatRun(String id, String conversationId, String memberHash) {
        this(id, conversationId, memberHash, false);
    }

    private ChatRun(String id, String conversationId, String memberHash, boolean detach) {
        this.id = id;
        this.conversationId = conversationId;
        this.memberHash = memberHash;
        this.detach = detach;
    }

    public static ChatRun detached() {
        return new ChatRun(null, null, null, true);
    }

    public static ChatRun start(String memberToken, String conversationId) {
        return new ChatRun(UUID.randomUUID().toString().replace("-", ""), conversationId,
                Tokens.fingerprint(memberToken));
    }

    public String id() {
        return id;
    }

    public String conversationId() {
        return conversationId;
    }

    public String memberHash() {
        return memberHash;
    }

    @Override
    public boolean cancelled() {
        return !detach && cancelled.get();
    }

    /** @return 这一次确实从进行中改成了已取消 */
    public boolean cancel() {
        if (detach) {
            return false;
        }
        return cancelled.compareAndSet(false, true);
    }
}
