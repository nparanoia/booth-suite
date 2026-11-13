package com.booth.agent.governance;

import com.booth.agent.config.BoothAgentProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class GovernanceConfig {

    @Bean
    UserRateLimiter userRateLimiter(StringRedisTemplate redis, BoothAgentProperties properties) {
        return new RedisUserRateLimiter(redis, properties);
    }

    /** 仅当没有 Redis 模板时（极少）退回内存实现。 */
    @Bean
    @ConditionalOnMissingBean(UserRateLimiter.class)
    UserRateLimiter inMemoryUserRateLimiter(BoothAgentProperties properties) {
        return new InMemoryUserRateLimiter(properties);
    }
}
