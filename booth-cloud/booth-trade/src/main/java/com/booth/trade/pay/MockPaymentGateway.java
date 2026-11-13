package com.booth.trade.pay;

import com.booth.trade.entity.TradeOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@ConditionalOnProperty(name = "booth.pay.mock-enabled", havingValue = "true")
public class MockPaymentGateway implements PaymentGateway {

    public static String content(String orderSn, BigDecimal amount, String tradeStatus, String channelTradeNo, long timestamp) {
        return "pay|" + orderSn + "|" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
                + "|" + tradeStatus + "|" + channelTradeNo + "|" + timestamp;
    }

    @Override
    public String createPay(TradeOrder order) {
        return "mock://pay/" + order.getOrderSn();
    }
}
