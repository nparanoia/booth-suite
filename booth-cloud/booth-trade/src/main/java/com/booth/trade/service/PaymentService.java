package com.booth.trade.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.common.core.exception.BizException;
import com.booth.common.core.security.RequestSigner;
import com.booth.trade.entity.PaymentRecord;
import com.booth.trade.entity.TradeOrder;
import com.booth.trade.enums.OrderStatus;
import com.booth.trade.enums.PaymentStatus;
import com.booth.trade.error.TradeErrorCode;
import com.booth.trade.mapper.PaymentRecordMapper;
import com.booth.trade.pay.AlipayGateway;
import com.booth.trade.pay.MockPaymentGateway;
import com.booth.trade.pay.PaymentGateway;
import com.booth.trade.support.TradeMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    public static final int PAY_TYPE_ALIPAY = 1;

    private final PaymentRecordMapper paymentRecordMapper;
    private final OrderLifecycleService lifecycleService;
    private final ObjectProvider<MockPaymentGateway> mockGateway;
    private final ObjectProvider<AlipayGateway> alipayGateway;
    private final RequestSigner requestSigner;
    private final TransactionTemplate transactionTemplate;
    private final TradeMetrics tradeMetrics;

    public String pay(Long memberId, String orderSn) {
        TradeOrder order = lifecycleService.requireMember(memberId, orderSn);
        BizException.throwIf(order.getStatus() != OrderStatus.WAIT_PAY, TradeErrorCode.ORDER_STATUS_CHANGED);
        ensureRecord(order);
        return gateway().createPay(order);
    }

    /**
     * 模拟收银台「确认付款」：仅 mock 渠道可用。真实支付宝走异步 notify，不经过此接口。
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmMockPay(Long memberId, String orderSn) {
        BizException.throwIf(mockGateway.getIfAvailable() == null, TradeErrorCode.PAY_CHANNEL_DISABLED);
        TradeOrder order = lifecycleService.requireMember(memberId, orderSn);
        BizException.throwIf(order.getStatus() != OrderStatus.WAIT_PAY, TradeErrorCode.ORDER_STATUS_CHANGED);
        String channelTradeNo = "MOCK-" + orderSn;
        String result = handleSuccess(orderSn, order.getPayAmount(), "TRADE_SUCCESS", channelTradeNo, PAY_TYPE_ALIPAY);
        BizException.throwIf(!"success".equals(result), TradeErrorCode.ORDER_STATUS_CHANGED);
    }

    public boolean verifyMock(String orderSn, BigDecimal amount, String tradeStatus, String channelTradeNo, long timestamp, String sign) {
        if (!RequestSigner.withinSkew(timestamp, System.currentTimeMillis() / 1000)) {
            return false;
        }
        String expected = requestSigner.signContent(MockPaymentGateway.content(orderSn, amount, tradeStatus, channelTradeNo, timestamp));
        return RequestSigner.matches(expected, sign);
    }

    /**
     * 先验签和金额，再在一个事务里改支付记录和订单。订单已关闭时只记待退款，不把订单改回已支付。
     */
    @Transactional(rollbackFor = Exception.class)
    public String handleSuccess(String orderSn, BigDecimal amount, String tradeStatus, String channelTradeNo, int payType) {
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            return "success";
        }
        TradeOrder order = lifecycleService.find(orderSn);
        if (order == null) {
            return "failure";
        }
        if (amount.compareTo(order.getPayAmount()) != 0) {
            log.warn("支付金额不符，orderSn={}，通知金额={}，订单金额={}", orderSn, amount, order.getPayAmount());
            return "failure";
        }
        PaymentRecord record = ensureRecord(order);
        if (record.getStatus() == PaymentStatus.SUCCESS) {
            return "success";
        }
        if (order.getStatus() == OrderStatus.CLOSED) {
            markRefundRequired(record, channelTradeNo);
            return "success";
        }
        markSuccess(record, channelTradeNo);
        boolean paid = lifecycleService.markPaid(orderSn, payType);
        if (!paid && lifecycleService.require(orderSn).getStatus() == OrderStatus.CLOSED) {
            markRefundRequired(record, channelTradeNo);
        }
        return "success";
    }

    public void queryPending() {
        AlipayGateway alipay = alipayGateway.getIfAvailable();
        if (alipay == null || mockGateway.getIfAvailable() != null) {
            return;
        }
        List<PaymentRecord> pending = paymentRecordMapper.selectList(Wrappers.<PaymentRecord>lambdaQuery()
                .eq(PaymentRecord::getStatus, PaymentStatus.WAIT_PAY)
                .lt(PaymentRecord::getCreateTime, LocalDateTime.now().minusMinutes(5))
                .last("LIMIT 50"));
        for (PaymentRecord record : pending) {
            alipay.query(record.getOrderSn()).ifPresent(queried -> transactionTemplate.executeWithoutResult(status ->
                    handleSuccess(record.getOrderSn(), queried.amount(), queried.tradeStatus(), queried.channelTradeNo(), PAY_TYPE_ALIPAY)));
        }
    }

    private PaymentGateway gateway() {
        MockPaymentGateway mock = mockGateway.getIfAvailable();
        if (mock != null) {
            return mock;
        }
        AlipayGateway alipay = alipayGateway.getIfAvailable();
        if (alipay != null) {
            return alipay;
        }
        throw new BizException(TradeErrorCode.PAY_CHANNEL_DISABLED);
    }

    private PaymentRecord ensureRecord(TradeOrder order) {
        PaymentRecord existing = paymentRecordMapper.selectOne(Wrappers.<PaymentRecord>lambdaQuery()
                .eq(PaymentRecord::getOrderSn, order.getOrderSn())
                .eq(PaymentRecord::getPayType, PAY_TYPE_ALIPAY));
        if (existing != null) {
            return existing;
        }
        PaymentRecord record = new PaymentRecord();
        record.setOrderSn(order.getOrderSn());
        record.setPayType(PAY_TYPE_ALIPAY);
        record.setAmount(order.getPayAmount());
        record.setStatus(PaymentStatus.WAIT_PAY);
        try {
            paymentRecordMapper.insert(record);
            return record;
        } catch (DuplicateKeyException e) {
            return paymentRecordMapper.selectOne(Wrappers.<PaymentRecord>lambdaQuery()
                    .eq(PaymentRecord::getOrderSn, order.getOrderSn())
                    .eq(PaymentRecord::getPayType, PAY_TYPE_ALIPAY));
        }
    }

    private void markSuccess(PaymentRecord record, String channelTradeNo) {
        paymentRecordMapper.update(null, Wrappers.<PaymentRecord>lambdaUpdate()
                .set(PaymentRecord::getStatus, PaymentStatus.SUCCESS)
                .set(PaymentRecord::getChannelTradeNo, channelTradeNo)
                .set(PaymentRecord::getNotifyTime, LocalDateTime.now())
                .eq(PaymentRecord::getId, record.getId())
                .eq(PaymentRecord::getStatus, PaymentStatus.WAIT_PAY));
    }

    private void markRefundRequired(PaymentRecord record, String channelTradeNo) {
        int updated = paymentRecordMapper.update(null, Wrappers.<PaymentRecord>lambdaUpdate()
                .set(PaymentRecord::getStatus, PaymentStatus.REFUND_REQUIRED)
                .set(PaymentRecord::getChannelTradeNo, channelTradeNo)
                .set(PaymentRecord::getNotifyTime, LocalDateTime.now())
                .eq(PaymentRecord::getId, record.getId())
                .in(PaymentRecord::getStatus, PaymentStatus.WAIT_PAY, PaymentStatus.SUCCESS));
        if (updated == 1) {
            tradeMetrics.payAfterClose();
            log.error("订单已关闭但收到支付成功通知，需退款，orderSn={}，渠道交易号={}", record.getOrderSn(), channelTradeNo);
        }
    }
}
