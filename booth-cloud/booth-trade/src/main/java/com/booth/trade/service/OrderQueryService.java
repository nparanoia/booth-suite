package com.booth.trade.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.booth.common.core.result.PageResult;
import com.booth.common.mybatis.page.PageResults;
import com.booth.trade.dto.OrderQuery;
import com.booth.trade.entity.PaymentRecord;
import com.booth.trade.entity.TradeOrder;
import com.booth.trade.entity.TradeOrderItem;
import com.booth.trade.mapper.PaymentRecordMapper;
import com.booth.trade.mapper.TradeOrderMapper;
import com.booth.trade.vo.OrderDetailVO;
import com.booth.trade.vo.OrderItemVO;
import com.booth.trade.vo.OrderSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final TradeOrderMapper orderMapper;
    private final OrderLifecycleService lifecycleService;
    private final PaymentRecordMapper paymentRecordMapper;

    public PageResult<OrderSummaryVO> pageMine(Long memberId, OrderQuery query) {
        Page<TradeOrder> page = orderMapper.selectPage(query.toPage(), Wrappers.<TradeOrder>lambdaQuery()
                .eq(TradeOrder::getMemberId, memberId)
                .eq(TradeOrder::getMemberDeleted, false)
                .eq(query.getStatus() != null, TradeOrder::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getOrderSn()), TradeOrder::getOrderSn, query.getOrderSn())
                .orderByDesc(TradeOrder::getId));
        return PageResults.of(page, this::toSummary);
    }

    public OrderDetailVO detailMine(Long memberId, String orderSn) {
        return toDetail(lifecycleService.requireMember(memberId, orderSn));
    }

    public PageResult<OrderSummaryVO> pageAdmin(OrderQuery query) {
        Page<TradeOrder> page = orderMapper.selectPage(query.toPage(), Wrappers.<TradeOrder>lambdaQuery()
                .eq(query.getStatus() != null, TradeOrder::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getOrderSn()), TradeOrder::getOrderSn, query.getOrderSn())
                .orderByDesc(TradeOrder::getId));
        return PageResults.of(page, this::toSummary);
    }

    public OrderDetailVO detailAdmin(String orderSn) {
        return toDetail(lifecycleService.require(orderSn));
    }

    private OrderSummaryVO toSummary(TradeOrder order) {
        return new OrderSummaryVO(order.getOrderSn(), order.getStatus(), order.getPayAmount(), order.getCreateTime());
    }

    private OrderDetailVO toDetail(TradeOrder order) {
        List<OrderItemVO> items = lifecycleService.items(order.getId()).stream().map(this::toItem).toList();
        PaymentRecord payment = paymentRecordMapper.selectOne(Wrappers.<PaymentRecord>lambdaQuery()
                .eq(PaymentRecord::getOrderSn, order.getOrderSn())
                .orderByDesc(PaymentRecord::getId)
                .last("LIMIT 1"));
        return new OrderDetailVO(order.getOrderSn(), order.getMemberId(), order.getMemberUsername(), order.getStatus(),
                order.getOrderType(),
                order.getTotalAmount(), order.getFreightAmount(), order.getCouponAmount(), order.getIntegrationAmount(),
                order.getPayAmount(), order.getUseIntegration(), order.getRewardIntegration(), order.getPayType(),
                payment == null ? null : payment.getStatus(), order.getPaymentTime(),
                order.getReceiverName(), order.getReceiverPhone(), order.getReceiverProvince(), order.getReceiverCity(),
                order.getReceiverRegion(), order.getReceiverDetailAddress(), order.getNote(),
                order.getDeliveryCompany(), order.getDeliverySn(), order.getDeliveryTime(), order.getReceiveTime(),
                order.getCloseTime(), order.getCloseReason(), order.getCreateTime(), items);
    }

    private OrderItemVO toItem(TradeOrderItem item) {
        return new OrderItemVO(item.getSkuId(), item.getProductName(), item.getProductPic(), item.getSpecData(),
                item.getPrice(), item.getQuantity(), item.getRealAmount(), item.getId());
    }
}
