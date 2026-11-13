package com.booth.agent.governance;

import com.booth.agent.common.ApiException;
import com.booth.agent.common.Tokens;
import com.booth.agent.config.BoothAgentProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

public class RedisUserRateLimiter implements UserRateLimiter {

    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>();

    static {
        SCRIPT.setResultType(Long.class);
        SCRIPT.setScriptText(
                """
                        local inflightKey = KEYS[1]
                        local windowKey = KEYS[2]
                        local maxConcurrent = tonumber(ARGV[1])
                        local perMinute = tonumber(ARGV[2])
                        local now = tonumber(ARGV[3])
                        local member = ARGV[4]

                        redis.call('ZREMRANGEBYSCORE', windowKey, 0, now - 60000)
                        local count = redis.call('ZCARD', windowKey)
                        if count >= perMinute then
                          return -1
                        end

                        local inflight = tonumber(redis.call('GET', inflightKey) or '0')
                        if inflight >= maxConcurrent then
                          return -2
                        end

                        redis.call('INCR', inflightKey)
                        redis.call('EXPIRE', inflightKey, 120)
                        redis.call('ZADD', windowKey, now, member .. ':' .. now .. ':' .. math.random(1000000))
                        redis.call('EXPIRE', windowKey, 120)
                        return 1
                        """
        );
    }

    private final StringRedisTemplate redis;
    private final int maxConcurrent;
    private final int perMinute;

    public RedisUserRateLimiter(StringRedisTemplate redis, BoothAgentProperties properties) {
        this.redis = redis;
        var g = properties.agent().governance();
        this.maxConcurrent = g.maxConcurrentPerUser();
        this.perMinute = g.maxRequestsPerMinute();
    }

    @Override
    public Lease acquire(String memberToken) {
        String hash = Tokens.fingerprint(memberToken);
        String inflightKey = "booth:agent:rl:inflight:" + hash;
        String windowKey = "booth:agent:rl:window:" + hash;
        Long result = redis.execute(SCRIPT, List.of(inflightKey, windowKey),
                String.valueOf(maxConcurrent),
                String.valueOf(perMinute),
                String.valueOf(System.currentTimeMillis()),
                hash);
        if (result == null) {
            throw new ApiException(503, 50301, "限流服务暂不可用");
        }
        if (result == -1L) {
            throw new ApiException(429, 42901, "请求过于频繁，请稍后再试");
        }
        if (result == -2L) {
            throw new ApiException(429, 42902, "同一账号并发对话过多，请稍后再试");
        }
        return () -> redis.opsForValue().decrement(inflightKey);
    }
}
