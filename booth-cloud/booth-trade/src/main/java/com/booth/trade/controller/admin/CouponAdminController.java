package com.booth.trade.controller.admin;

import com.booth.common.core.result.PageResult;
import com.booth.common.core.result.Result;
import com.booth.common.web.security.RequiresPermission;
import com.booth.trade.dto.CouponSaveRequest;
import com.booth.trade.dto.CouponScopeBatchRequest;
import com.booth.trade.entity.Coupon;
import com.booth.trade.entity.CouponScope;
import com.booth.trade.enums.ReleaseStatus;
import com.booth.trade.service.CouponAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "后台-优惠券")
@RequiresPermission("trade:coupon:edit")
@RestController
@RequestMapping("/admin/coupons")
@RequiredArgsConstructor
public class CouponAdminController {

    private final CouponAdminService couponAdminService;

    @Operation(summary = "优惠券分页")
    @RequiresPermission("trade:coupon:view")
    @GetMapping
    public Result<PageResult<Coupon>> page(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) ReleaseStatus status,
                                           @RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(couponAdminService.page(keyword, status, pageNum, pageSize));
    }

    @Operation(summary = "优惠券详情")
    @RequiresPermission("trade:coupon:view")
    @GetMapping("/{id}")
    public Result<Coupon> detail(@PathVariable Long id) {
        return Result.ok(couponAdminService.detail(id));
    }

    @Operation(summary = "新增优惠券")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CouponSaveRequest request) {
        return Result.ok(couponAdminService.create(request));
    }

    @Operation(summary = "修改优惠券", description = "已发布的券不能改面额和门槛")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CouponSaveRequest request) {
        couponAdminService.update(id, request);
        return Result.ok();
    }

    @Operation(summary = "发布")
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        couponAdminService.publish(id);
        return Result.ok();
    }

    @Operation(summary = "删除草稿", description = "已发布或已下线的券只能下线，不能删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        couponAdminService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "下线")
    @PostMapping("/{id}/offline")
    public Result<Void> offline(@PathVariable Long id) {
        couponAdminService.offline(id);
        return Result.ok();
    }

    @Operation(summary = "适用范围")
    @RequiresPermission("trade:coupon:view")
    @GetMapping("/{id}/scopes")
    public Result<List<CouponScope>> scopes(@PathVariable Long id) {
        return Result.ok(couponAdminService.scopes(id));
    }

    @Operation(summary = "覆盖适用范围")
    @PutMapping("/{id}/scopes")
    public Result<Void> replaceScopes(@PathVariable Long id, @Valid @RequestBody CouponScopeBatchRequest request) {
        couponAdminService.replaceScopes(id, request.scopes());
        return Result.ok();
    }
}
