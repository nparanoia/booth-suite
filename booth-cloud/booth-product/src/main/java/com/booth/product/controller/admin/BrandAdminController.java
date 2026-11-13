package com.booth.product.controller.admin;

import com.booth.common.core.result.PageResult;
import com.booth.common.core.result.Result;
import com.booth.common.web.security.RequiresPermission;
import com.booth.product.dto.BrandQuery;
import com.booth.product.dto.BrandSaveRequest;
import com.booth.product.service.BrandService;
import com.booth.product.vo.BrandVO;
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
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-品牌")
@RequiresPermission("product:brand:edit")
@RestController
@RequestMapping("/admin/brands")
@RequiredArgsConstructor
public class BrandAdminController {

    private final BrandService brandService;

    @Operation(summary = "分页查询品牌")
    @RequiresPermission("product:brand:view")
    @GetMapping
    public Result<PageResult<BrandVO>> page(@Valid BrandQuery query) {
        return Result.ok(brandService.page(query));
    }

    @Operation(summary = "品牌详情")
    @RequiresPermission("product:brand:view")
    @GetMapping("/{id}")
    public Result<BrandVO> get(@PathVariable Long id) {
        return Result.ok(brandService.get(id));
    }

    @Operation(summary = "新增品牌")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody BrandSaveRequest request) {
        return Result.ok(brandService.create(request));
    }

    @Operation(summary = "修改品牌", description = "改名会同步商品表里冗余的品牌名")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody BrandSaveRequest request) {
        brandService.update(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除品牌", description = "品牌下还有商品时不允许删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        brandService.delete(id);
        return Result.ok();
    }
}
