package com.booth.trade.controller.app;

import com.booth.common.core.constant.AuthHeaders;
import com.booth.common.core.result.Result;
import com.booth.trade.dto.ReturnApplyRequest;
import com.booth.trade.service.ReturnService;
import com.booth.trade.vo.ReturnApplyVO;
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

import java.util.List;

@Tag(name = "前台-售后")
@RestController
@RequestMapping("/app")
@RequiredArgsConstructor
public class ReturnAppController {

    private final ReturnService returnService;

    @Operation(summary = "申请退货", description = "仅已完成订单，一个订单明细只能申请一次")
    @PostMapping("/orders/{orderSn}/returns")
    public Result<Long> apply(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId, @PathVariable String orderSn,
                              @Valid @RequestBody ReturnApplyRequest request) {
        return Result.ok(returnService.apply(memberId, orderSn, request));
    }

    @Operation(summary = "我的售后")
    @GetMapping("/returns")
    public Result<List<ReturnApplyVO>> mine(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId) {
        return Result.ok(returnService.mine(memberId));
    }
}
