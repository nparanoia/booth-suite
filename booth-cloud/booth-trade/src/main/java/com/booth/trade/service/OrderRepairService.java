package com.booth.trade.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.api.user.IntegrationInnerClient;
import com.booth.api.user.dto.IntegrationChangeRequest;
import com.booth.trade.entity.MemberCoupon;
import com.booth.trade.entity.TradeOrder;
import com.booth.trade.enums.MemberCouponStatus;
import com.booth.trade.enums.OrderStatus;
import com.booth.trade.mapper.MemberCouponMapper;
import com.booth.trade.mapper.TradeOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 优惠券和完成赠送积分的补救。积分退回不在这里扫：用户服务按扣减流水补，避免反复处理同一批已关闭订单。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderRepairService {

    private static final Set<OrderStatus> PAID = EnumSet.of(
            OrderStatus.WAIT_DELIVER, OrderStatus.DELIVERED, OrderStatus.COMPLETED);

    private final MemberCouponMapper memberCouponMapper;
    private final TradeOrderMapper orderMapper;
    private final CouponService couponService;
    private final OrderLifecycleService orderLifecycleService;
    private final IntegrationInnerClient integrationInnerClient;

    public void repairCoupons() {
        List<MemberCoupon> locked = memberCouponMapper.selectList(Wrappers.<MemberCoupon>lambdaQuery()
                .eq(MemberCoupon::getStatus, MemberCouponStatus.LOCKED)
                .isNotNull(MemberCoupon::getOrderSn)
                .orderByAsc(MemberCoupon::getId)
                .last("LIMIT 100"));
        for (MemberCoupon coupon : locked) {
            String orderSn = coupon.getOrderSn();
            try {
                TradeOrder order = orderLifecycleService.find(orderSn);
                if (order == null || order.getStatus() == OrderStatus.CLOSED) {
                    couponService.unlock(orderSn);
                    log.info("对账解锁优惠券，orderSn={}", orderSn);
                } else if (PAID.contains(order.getStatus())) {
                    couponService.markUsed(orderSn);
                    log.info("对账核销优惠券，orderSn={}", orderSn);
                }
            } catch (RuntimeException e) {
                log.warn("对账优惠券失败，orderSn={}", orderSn, e);
            }
        }
    }

    public void repairRewards() {
        List<TradeOrder> completed = orderMapper.selectList(Wrappers.<TradeOrder>lambdaQuery()
                .eq(TradeOrder::getStatus, OrderStatus.COMPLETED)
                .gt(TradeOrder::getRewardIntegration, 0)
                .ge(TradeOrder::getReceiveTime, LocalDateTime.now().minusDays(3))
                .orderByDesc(TradeOrder::getReceiveTime)
                .last("LIMIT 200"));
        for (TradeOrder order : completed) {
            try {
                integrationInnerClient.reward(order.getMemberId(),
                        new IntegrationChangeRequest(order.getOrderSn(), order.getRewardIntegration())).dataOrThrow();
            } catch (RuntimeException e) {
                log.warn("对账赠送积分失败，orderSn={}", order.getOrderSn(), e);
            }
        }
    }
}
