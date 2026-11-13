package com.booth.product.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSearchDocServiceTest {

    @Test
    void versionUsesShanghaiEpochMillis() {
        LocalDateTime time = LocalDateTime.of(2026, 1, 1, 8, 0, 0);
        long expected = time.atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
        assertThat(ProductSearchDocService.epoch(time)).isEqualTo(expected);
        assertThat(ProductSearchDocService.epoch(null)).isEqualTo(1L);
    }
}
