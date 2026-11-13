package com.booth.trade.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.common.core.exception.BizException;
import com.booth.trade.config.OrderProperties;
import com.booth.trade.entity.OrderStatusLog;
import com.booth.trade.entity.TradeOrder;
import com.booth.trade.entity.TradeOrderItem;
import com.booth.trade.enums.OrderStatus;
import com.booth.trade.error.TradeErrorCode;
import com.booth.trade.mapper.OrderStatusLogMapper;
import com.booth.trade.mapper.TradeOrderItemMapper;
import com.booth.trade.mapper.TradeOrderMapper;
import com.booth.trade.support.OrderEvent;
import com.booth.trade.support.OrderMq;
import com.booth.trade.support.OrderOperators;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 每一次状态变化都是一条 WHERE status = 旧值 的更新。更新成功才写状态日志和本地消息。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderLifecycleService {

    private final TradeOrderMapper orderMapper;
    private final TradeOrderItemMapper itemMapper;
    private final OrderStatusLogMapper statusLogMapper;
    private final LocalMessageService localMessageService;
    private final OrderProperties orderProperties;
    private final TransactionTemplate transactionTemplate;

    public TradeOrder find(String orderSn) {
        return orderMapper.selectOne(Wrappers.<TradeOrder>lambdaQuery().eq(TradeOrder::getOrderSn, orderSn));
    }

    public TradeOrder require(String orderSn) {
        TradeOrder order = find(orderSn);
        BizException.throwIf(order == null, TradeErrorCode.ORDER_NOT_FOUND);
        return order;
    }

    public TradeOrder requireMember(Long memberId, String orderSn) {
        TradeOrder order = orderMapper.selectOne(Wrappers.<TradeOrder>lambdaQuery()
                .eq(TradeOrder::getOrderSn, orderSn)
                .eq(TradeOrder::getMemberId, memberId));
        BizException.throwIf(order == null, TradeErrorCode.ORDER_NOT_FOUND);
        return order;
    }

    public List<TradeOrderItem> items(Long orderId) {
        return itemMapper.selectList(Wrappers.<TradeOrderItem>lambdaQuery()
                .eq(TradeOrderItem::getOrderId, orderId)
                .orderByAsc(TradeOrderItem::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long memberId, String orderSn) {
        TradeOrder order = requireMember(memberId, orderSn);
        BizException.throwIf(!close(order, "MEMBER_CANCEL", OrderOperators.MEMBER, String.valueOf(memberId)),
                TradeErrorCode.ORDER_STATUS_CHANGED);
    }

    @Transactional(rollbackFor = Exception.class)
    public void closeByAdmin(String orderSn, Long adminId) {
        BizException.throwIf(!close(require(orderSn), "ADMIN_CLOSE", OrderOperators.ADMIN, String.valueOf(adminId)),
                TradeErrorCode.ORDER_STATUS_CHANGED);
    }

    @Transactional(rollbackFor = Exception.class)
    public void closeTimeout(String orderSn) {
        TradeOrder order = find(orderSn);
        if (order == null) {
            return;
        }
        close(order, "TIMEOUT", OrderOperators.SYSTEM, "system");
    }

    public void closeExpired() {
        LocalDateTime deadline = LocalDateTime.now().minus(orderProperties.payTimeout());
        List<TradeOrder> expired = orderMapper.selectList(Wrappers.<TradeOrder>lambdaQuery()
                .eq(TradeOrder::getStatus, OrderStatus.WAIT_PAY)
                .lt(TradeOrder::getCreateTime, deadline)
                .last("LIMIT 100"));
        for (TradeOrder order : expired) {
            try {
                transactionTemplate.executeWithoutResult(status -> close(order, "TIMEOUT", OrderOperators.SYSTEM, "system"));
            } catch (RuntimeException e) {
                log.warn("超时关单失败，orderSn={}", order.getOrderSn(), e);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean markPaid(String orderSn, int payType) {
        int updated = orderMapper.update(null, Wrappers.<TradeOrder>lambdaUpdate()
                .set(TradeOrder::getStatus, OrderStatus.WAIT_DELIVER)
                .set(TradeOrder::getPayType, payType)
                .set(TradeOrder::getPaymentTime, LocalDateTime.now())
                .eq(TradeOrder::getOrderSn, orderSn)
                .eq(TradeOrder::getStatus, OrderStatus.WAIT_PAY));
        if (updated == 0) {
            return false;
        }
        TradeOrder order = require(orderSn);
        appendLog(order.getId(), OrderStatus.WAIT_PAY, OrderStatus.WAIT_DELIVER, OrderOperators.SYSTEM, "pay", "支付成功");
        localMessageService.save(OrderMq.BIZ_PAID, orderSn, OrderMq.EVENT_EXCHANGE, OrderMq.RK_PAID, new OrderEvent(orderSn), 0);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public void ship(String orderSn, String company, String deliverySn, Long adminId) {
        int updated = orderMapper.update(null, Wrappers.<TradeOrder>lambdaUpdate()
                .set(TradeOrder::getStatus, OrderStatus.DELIVERED)
                .set(TradeOrder::getDeliveryCompany, company)
                .set(TradeOrder::getDeliverySn, deliverySn)
                .set(TradeOrder::getDeliveryTime, LocalDateTime.now())
                .eq(TradeOrder::getOrderSn, orderSn)
                .eq(TradeOrder::getStatus, OrderStatus.WAIT_DELIVER));
        BizException.throwIf(updated == 0, TradeErrorCode.ORDER_STATUS_CHANGED);
        appendLog(require(orderSn).getId(), OrderStatus.WAIT_DELIVER, OrderStatus.DELIVERED,
                OrderOperators.ADMIN, String.valueOf(adminId), company + " " + deliverySn);
    }

    @Transactional(rollbackFor = Exception.class)
    public void receive(Long memberId, String orderSn) {
        TradeOrder order = requireMember(memberId, orderSn);
        BizException.throwIf(!complete(order, OrderOperators.MEMBER, String.valueOf(memberId)), TradeErrorCode.ORDER_STATUS_CHANGED);
    }

    /** 仅待发货可改收货信息；不改变订单状态。 */
    @Transactional(rollbackFor = Exception.class)
    public void updateReceiver(Long memberId, String orderSn,
                               String name, String phone, String province, String city, String region, String detail) {
        TradeOrder order = requireMember(memberId, orderSn);
        BizException.throwIf(order.getStatus() != OrderStatus.WAIT_DELIVER, TradeErrorCode.ORDER_RECEIVER_LOCKED);
        int updated = orderMapper.update(null, Wrappers.<TradeOrder>lambdaUpdate()
                .set(TradeOrder::getReceiverName, name)
                .set(TradeOrder::getReceiverPhone, phone)
                .set(TradeOrder::getReceiverProvince, province)
                .set(TradeOrder::getReceiverCity, city)
                .set(TradeOrder::getReceiverRegion, region)
                .set(TradeOrder::getReceiverDetailAddress, detail)
                .eq(TradeOrder::getOrderSn, orderSn)
                .eq(TradeOrder::getMemberId, memberId)
                .eq(TradeOrder::getStatus, OrderStatus.WAIT_DELIVER));
        BizException.throwIf(updated == 0, TradeErrorCode.ORDER_STATUS_CHANGED);
        appendLog(order.getId(), OrderStatus.WAIT_DELIVER, OrderStatus.WAIT_DELIVER,
                OrderOperators.MEMBER, String.valueOf(memberId), "修改收货地址");
    }

    public void autoReceive() {
        LocalDateTime deadline = LocalDateTime.now().minus(orderProperties.autoReceive());
        List<TradeOrder> due = orderMapper.selectList(Wrappers.<TradeOrder>lambdaQuery()
                .eq(TradeOrder::getStatus, OrderStatus.DELIVERED)
                .lt(TradeOrder::getDeliveryTime, deadline)
                .last("LIMIT 100"));
        for (TradeOrder order : due) {
            try {
                transactionTemplate.executeWithoutResult(status -> complete(order, OrderOperators.SYSTEM, "system"));
            } catch (RuntimeException e) {
                log.warn("自动确认收货失败，orderSn={}", order.getOrderSn(), e);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void hide(Long memberId, String orderSn) {
        int updated = orderMapper.update(null, Wrappers.<TradeOrder>lambdaUpdate()
                .set(TradeOrder::getMemberDeleted, true)
                .eq(TradeOrder::getOrderSn, orderSn)
                .eq(TradeOrder::getMemberId, memberId)
                .in(TradeOrder::getStatus, OrderStatus.COMPLETED, OrderStatus.CLOSED));
        BizException.throwIf(updated == 0, TradeErrorCode.ORDER_STATUS_CHANGED);
    }

    private boolean close(TradeOrder order, String reason, int operatorType, String operator) {
        int updated = orderMapper.update(null, Wrappers.<TradeOrder>lambdaUpdate()
                .set(TradeOrder::getStatus, OrderStatus.CLOSED)
                .set(TradeOrder::getCloseTime, LocalDateTime.now())
                .set(TradeOrder::getCloseReason, reason)
                .eq(TradeOrder::getId, order.getId())
                .eq(TradeOrder::getStatus, OrderStatus.WAIT_PAY));
        if (updated == 0) {
            return false;
        }
        appendLog(order.getId(), OrderStatus.WAIT_PAY, OrderStatus.CLOSED, operatorType, operator, reason);
        localMessageService.save(OrderMq.BIZ_CLOSED, order.getOrderSn(), OrderMq.EVENT_EXCHANGE, OrderMq.RK_CLOSED,
                new OrderEvent(order.getOrderSn()), 0);
        return true;
    }

    private boolean complete(TradeOrder order, int operatorType, String operator) {
        int updated = orderMapper.update(null, Wrappers.<TradeOrder>lambdaUpdate()
                .set(TradeOrder::getStatus, OrderStatus.COMPLETED)
                .set(TradeOrder::getReceiveTime, LocalDateTime.now())
                .eq(TradeOrder::getId, order.getId())
                .eq(TradeOrder::getStatus, OrderStatus.DELIVERED));
        if (updated == 0) {
            return false;
        }
        appendLog(order.getId(), OrderStatus.DELIVERED, OrderStatus.COMPLETED, operatorType, operator, "确认收货");
        localMessageService.save(OrderMq.BIZ_COMPLETED, order.getOrderSn(), OrderMq.EVENT_EXCHANGE, OrderMq.RK_COMPLETED,
                new OrderEvent(order.getOrderSn()), 0);
        return true;
    }

    private void appendLog(Long orderId, OrderStatus from, OrderStatus to, int operatorType, String operator, String note) {
        OrderStatusLog row = new OrderStatusLog();
        row.setOrderId(orderId);
        row.setFromStatus(from);
        row.setToStatus(to);
        row.setOperatorType(operatorType);
        row.setOperator(operator);
        row.setNote(note);
        statusLogMapper.insert(row);
    }
}
