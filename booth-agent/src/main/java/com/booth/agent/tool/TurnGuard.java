package com.booth.agent.tool;

import java.util.HashSet;
import java.util.Set;

/** 一轮对话内的步数和失败签名。不跨请求共享。 */
public final class TurnGuard {

    private final int maxSteps;
    private int used;
    private int invalidStreak;
    private final Set<String> failed = new HashSet<>();

    public TurnGuard(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    public boolean tryAcquire() {
        if (used >= maxSteps) {
            return false;
        }
        used++;
        return true;
    }

    public int remaining() {
        return maxSteps - used;
    }

    public int maxSteps() {
        return maxSteps;
    }

    public boolean seen(String signature) {
        return failed.contains(signature);
    }

    public void markFailed(String signature) {
        failed.add(signature);
    }

    /** @return 这一次参数错误是否已经用掉「再试一次」的额度 */
    public boolean recordInvalid() {
        invalidStreak++;
        return invalidStreak >= 2;
    }

    public void recordSuccess() {
        invalidStreak = 0;
    }
}
