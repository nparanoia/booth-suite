package com.booth.agent.governance;

/** 用户级对话配额：并发槽位 + 每分钟请求数。 */
public interface UserRateLimiter {

    /**
     * @throws com.booth.agent.common.ApiException 429 超限
     */
    Lease acquire(String memberToken);

    interface Lease extends AutoCloseable {
        Lease NOOP = () -> {
        };

        @Override
        void close();
    }
}
