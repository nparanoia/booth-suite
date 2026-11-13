package com.booth.agent.governance;

import com.booth.agent.common.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserRateLimiterTest {

    @Test
    void blocksWhenOverPerMinute() {
        InMemoryUserRateLimiter limiter = new InMemoryUserRateLimiter(5, 2);
        limiter.acquire("u1").close();
        limiter.acquire("u1").close();
        ApiException ex = assertThrows(ApiException.class, () -> limiter.acquire("u1"));
        assertEquals(42901, ex.getCode());
    }

    @Test
    void blocksConcurrent() {
        InMemoryUserRateLimiter limiter = new InMemoryUserRateLimiter(1, 20);
        UserRateLimiter.Lease first = limiter.acquire("u2");
        ApiException ex = assertThrows(ApiException.class, () -> limiter.acquire("u2"));
        assertEquals(42902, ex.getCode());
        first.close();
        limiter.acquire("u2").close();
    }
}
