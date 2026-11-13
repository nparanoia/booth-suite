package com.booth.trade.pay;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.booth.trade.config.PayProperties;
import com.booth.trade.entity.TradeOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${booth.pay.alipay.app-id:}')")
public class AlipayGateway implements PaymentGateway {

    private static final String CHARSET = "UTF-8";
    private static final String SIGN = "RSA2";

    private final PayProperties.Alipay config;
    private final AlipayClient client;
    private final ObjectMapper objectMapper;

    public AlipayGateway(PayProperties properties, ObjectMapper objectMapper) {
        this.config = properties.alipay();
        this.objectMapper = objectMapper;
        this.client = new DefaultAlipayClient(config.gateway(), config.appId(), config.privateKey(),
                "json", CHARSET, config.publicKey(), SIGN);
    }

    @Override
    public String createPay(TradeOrder order) {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(config.notifyUrl());
        request.setBizContent(json(Map.of(
                "out_trade_no", order.getOrderSn(),
                "total_amount", order.getPayAmount().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                "subject", "订单" + order.getOrderSn(),
                "product_code", "FAST_INSTANT_TRADE_PAY")));
        try {
            return client.pageExecute(request).getBody();
        } catch (AlipayApiException e) {
            throw new IllegalStateException("创建支付宝订单失败", e);
        }
    }

    public boolean verify(Map<String, String> params) {
        if (!StringUtils.hasText(config.appId()) || !config.appId().equals(params.get("app_id"))) {
            return false;
        }
        try {
            return AlipaySignature.rsaCheckV1(new LinkedHashMap<>(params), config.publicKey(), CHARSET, SIGN);
        } catch (AlipayApiException e) {
            log.warn("支付宝验签异常", e);
            return false;
        }
    }

    public Optional<QueriedPay> query(String orderSn) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent(json(Map.of("out_trade_no", orderSn)));
        try {
            AlipayTradeQueryResponse response = client.execute(request);
            if (!response.isSuccess() || response.getTotalAmount() == null) {
                return Optional.empty();
            }
            return Optional.of(new QueriedPay(response.getTradeStatus(), new BigDecimal(response.getTotalAmount()), response.getTradeNo()));
        } catch (AlipayApiException e) {
            log.warn("查询支付宝订单失败，orderSn={}", orderSn, e);
            return Optional.empty();
        }
    }

    private String json(Map<String, String> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public record QueriedPay(String tradeStatus, BigDecimal amount, String channelTradeNo) {
    }
}
