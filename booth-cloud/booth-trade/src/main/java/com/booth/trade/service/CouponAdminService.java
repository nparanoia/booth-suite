package com.booth.trade.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.booth.common.core.exception.BizException;
import com.booth.common.core.result.PageResult;
import com.booth.common.mybatis.page.PageResults;
import com.booth.trade.dto.CouponSaveRequest;
import com.booth.trade.dto.CouponScopeRequest;
import com.booth.trade.entity.Coupon;
import com.booth.trade.entity.CouponScope;
import com.booth.trade.enums.ReleaseStatus;
import com.booth.trade.error.TradeErrorCode;
import com.booth.trade.mapper.CouponMapper;
import com.booth.trade.mapper.CouponScopeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponAdminService {

    private final CouponMapper couponMapper;
    private final CouponScopeMapper scopeMapper;
    private final CouponService couponService;

    public PageResult<Coupon> page(String keyword, ReleaseStatus status, long pageNum, long pageSize) {
        Page<Coupon> page = couponMapper.selectPage(Page.of(pageNum, pageSize), Wrappers.<Coupon>lambdaQuery()
                .like(StringUtils.hasText(keyword), Coupon::getName, keyword)
                .eq(status != null, Coupon::getStatus, status)
                .orderByDesc(Coupon::getId));
        return PageResults.of(page);
    }

    public Coupon detail(Long id) {
        return couponService.requireCoupon(id);
    }

    public List<CouponScope> scopes(Long couponId) {
        couponService.requireCoupon(couponId);
        return scopeMapper.selectList(Wrappers.<CouponScope>lambdaQuery().eq(CouponScope::getCouponId, couponId));
    }

    public Long create(CouponSaveRequest request) {
        Coupon coupon = new Coupon();
        fill(coupon, request);
        coupon.setRemainCount(request.totalCount());
        coupon.setStatus(ReleaseStatus.DRAFT);
        couponMapper.insert(coupon);
        return coupon.getId();
    }

    public void update(Long id, CouponSaveRequest request) {
        Coupon coupon = couponService.requireCoupon(id);
        boolean moneyChanged = coupon.getAmount().compareTo(request.amount()) != 0
                || coupon.getMinPoint().compareTo(request.minPoint()) != 0;
        BizException.throwIf(coupon.getStatus() == ReleaseStatus.RELEASED && moneyChanged, TradeErrorCode.COUPON_IMMUTABLE);
        fill(coupon, request);
        couponMapper.updateById(coupon);
        if (coupon.getStatus() == ReleaseStatus.DRAFT) {
            couponMapper.update(null, Wrappers.<Coupon>lambdaUpdate()
                    .set(Coupon::getRemainCount, request.totalCount())
                    .eq(Coupon::getId, id));
        }
    }

    public void publish(Long id) {
        int updated = couponMapper.update(null, Wrappers.<Coupon>lambdaUpdate()
                .set(Coupon::getStatus, ReleaseStatus.RELEASED)
                .eq(Coupon::getId, id)
                .eq(Coupon::getStatus, ReleaseStatus.DRAFT));
        BizException.throwIf(updated == 0, TradeErrorCode.COUPON_UNAVAILABLE);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Coupon coupon = couponService.requireCoupon(id);
        BizException.throwIf(coupon.getStatus() != ReleaseStatus.DRAFT, TradeErrorCode.COUPON_DELETE);
        scopeMapper.delete(Wrappers.<CouponScope>lambdaQuery().eq(CouponScope::getCouponId, id));
        couponMapper.deleteById(id);
    }

    public void offline(Long id) {
        int updated = couponMapper.update(null, Wrappers.<Coupon>lambdaUpdate()
                .set(Coupon::getStatus, ReleaseStatus.OFFLINE)
                .eq(Coupon::getId, id)
                .eq(Coupon::getStatus, ReleaseStatus.RELEASED));
        BizException.throwIf(updated == 0, TradeErrorCode.COUPON_UNAVAILABLE);
    }

    @Transactional(rollbackFor = Exception.class)
    public void replaceScopes(Long couponId, List<CouponScopeRequest> scopes) {
        couponService.requireCoupon(couponId);
        scopeMapper.delete(Wrappers.<CouponScope>lambdaQuery().eq(CouponScope::getCouponId, couponId));
        if (scopes == null || scopes.isEmpty()) {
            return;
        }
        for (CouponScopeRequest request : scopes) {
            CouponScope scope = new CouponScope();
            scope.setCouponId(couponId);
            scope.setScopeType(request.scopeType());
            scope.setTargetId(request.targetId());
            scope.setTargetName(request.targetName());
            scopeMapper.insert(scope);
        }
    }

    private static void fill(Coupon coupon, CouponSaveRequest request) {
        coupon.setName(request.name());
        coupon.setAmount(request.amount());
        coupon.setMinPoint(request.minPoint());
        coupon.setUseType(request.useType());
        coupon.setTotalCount(request.totalCount());
        coupon.setPerLimit(request.perLimit());
        coupon.setReceiveStartTime(request.receiveStartTime());
        coupon.setReceiveEndTime(request.receiveEndTime());
        coupon.setUseStartTime(request.useStartTime());
        coupon.setUseEndTime(request.useEndTime());
        coupon.setNote(request.note());
    }
}
