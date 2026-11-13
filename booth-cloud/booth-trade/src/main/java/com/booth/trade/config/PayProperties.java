package com.booth.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "booth.pay")
public record PayProperties(boolean mockEnabled, boolean allowDevSecret, Alipay alipay) {

    public PayProperties {
        if (alipay == null) {
            alipay = new Alipay(null, null, null, null, null);
        }
    }

    public record Alipay(String appId, String privateKey, String publicKey, String gateway, String notifyUrl) {
    }
}
