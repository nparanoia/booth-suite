package com.booth.agent.governance;

import com.booth.agent.common.ApiException;
import com.booth.agent.common.Tokens;
import com.booth.agent.config.BoothAgentProperties;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** 单测与兜底限流。 */
public class InMemoryUserRateLimiter implements UserRateLimiter {

    private final int maxConcurrent;
    private final int perMinute;
    private final Map<String, AtomicInteger> inflight = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    public InMemoryUserRateLimiter(BoothAgentProperties properties) {
        var g = properties.agent().governance();
        this.maxConcurrent = g.maxConcurrentPerUser();
        this.perMinute = g.maxRequestsPerMinute();
    }

    public InMemoryUserRateLimiter(int maxConcurrent, int perMinute) {
        this.maxConcurrent = maxConcurrent;
        this.perMinute = perMinute;
    }

    @Override
    public Lease acquire(String memberToken) {
        String key = Tokens.fingerprint(memberToken);
        long now = System.currentTimeMillis();
        Deque<Long> window = windows.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() > 60_000L) {
                window.pollFirst();
            }
            if (window.size() >= perMinute) {
                throw new ApiException(429, 42901, "请求过于频繁，请稍后再试");
            }
            window.addLast(now);
        }
        AtomicInteger counter = inflight.computeIfAbsent(key, k -> new AtomicInteger());
        int current = counter.incrementAndGet();
        if (current > maxConcurrent) {
            counter.decrementAndGet();
            throw new ApiException(429, 42902, "同一账号并发对话过多，请稍后再试");
        }
        return () -> counter.decrementAndGet();
    }
}
