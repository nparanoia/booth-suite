package com.booth.trade.controller.open;

import com.booth.common.core.exception.BizException;
import com.booth.common.core.result.Result;
import com.booth.trade.error.TradeErrorCode;
import com.booth.trade.service.SeckillCatalogService;
import com.booth.trade.vo.SeckillSessionVO;
import com.booth.trade.vo.SeckillSkuVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "前台-秒杀")
@RestController
@RequestMapping("/open/seckill")
@RequiredArgsConstructor
public class SeckillOpenController {

    private final SeckillCatalogService seckillCatalogService;

    @Operation(summary = "当前场次")
    @GetMapping("/sessions/current")
    public Result<List<SeckillSessionVO>> current() {
        return Result.ok(seckillCatalogService.current());
    }

    @Operation(summary = "秒杀商品详情")
    @GetMapping("/skus/{id}")
    public Result<SeckillSkuVO> detail(@PathVariable Long id) {
        SeckillSkuVO sku = seckillCatalogService.detail(id);
        BizException.throwIf(sku == null, TradeErrorCode.SECKILL_NOT_FOUND);
        return Result.ok(sku);
    }
}
