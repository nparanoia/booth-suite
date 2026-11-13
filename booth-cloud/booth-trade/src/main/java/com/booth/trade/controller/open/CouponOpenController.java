package com.booth.trade.controller.open;

import com.booth.common.core.result.Result;
import com.booth.trade.entity.Coupon;
import com.booth.trade.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "前台-可领取优惠券")
@RestController
@RequestMapping("/open/coupons")
@RequiredArgsConstructor
public class CouponOpenController {

    private final CouponService couponService;

    @Operation(summary = "可领取的优惠券")
    @GetMapping
    public Result<List<Coupon>> list() {
        return Result.ok(couponService.listReceivable());
    }
}
