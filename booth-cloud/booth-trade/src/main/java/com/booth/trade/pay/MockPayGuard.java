package com.booth.trade.pay;

import com.booth.trade.config.PayProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 模拟支付默认关闭。打开之后如果还在用仓库里的默认密钥，必须显式允许，否则拒绝启动。
 */
@Component
@ConditionalOnProperty(name = "booth.pay.mock-enabled", havingValue = "true")
public class MockPayGuard {

    static final String DEV_SECRET = "dev-only-secret-change-me-0123456789abcdef";

    public MockPayGuard(@Value("${booth.security.secret}") String secret, PayProperties pay) {
        if (DEV_SECRET.equals(secret) && !pay.allowDevSecret()) {
            throw new IllegalStateException(
                    "模拟支付正在使用仓库中的默认密钥。本地开发请同时设置 BOOTH_PAY_ALLOW_DEV_SECRET=true；生产环境请关闭 BOOTH_PAY_MOCK_ENABLED 并更换 BOOTH_SECURITY_SECRET");
        }
    }
}
