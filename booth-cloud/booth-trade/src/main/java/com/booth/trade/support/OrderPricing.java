package com.booth.trade.support;

import com.booth.common.core.exception.BizException;
import com.booth.trade.error.TradeErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 金额全部用 BigDecimal，保留 2 位、四舍五入。
 * 100 积分抵 1 元，抵扣不超过商品总额的 50%，且应付金额不能小于 0.01。
 * 优惠按明细金额比例分摊，最后一行吃掉尾差。
 */
public final class OrderPricing {

    public static final int POINTS_PER_YUAN = 100;
    public static final BigDecimal MAX_DEDUCT_RATIO = new BigDecimal("0.50");
    public static final BigDecimal MIN_PAY = new BigDecimal("0.01");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private OrderPricing() {
    }

    public record Line(long skuId, BigDecimal unitPrice, int quantity) {
        public BigDecimal amount() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public record Share(long skuId, BigDecimal couponAmount, BigDecimal integrationAmount, BigDecimal realAmount) {
    }

    public record Priced(
            BigDecimal goodsAmount,
            BigDecimal couponAmount,
            BigDecimal integrationAmount,
            int useIntegration,
            int maxUseIntegration,
            BigDecimal payAmount,
            List<Share> shares) {
    }

    public static Priced price(List<Line> lines, BigDecimal couponAmount, int requestedPoints, int memberPoints) {
        BigDecimal goods = lines.stream().map(Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BizException.throwIf(goods.signum() <= 0, TradeErrorCode.ORDER_ITEM_EMPTY);
        BigDecimal coupon = couponAmount == null ? BigDecimal.ZERO : couponAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal room = goods.subtract(MIN_PAY);
        BizException.throwIf(coupon.compareTo(BigDecimal.ZERO) < 0 || coupon.compareTo(room) > 0,
                TradeErrorCode.ORDER_PAY_TOO_SMALL);

        int maxByRatio = goods.multiply(MAX_DEDUCT_RATIO).multiply(HUNDRED).setScale(0, RoundingMode.DOWN).intValue();
        int maxByRoom = room.subtract(coupon).multiply(HUNDRED).setScale(0, RoundingMode.DOWN).intValue();
        int maxUse = Math.max(0, Math.min(Math.max(memberPoints, 0), Math.min(maxByRatio, maxByRoom)));
        int use = Math.min(Math.max(requestedPoints, 0), maxUse);
        BigDecimal integrationAmount = BigDecimal.valueOf(use).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal pay = goods.subtract(coupon).subtract(integrationAmount);
        return new Priced(goods, coupon, integrationAmount, use, maxUse, pay, apportion(lines, goods, coupon, integrationAmount));
    }

    /**
     * 先按比例分，最后一行吃尾差。某一行因此变成负数时，把多出来的优惠挪到还有剩余货款的行上。
     * 总优惠不超过货款减 0.01，所以一定挪得动。
     */
    private static List<Share> apportion(List<Line> lines, BigDecimal goods, BigDecimal coupon, BigDecimal points) {
        int size = lines.size();
        BigDecimal[] couponShares = new BigDecimal[size];
        BigDecimal[] pointShares = new BigDecimal[size];
        BigDecimal couponLeft = coupon;
        BigDecimal pointLeft = points;
        for (int i = 0; i < size; i++) {
            boolean last = i == size - 1;
            BigDecimal amount = lines.get(i).amount();
            couponShares[i] = last ? couponLeft : coupon.multiply(amount).divide(goods, 2, RoundingMode.HALF_UP);
            pointShares[i] = last ? pointLeft : points.multiply(amount).divide(goods, 2, RoundingMode.HALF_UP);
            if (!last) {
                couponLeft = couponLeft.subtract(couponShares[i]);
                pointLeft = pointLeft.subtract(pointShares[i]);
            }
        }
        moveOverflow(lines, couponShares, pointShares);
        List<Share> shares = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Line line = lines.get(i);
            shares.add(new Share(line.skuId(), couponShares[i], pointShares[i],
                    line.amount().subtract(couponShares[i]).subtract(pointShares[i])));
        }
        return shares;
    }

    private static void moveOverflow(List<Line> lines, BigDecimal[] coupons, BigDecimal[] points) {
        for (int i = 0; i < lines.size(); i++) {
            BigDecimal overflow = coupons[i].add(points[i]).subtract(lines.get(i).amount());
            for (int j = 0; j < lines.size() && overflow.signum() > 0; j++) {
                if (j == i) {
                    continue;
                }
                BigDecimal room = lines.get(j).amount().subtract(coupons[j]).subtract(points[j]);
                if (room.signum() <= 0) {
                    continue;
                }
                BigDecimal moveCoupon = coupons[i].min(room).min(overflow);
                if (moveCoupon.signum() > 0) {
                    coupons[i] = coupons[i].subtract(moveCoupon);
                    coupons[j] = coupons[j].add(moveCoupon);
                    overflow = overflow.subtract(moveCoupon);
                    room = room.subtract(moveCoupon);
                }
                BigDecimal movePoints = points[i].min(room).min(overflow);
                if (movePoints.signum() > 0) {
                    points[i] = points[i].subtract(movePoints);
                    points[j] = points[j].add(movePoints);
                    overflow = overflow.subtract(movePoints);
                }
            }
            BizException.throwIf(overflow.signum() > 0, TradeErrorCode.ORDER_PAY_TOO_SMALL, "优惠分摊后出现负的明细金额");
        }
    }
}
