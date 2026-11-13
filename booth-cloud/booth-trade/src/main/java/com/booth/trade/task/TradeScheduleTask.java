package com.booth.trade.task;

import com.booth.trade.service.CouponService;
import com.booth.trade.service.LocalMessageService;
import com.booth.trade.service.OrderLifecycleService;
import com.booth.trade.service.OrderRepairService;
import com.booth.trade.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TradeScheduleTask {

    private final LocalMessageService localMessageService;
    private final OrderLifecycleService orderLifecycleService;
    private final PaymentService paymentService;
    private final CouponService couponService;
    private final OrderRepairService orderRepairService;

    @Scheduled(fixedDelay = 10_000, initialDelay = 15_000)
    public void retryMessages() {
        localMessageService.retryDue();
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 20_000)
    public void closeTimeoutOrders() {
        orderLifecycleService.closeExpired();
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 60_000)
    public void autoReceive() {
        orderLifecycleService.autoReceive();
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 40_000)
    public void queryPayments() {
        paymentService.queryPending();
    }

    @Scheduled(cron = "0 15 3 * * *")
    public void expireCoupons() {
        couponService.expireUnused();
    }

    @Scheduled(fixedDelay = 120_000, initialDelay = 80_000)
    public void repairSideEffects() {
        orderRepairService.repairCoupons();
        orderRepairService.repairRewards();
    }
}
