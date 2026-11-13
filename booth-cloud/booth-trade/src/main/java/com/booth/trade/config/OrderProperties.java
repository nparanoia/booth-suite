package com.booth.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "booth.order")
public record OrderProperties(Duration payTimeout, Duration autoReceive) {

    public OrderProperties {
        if (payTimeout == null) {
            payTimeout = Duration.ofMinutes(30);
        }
        if (autoReceive == null) {
            autoReceive = Duration.ofDays(14);
        }
    }
}
