package com.booth.common.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration(after = {RedisAutoConfiguration.class, JacksonAutoConfiguration.class})
public class BoothRedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RedisJsonCache redisJsonCache(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        return new RedisJsonCache(stringRedisTemplate, objectMapper);
    }
}
