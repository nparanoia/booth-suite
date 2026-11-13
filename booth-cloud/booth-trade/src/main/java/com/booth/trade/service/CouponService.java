package com.booth.trade.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.common.core.exception.BizException;
import com.booth.trade.entity.Coupon;
import com.booth.trade.entity.CouponScope;
import com.booth.trade.entity.MemberCoupon;
import com.booth.trade.enums.MemberCouponStatus;
import com.booth.trade.enums.ReleaseStatus;
import com.booth.trade.error.TradeErrorCode;
import com.booth.trade.mapper.CouponMapper;
import com.booth.trade.mapper.CouponScopeMapper;
import com.booth.trade.mapper.MemberCouponMapper;
import com.booth.trade.support.OrderPricing;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponMapper couponMapper;
    private final CouponScopeMapper scopeMapper;
    private final MemberCouponMapper memberCouponMapper;

    public List<Coupon> listReceivable() {
        LocalDateTime now = LocalDateTime.now();
        return couponMapper.selectList(Wrappers.<Coupon>lambdaQuery()
                .eq(Coupon::getStatus, ReleaseStatus.RELEASED)
                .gt(Coupon::getRemainCount, 0)
                .le(Coupon::getReceiveStartTime, now)
                .ge(Coupon::getReceiveEndTime, now)
                .orderByDesc(Coupon::getId));
    }

    public List<MemberCoupon> listMine(Long memberId, MemberCouponStatus status) {
        return memberCouponMapper.selectList(Wrappers.<MemberCoupon>lambdaQuery()
                .eq(MemberCoupon::getMemberId, memberId)
                .eq(status != null, MemberCoupon::getStatus, status)
                .orderByDesc(MemberCoupon::getId));
    }

    public Coupon requireCoupon(Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        BizException.throwIf(coupon == null, TradeErrorCode.COUPON_NOT_FOUND);
        return coupon;
    }

    /**
     * 先扣剩余数量，这一行的行锁会把同一张券的并发领取串行化，后面的每人限领计数才不会被并发突破。
     * 超过限领时抛异常，整个事务回滚，剩余数量也会加回去。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long receive(Long memberId, Long couponId) {
        int taken = couponMapper.update(null, Wrappers.<Coupon>lambdaUpdate()
                .setSql("remain_count = remain_count - 1")
                .eq(Coupon::getId, couponId)
                .eq(Coupon::getStatus, ReleaseStatus.RELEASED)
                .gt(Coupon::getRemainCount, 0)
                .apply("NOW(3) BETWEEN receive_start_time AND receive_end_time"));
        BizException.throwIf(taken == 0, TradeErrorCode.COUPON_SOLD_OUT);
        Coupon coupon = requireCoupon(couponId);
        long owned = memberCouponMapper.selectCount(Wrappers.<MemberCoupon>lambdaQuery()
                .eq(MemberCoupon::getCouponId, couponId)
                .eq(MemberCoupon::getMemberId, memberId));
        BizException.throwIf(owned >= coupon.getPerLimit(), TradeErrorCode.COUPON_LIMIT);
        MemberCoupon mine = new MemberCoupon();
        mine.setCouponId(couponId);
        mine.setMemberId(memberId);
        mine.setGetType(1);
        mine.setStatus(MemberCouponStatus.UNUSED);
        memberCouponMapper.insert(mine);
        return mine.getId();
    }

    public List<MemberCoupon> usable(Long memberId, BigDecimal goodsAmount, Set<Long> categoryIds, Set<Long> productIds) {
        List<MemberCoupon> mine = listMine(memberId, MemberCouponStatus.UNUSED);
        return mine.stream().filter(item -> {
            Coupon coupon = couponMapper.selectById(item.getCouponId());
            return coupon != null && applicable(coupon, goodsAmount, categoryIds, productIds, true);
        }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public BigDecimal lockForOrder(Long memberId, Long memberCouponId, String orderSn, BigDecimal goodsAmount,
                                   Set<Long> categoryIds, Set<Long> productIds) {
        MemberCoupon mine = memberCouponMapper.selectOne(Wrappers.<MemberCoupon>lambdaQuery()
                .eq(MemberCoupon::getId, memberCouponId)
                .eq(MemberCoupon::getMemberId, memberId));
        BizException.throwIf(mine == null, TradeErrorCode.COUPON_NOT_FOUND);
        Coupon coupon = requireCoupon(mine.getCouponId());
        BizException.throwIf(!applicable(coupon, goodsAmount, categoryIds, productIds, true), TradeErrorCode.COUPON_NOT_APPLICABLE);
        int locked = memberCouponMapper.update(null, Wrappers.<MemberCoupon>lambdaUpdate()
                .set(MemberCoupon::getStatus, MemberCouponStatus.LOCKED)
                .set(MemberCoupon::getOrderSn, orderSn)
                .set(MemberCoupon::getLockTime, LocalDateTime.now())
                .eq(MemberCoupon::getId, mine.getId())
                .eq(MemberCoupon::getMemberId, memberId)
                .eq(MemberCoupon::getStatus, MemberCouponStatus.UNUSED));
        BizException.throwIf(locked == 0, TradeErrorCode.COUPON_UNAVAILABLE);
        return coupon.getAmount();
    }

    public void unlock(String orderSn) {
        memberCouponMapper.update(null, Wrappers.<MemberCoupon>lambdaUpdate()
                .set(MemberCoupon::getStatus, MemberCouponStatus.UNUSED)
                .set(MemberCoupon::getOrderSn, null)
                .set(MemberCoupon::getLockTime, null)
                .eq(MemberCoupon::getOrderSn, orderSn)
                .eq(MemberCoupon::getStatus, MemberCouponStatus.LOCKED));
    }

    public void markUsed(String orderSn) {
        memberCouponMapper.update(null, Wrappers.<MemberCoupon>lambdaUpdate()
                .set(MemberCoupon::getStatus, MemberCouponStatus.USED)
                .set(MemberCoupon::getUseTime, LocalDateTime.now())
                .eq(MemberCoupon::getOrderSn, orderSn)
                .eq(MemberCoupon::getStatus, MemberCouponStatus.LOCKED));
    }

    public int expireUnused() {
        return memberCouponMapper.expireUnused();
    }

    public boolean applicable(Coupon coupon, BigDecimal goodsAmount, Set<Long> categoryIds, Set<Long> productIds, boolean checkUseTime) {
        if (coupon.getStatus() != ReleaseStatus.RELEASED) {
            return false;
        }
        if (goodsAmount.compareTo(coupon.getMinPoint()) < 0) {
            return false;
        }
        if (coupon.getAmount().compareTo(goodsAmount.subtract(OrderPricing.MIN_PAY)) > 0) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (checkUseTime && (now.isBefore(coupon.getUseStartTime()) || now.isAfter(coupon.getUseEndTime()))) {
            return false;
        }
        if (coupon.getUseType() == null || coupon.getUseType() == 0) {
            return true;
        }
        Set<Long> targets = scopeMapper.selectList(Wrappers.<CouponScope>lambdaQuery()
                        .eq(CouponScope::getCouponId, coupon.getId()))
                .stream().map(CouponScope::getTargetId).collect(Collectors.toSet());
        Set<Long> actual = coupon.getUseType() == 1 ? categoryIds : productIds;
        return actual.stream().anyMatch(targets::contains);
    }
}
