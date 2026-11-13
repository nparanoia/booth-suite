package com.booth.product.controller.admin;

import com.booth.common.core.result.Result;
import com.booth.common.web.security.RequiresPermission;
import com.booth.product.dto.AttrGroupSaveRequest;
import com.booth.product.dto.AttrSaveRequest;
import com.booth.product.enums.AttrType;
import com.booth.product.service.AttrService;
import com.booth.product.vo.AttrGroupVO;
import com.booth.product.vo.AttrVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "后台-商品属性")
@RequiresPermission("product:attr:edit")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AttrAdminController {

    private final AttrService attrService;

    @Operation(summary = "属性分组列表")
    @RequiresPermission("product:attr:view")
    @GetMapping("/attr-groups")
    public Result<List<AttrGroupVO>> listGroups() {
        return Result.ok(attrService.listGroups());
    }

    @Operation(summary = "新增属性分组")
    @PostMapping("/attr-groups")
    public Result<Long> createGroup(@Valid @RequestBody AttrGroupSaveRequest request) {
        return Result.ok(attrService.createGroup(request));
    }

    @Operation(summary = "修改属性分组")
    @PutMapping("/attr-groups/{id}")
    public Result<Void> updateGroup(@PathVariable Long id, @Valid @RequestBody AttrGroupSaveRequest request) {
        attrService.updateGroup(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除属性分组")
    @DeleteMapping("/attr-groups/{id}")
    public Result<Void> deleteGroup(@PathVariable Long id) {
        attrService.deleteGroup(id);
        return Result.ok();
    }

    @Operation(summary = "分组下的属性", description = "type 不传返回全部")
    @RequiresPermission("product:attr:view")
    @GetMapping("/attr-groups/{groupId}/attrs")
    public Result<List<AttrVO>> listAttrs(@PathVariable Long groupId, @RequestParam(required = false) AttrType type) {
        return Result.ok(attrService.listAttrs(groupId, type));
    }

    @Operation(summary = "新增属性")
    @PostMapping("/attrs")
    public Result<Long> createAttr(@Valid @RequestBody AttrSaveRequest request) {
        return Result.ok(attrService.createAttr(request));
    }

    @Operation(summary = "修改属性")
    @PutMapping("/attrs/{id}")
    public Result<Void> updateAttr(@PathVariable Long id, @Valid @RequestBody AttrSaveRequest request) {
        attrService.updateAttr(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除属性", description = "已被商品使用时不允许删除")
    @DeleteMapping("/attrs/{id}")
    public Result<Void> deleteAttr(@PathVariable Long id) {
        attrService.deleteAttr(id);
        return Result.ok();
    }
}
