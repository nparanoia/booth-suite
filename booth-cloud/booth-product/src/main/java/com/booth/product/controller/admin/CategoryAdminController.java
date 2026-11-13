package com.booth.product.controller.admin;

import com.booth.common.core.result.Result;
import com.booth.common.web.security.RequiresPermission;
import com.booth.product.dto.CategorySaveRequest;
import com.booth.product.service.CategoryService;
import com.booth.product.vo.CategoryTreeVO;
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

import java.util.List;

@Tag(name = "后台-商品分类")
@RequiresPermission("product:category:edit")
@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class CategoryAdminController {

    private final CategoryService categoryService;

    @Operation(summary = "分类树（包含隐藏分类）")
    @RequiresPermission("product:category:view")
    @GetMapping("/tree")
    public Result<List<CategoryTreeVO>> tree() {
        return Result.ok(categoryService.tree(false));
    }

    @Operation(summary = "新增分类", description = "不传 parentId 为一级分类，最多两级")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CategorySaveRequest request) {
        return Result.ok(categoryService.create(request));
    }

    @Operation(summary = "修改分类")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CategorySaveRequest request) {
        categoryService.update(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除分类", description = "有子分类或商品时不允许删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok();
    }
}
