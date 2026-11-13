package com.booth.trade.support;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderPricingTest {

    @Test
    void lastLineTakesRemainder() {
        OrderPricing.Priced priced = OrderPricing.price(List.of(
                new OrderPricing.Line(1, new BigDecimal("10.00"), 1),
                new OrderPricing.Line(2, new BigDecimal("10.00"), 1),
                new OrderPricing.Line(3, new BigDecimal("10.00"), 1)
        ), new BigDecimal("10.00"), 100, 1000);

        assertEquals(new BigDecimal("19.00"), priced.payAmount());
        assertEquals(100, priced.useIntegration());
        BigDecimal realSum = priced.shares().stream().map(OrderPricing.Share::realAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal couponSum = priced.shares().stream().map(OrderPricing.Share::couponAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(priced.payAmount(), realSum);
        assertEquals(new BigDecimal("10.00"), couponSum);
    }

    @Test
    void pointsCappedAtHalfOfGoods() {
        OrderPricing.Priced priced = OrderPricing.price(List.of(
                new OrderPricing.Line(1, new BigDecimal("100.00"), 1)
        ), BigDecimal.ZERO, 10000, 10000);
        assertEquals(5000, priced.useIntegration());
        assertEquals(new BigDecimal("50.00"), priced.payAmount());
    }

    @Test
    void smallLastLineDoesNotGoNegative() {
        List<OrderPricing.Line> lines = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            lines.add(new OrderPricing.Line(i, new BigDecimal("0.03"), 1));
        }
        lines.add(new OrderPricing.Line(10, new BigDecimal("0.01"), 1));
        OrderPricing.Priced priced = OrderPricing.price(lines, new BigDecimal("0.25"), 0, 0);

        BigDecimal realSum = BigDecimal.ZERO;
        BigDecimal couponSum = BigDecimal.ZERO;
        for (OrderPricing.Share share : priced.shares()) {
            assertTrue(share.realAmount().signum() >= 0, share.toString());
            realSum = realSum.add(share.realAmount());
            couponSum = couponSum.add(share.couponAmount());
        }
        assertEquals(0, couponSum.compareTo(new BigDecimal("0.25")));
        assertEquals(0, realSum.compareTo(priced.payAmount()));
        assertEquals(0, priced.payAmount().compareTo(new BigDecimal("0.06")));
    }

    @Test
    void couponAndPointsOverflowMoveTogether() {
        List<OrderPricing.Line> lines = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            lines.add(new OrderPricing.Line(i, new BigDecimal("0.04"), 1));
        }
        lines.add(new OrderPricing.Line(10, new BigDecimal("0.01"), 1));
        OrderPricing.Priced priced = OrderPricing.price(lines, new BigDecimal("0.14"), 5, 100);

        BigDecimal realSum = BigDecimal.ZERO;
        BigDecimal couponSum = BigDecimal.ZERO;
        BigDecimal pointSum = BigDecimal.ZERO;
        boolean movedCoupon = false;
        boolean movedPoints = false;
        for (int i = 0; i < priced.shares().size(); i++) {
            OrderPricing.Share share = priced.shares().get(i);
            assertTrue(share.realAmount().signum() >= 0, share.toString());
            realSum = realSum.add(share.realAmount());
            couponSum = couponSum.add(share.couponAmount());
            pointSum = pointSum.add(share.integrationAmount());
            if (i < 10 && share.couponAmount().compareTo(new BigDecimal("0.01")) > 0) {
                movedCoupon = true;
            }
            if (i < 10 && share.integrationAmount().signum() > 0) {
                movedPoints = true;
            }
        }
        assertTrue(movedCoupon);
        assertTrue(movedPoints);
        assertEquals(0, couponSum.compareTo(new BigDecimal("0.14")));
        assertEquals(0, pointSum.compareTo(new BigDecimal("0.05")));
        assertEquals(0, realSum.compareTo(priced.payAmount()));
        assertEquals(0, priced.payAmount().compareTo(new BigDecimal("0.22")));
    }
}
