package com.booth.trade.controller.app;

import com.booth.common.core.constant.AuthHeaders;
import com.booth.common.core.result.Result;
import com.booth.trade.entity.Coupon;
import com.booth.trade.entity.MemberCoupon;
import com.booth.trade.enums.MemberCouponStatus;
import com.booth.trade.service.CouponService;
import com.booth.trade.vo.MemberCouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "前台-我的优惠券")
@RestController
@RequestMapping("/app/coupons")
@RequiredArgsConstructor
public class CouponAppController {

    private final CouponService couponService;

    @Operation(summary = "领取优惠券")
    @PostMapping("/{id}/receive")
    public Result<Long> receive(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId, @PathVariable Long id) {
        return Result.ok(couponService.receive(memberId, id));
    }

    @Operation(summary = "我的优惠券")
    @GetMapping
    public Result<List<MemberCouponVO>> mine(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId,
                                             @RequestParam(required = false) MemberCouponStatus status) {
        List<MemberCouponVO> list = couponService.listMine(memberId, status).stream().map(this::toVO).toList();
        return Result.ok(list);
    }

    private MemberCouponVO toVO(MemberCoupon item) {
        Coupon coupon = couponService.requireCoupon(item.getCouponId());
        return new MemberCouponVO(item.getId(), coupon.getId(), coupon.getName(), coupon.getAmount(),
                coupon.getMinPoint(), item.getStatus(), coupon.getUseEndTime());
    }
}
