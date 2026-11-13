package com.booth.product.config;

import com.booth.product.service.ProductViewCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

@Configuration
public class ProductCacheListenerConfig {

    @Bean
    public RedisMessageListenerContainer productCacheListener(RedisConnectionFactory factory, ProductViewCache cache) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener((message, pattern) ->
                        cache.onRemote(new String(message.getBody(), StandardCharsets.UTF_8)),
                new ChannelTopic(ProductViewCache.CHANNEL));
        return container;
    }
}
