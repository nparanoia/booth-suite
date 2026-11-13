package com.booth.common.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;

/**
 * 值以普通 JSON 字符串存储，读取时由调用方指定目标类型。
 * 不在 JSON 中写入类名（不启用 activateDefaultTyping），避免反序列化任意类型带来的安全风险，
 * 也让 Redis 中的数据可以被其他语言或命令行直接读取。
 */
public class RedisJsonCache {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisJsonCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void set(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, toJson(value), ttl);
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        return read(key, objectMapper.constructType(type));
    }

    public <T> Optional<T> get(String key, TypeReference<T> type) {
        return read(key, objectMapper.constructType(type));
    }

    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    public StringRedisTemplate template() {
        return redisTemplate;
    }

    private <T> Optional<T> read(String key, JavaType type) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(objectMapper.readValue(json, type));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Redis 缓存反序列化失败，key=" + key, e);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("对象无法序列化为 JSON：" + value.getClass().getName(), e);
        }
    }
}
