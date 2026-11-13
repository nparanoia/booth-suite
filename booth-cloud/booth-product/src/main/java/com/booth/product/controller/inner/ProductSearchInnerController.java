package com.booth.product.controller.inner;

import com.booth.api.product.dto.ProductIdQuery;
import com.booth.api.product.dto.ProductSearchDocDTO;
import com.booth.api.product.dto.ProductSearchExportDTO;
import com.booth.common.core.result.Result;
import com.booth.product.service.ProductSearchDocService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 实现 {@link com.booth.api.product.ProductSearchClient}。
 */
@Hidden
@Validated
@RestController
@RequestMapping("/inner/products")
@RequiredArgsConstructor
public class ProductSearchInnerController {

    private final ProductSearchDocService productSearchDocService;

    @PostMapping("/search-docs")
    public Result<List<ProductSearchDocDTO>> docs(@RequestBody @Size(max = 200) List<Long> ids) {
        return Result.ok(productSearchDocService.docs(ids));
    }

    @GetMapping("/search-docs/export")
    public Result<ProductSearchExportDTO> export(@RequestParam @Min(0) long afterId,
                                                 @RequestParam @Min(1) @Max(200) int size) {
        return Result.ok(productSearchDocService.export(afterId, size));
    }

    @PostMapping("/search-docs/ids")
    public Result<List<Long>> ids(@Valid @RequestBody ProductIdQuery query) {
        return Result.ok(productSearchDocService.ids(query));
    }
}
