package com.booth.trade.controller.app;

import com.booth.common.core.constant.AuthHeaders;
import com.booth.common.core.result.PageResult;
import com.booth.common.core.result.Result;
import com.booth.trade.dto.OrderPrepareRequest;
import com.booth.trade.dto.OrderQuery;
import com.booth.trade.dto.OrderReceiverUpdateRequest;
import com.booth.trade.dto.OrderSubmitRequest;
import com.booth.trade.service.OrderCommandService;
import com.booth.trade.service.OrderLifecycleService;
import com.booth.trade.service.OrderQueryService;
import com.booth.trade.service.PaymentService;
import com.booth.trade.support.TradeMetrics;
import com.booth.trade.vo.OrderConfirmVO;
import com.booth.trade.vo.OrderDetailVO;
import com.booth.trade.vo.OrderSummaryVO;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "前台-订单")
@RestController
@RequestMapping("/app/orders")
@RequiredArgsConstructor
public class OrderAppController {

    private final OrderCommandService orderCommandService;
    private final OrderQueryService orderQueryService;
    private final OrderLifecycleService orderLifecycleService;
    private final PaymentService paymentService;
    private final TradeMetrics tradeMetrics;

    @Operation(summary = "确认订单", description = "返回结算信息和一次性的 submitToken")
    @PostMapping("/confirm")
    public Result<OrderConfirmVO> confirm(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId,
                                          @Valid @RequestBody OrderPrepareRequest request) {
        return Result.ok(orderCommandService.confirm(memberId, request));
    }

    @Operation(summary = "提交订单")
    @PostMapping
    public Result<String> submit(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId,
                                 @Valid @RequestBody OrderSubmitRequest request) {
        try {
            String orderSn = orderCommandService.submit(memberId, request);
            tradeMetrics.orderPlaced();
            return Result.ok(orderSn);
        } catch (RuntimeException e) {
            tradeMetrics.orderFailed();
            throw e;
        }
    }

    @Operation(summary = "我的订单")
    @GetMapping
    public Result<PageResult<OrderSummaryVO>> page(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId,
                                                   @Valid OrderQuery query) {
        return Result.ok(orderQueryService.pageMine(memberId, query));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{orderSn}")
    public Result<OrderDetailVO> detail(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId, @PathVariable String orderSn) {
        return Result.ok(orderQueryService.detailMine(memberId, orderSn));
    }

    @Operation(summary = "取消待付款订单")
    @PostMapping("/{orderSn}/cancel")
    public Result<Void> cancel(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId, @PathVariable String orderSn) {
        orderLifecycleService.cancel(memberId, orderSn);
        return Result.ok();
    }

    @Operation(summary = "确认收货")
    @PostMapping("/{orderSn}/receive")
    public Result<Void> receive(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId, @PathVariable String orderSn) {
        orderLifecycleService.receive(memberId, orderSn);
        return Result.ok();
    }

    @Operation(summary = "修改待发货订单收货地址")
    @PutMapping("/{orderSn}/receiver")
    public Result<Void> updateReceiver(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId,
                                       @PathVariable String orderSn,
                                       @Valid @RequestBody OrderReceiverUpdateRequest request) {
        orderLifecycleService.updateReceiver(memberId, orderSn,
                request.receiverName(), request.receiverPhone(),
                request.receiverProvince(), request.receiverCity(),
                request.receiverRegion(), request.receiverDetailAddress());
        return Result.ok();
    }

    @Operation(summary = "发起支付", description = "返回 mock://pay/{orderSn} 或支付宝表单 HTML，并不立刻改订单状态")
    @PostMapping("/{orderSn}/pay")
    public Result<String> pay(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId, @PathVariable String orderSn) {
        return Result.ok(paymentService.pay(memberId, orderSn));
    }

    @Operation(summary = "模拟收银台确认付款", description = "仅 booth.pay.mock-enabled=true 时可用；对应支付宝沙箱点支付")
    @PostMapping("/{orderSn}/pay/mock-confirm")
    public Result<Void> mockConfirmPay(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId,
                                       @PathVariable String orderSn) {
        paymentService.confirmMockPay(memberId, orderSn);
        return Result.ok();
    }

    @Operation(summary = "隐藏已结束的订单")
    @DeleteMapping("/{orderSn}")
    public Result<Void> hide(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId, @PathVariable String orderSn) {
        orderLifecycleService.hide(memberId, orderSn);
        return Result.ok();
    }
}
