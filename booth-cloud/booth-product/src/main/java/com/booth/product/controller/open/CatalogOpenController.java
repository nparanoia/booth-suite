package com.booth.product.controller.open;

import com.booth.common.core.result.PageResult;
import com.booth.common.core.result.Result;
import com.booth.product.dto.ProductQuery;
import com.booth.product.service.CategoryService;
import com.booth.product.service.ProductBrowseService;
import com.booth.product.vo.CategoryTreeVO;
import com.booth.product.vo.ProductDetailVO;
import com.booth.product.vo.ProductListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "前台-商品浏览")
@RestController
@RequestMapping("/open")
@RequiredArgsConstructor
public class CatalogOpenController {

    private final CategoryService categoryService;
    private final ProductBrowseService browseService;

    @Operation(summary = "分类树（只含展示中的分类）")
    @GetMapping("/categories/tree")
    public Result<List<CategoryTreeVO>> categoryTree() {
        return Result.ok(categoryService.tree(true));
    }

    @Operation(summary = "商品列表（只含上架商品）")
    @GetMapping("/products")
    public Result<PageResult<ProductListVO>> page(@Valid ProductQuery query) {
        return Result.ok(browseService.page(query));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/products/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        return Result.ok(browseService.detail(id));
    }
}
