package com.booth.trade.controller.app;

import com.booth.common.core.constant.AuthHeaders;
import com.booth.common.core.result.Result;
import com.booth.trade.dto.SeckillOrderRequest;
import com.booth.trade.service.SeckillBuyService;
import com.booth.trade.vo.SeckillResultVO;
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

@Tag(name = "前台-秒杀抢购")
@RestController
@RequestMapping("/app/seckill")
@RequiredArgsConstructor
public class SeckillAppController {

    private final SeckillBuyService seckillBuyService;

    @Operation(summary = "获取一次性抢购地址")
    @PostMapping("/{id}/path")
    public Result<String> path(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId, @PathVariable Long id) {
        return Result.ok(seckillBuyService.path(memberId, id));
    }

    @Operation(summary = "抢购", description = "立刻返回 requestId，用结果接口看是否下单成功")
    @PostMapping("/{id}/orders")
    public Result<String> buy(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId, @PathVariable Long id,
                              @Valid @RequestBody SeckillOrderRequest request) {
        return Result.ok(seckillBuyService.buy(id, memberId, request.path(), request.quantity(), request.addressId()));
    }

    @Operation(summary = "抢购结果")
    @GetMapping("/results/{requestId}")
    public Result<SeckillResultVO> result(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId,
                                          @PathVariable String requestId) {
        return Result.ok(seckillBuyService.result(memberId, requestId));
    }
}
