package com.booth.agent.confirm;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisConfirmStore implements ConfirmStore {

    private final StringRedisTemplate redis;

    @Override
    public void save(String key, String json, Duration ttl) {
        redis.opsForValue().set(key, json, ttl);
    }

    @Override
    public String get(String key) {
        return redis.opsForValue().get(key);
    }

    @Override
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redis.delete(key));
    }
}
