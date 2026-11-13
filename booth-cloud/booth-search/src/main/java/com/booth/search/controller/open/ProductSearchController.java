package com.booth.search.controller.open;

import com.booth.common.core.result.Result;
import com.booth.search.dto.ProductSearchQuery;
import com.booth.search.service.ProductSearchService;
import com.booth.search.vo.ProductSearchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "前台-商品搜索")
@RestController
@RequestMapping("/open/search")
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    @Operation(summary = "搜索上架商品", description = "attrs 重复传，每一项是 属性名:属性值。sort 取值 DEFAULT、SALE、PRICE_ASC、PRICE_DESC、NEW")
    @GetMapping("/products")
    public Result<ProductSearchVO> search(@Valid ProductSearchQuery query) {
        return Result.ok(productSearchService.search(query));
    }
}
