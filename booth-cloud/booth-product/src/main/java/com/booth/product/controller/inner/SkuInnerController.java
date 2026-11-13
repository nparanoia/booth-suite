package com.booth.product.controller.inner;

import com.booth.api.product.dto.SkuInfoDTO;
import com.booth.common.core.result.Result;
import com.booth.product.service.SkuInnerService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 实现 {@link com.booth.api.product.SkuInnerClient}，路径和参数必须与它保持一致。
 */
@Hidden
@RestController
@RequestMapping("/inner/skus")
@RequiredArgsConstructor
public class SkuInnerController {

    private final SkuInnerService skuInnerService;

    @PostMapping("/batch")
    public Result<List<SkuInfoDTO>> listByIds(@RequestBody @Size(max = 200) List<Long> skuIds) {
        return Result.ok(skuInnerService.listByIds(skuIds));
    }
}
