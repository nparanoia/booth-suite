package com.booth.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.booth.product.mapper.ProductMapper;
import com.booth.product.mapper.SkuMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 第二个实例不会连上 Redis 广播，这里直接调用它收到消息后的处理：只清本地缓存，下一次重新回源。
 */
class ProductViewCacheTest {

    @Test
    void remoteMessageDropsLocalDetail() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(values.get(anyString())).thenReturn(null);

        ProductViewCache cache = new ProductViewCache(redis, new ObjectMapper(),
                mock(ProductMapper.class), mock(SkuMapper.class));
        AtomicInteger loads = new AtomicInteger();
        cache.detail(8L, () -> {
            loads.incrementAndGet();
            return null;
        });
        cache.detail(8L, () -> {
            loads.incrementAndGet();
            return null;
        });
        assertThat(loads).hasValue(1);

        cache.onRemote("p:8");
        cache.detail(8L, () -> {
            loads.incrementAndGet();
            return null;
        });
        assertThat(loads).hasValue(2);
    }
}
