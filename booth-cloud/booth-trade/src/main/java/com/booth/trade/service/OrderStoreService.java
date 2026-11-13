package com.booth.trade.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.trade.config.OrderProperties;
import com.booth.trade.entity.CartItem;
import com.booth.trade.entity.OrderStatusLog;
import com.booth.trade.entity.TradeOrder;
import com.booth.trade.entity.TradeOrderItem;
import com.booth.trade.enums.OrderStatus;
import com.booth.trade.mapper.CartItemMapper;
import com.booth.trade.mapper.OrderStatusLogMapper;
import com.booth.trade.mapper.TradeOrderItemMapper;
import com.booth.trade.mapper.TradeOrderMapper;
import com.booth.trade.support.OrderEvent;
import com.booth.trade.support.OrderMq;
import com.booth.trade.support.OrderOperators;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 只写本地表和本地消息，不调用其他服务。由 {@link OrderCommandService} 在远程步骤成功后调用。
 */
@Service
@RequiredArgsConstructor
public class OrderStoreService {

    private final TradeOrderMapper orderMapper;
    private final TradeOrderItemMapper itemMapper;
    private final OrderStatusLogMapper statusLogMapper;
    private final CartItemMapper cartItemMapper;
    private final LocalMessageService localMessageService;
    private final OrderProperties orderProperties;

    @Transactional(rollbackFor = Exception.class)
    public void save(TradeOrder order, List<TradeOrderItem> items, List<Long> cartSkuIds) {
        orderMapper.insert(order);
        for (TradeOrderItem item : items) {
            itemMapper.insert(item);
        }
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(order.getId());
        log.setToStatus(OrderStatus.WAIT_PAY);
        log.setOperatorType(OrderOperators.MEMBER);
        log.setOperator(String.valueOf(order.getMemberId()));
        log.setNote("创建订单");
        statusLogMapper.insert(log);
        localMessageService.save(OrderMq.BIZ_TIMEOUT, order.getOrderSn(), OrderMq.DELAY_EXCHANGE, OrderMq.RK_TIMEOUT,
                new OrderEvent(order.getOrderSn()), orderProperties.payTimeout().toMillis());
        if (cartSkuIds != null && !cartSkuIds.isEmpty()) {
            cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery()
                    .eq(CartItem::getMemberId, order.getMemberId())
                    .in(CartItem::getSkuId, cartSkuIds));
        }
    }
}
