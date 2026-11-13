package com.booth.trade.controller.admin;

import com.booth.common.core.constant.AuthHeaders;
import com.booth.common.core.result.PageResult;
import com.booth.common.core.result.Result;
import com.booth.common.web.security.RequiresPermission;
import com.booth.trade.dto.OrderQuery;
import com.booth.trade.dto.ShipRequest;
import com.booth.trade.service.OrderLifecycleService;
import com.booth.trade.service.OrderQueryService;
import com.booth.trade.vo.OrderDetailVO;
import com.booth.trade.vo.OrderSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-订单")
@RequiresPermission("trade:order:edit")
@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class OrderAdminController {

    private final OrderQueryService orderQueryService;
    private final OrderLifecycleService orderLifecycleService;

    @Operation(summary = "订单分页")
    @RequiresPermission("trade:order:view")
    @GetMapping
    public Result<PageResult<OrderSummaryVO>> page(@Valid OrderQuery query) {
        return Result.ok(orderQueryService.pageAdmin(query));
    }

    @Operation(summary = "订单详情")
    @RequiresPermission("trade:order:view")
    @GetMapping("/{orderSn}")
    public Result<OrderDetailVO> detail(@PathVariable String orderSn) {
        return Result.ok(orderQueryService.detailAdmin(orderSn));
    }

    @Operation(summary = "发货")
    @PostMapping("/{orderSn}/ship")
    public Result<Void> ship(@RequestHeader(AuthHeaders.ADMIN_ID) Long adminId,
                             @PathVariable String orderSn, @Valid @RequestBody ShipRequest request) {
        orderLifecycleService.ship(orderSn, request.deliveryCompany(), request.deliverySn(), adminId);
        return Result.ok();
    }

    @Operation(summary = "关闭待付款订单")
    @PostMapping("/{orderSn}/close")
    public Result<Void> close(@RequestHeader(AuthHeaders.ADMIN_ID) Long adminId, @PathVariable String orderSn) {
        orderLifecycleService.closeByAdmin(orderSn, adminId);
        return Result.ok();
    }
}
