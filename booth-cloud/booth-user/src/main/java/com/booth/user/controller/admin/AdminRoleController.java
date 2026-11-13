package com.booth.user.controller.admin;

import com.booth.common.core.result.Result;
import com.booth.common.web.security.RequiresPermission;
import com.booth.user.dto.PermissionAssignRequest;
import com.booth.user.dto.RoleCreateRequest;
import com.booth.user.dto.RoleUpdateRequest;
import com.booth.user.service.AdminPermissionService;
import com.booth.user.service.AdminRoleService;
import com.booth.user.vo.PermissionTreeVO;
import com.booth.user.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "后台-角色与权限")
@RequiresPermission("user:role:edit")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminRoleController {

    private final AdminRoleService roleService;
    private final AdminPermissionService permissionService;

    @Operation(summary = "权限树", description = "模块 → 操作两级")
    @RequiresPermission("user:role:view")
    @GetMapping("/permissions")
    public Result<List<PermissionTreeVO>> permissionTree() {
        return Result.ok(permissionService.tree());
    }

    @Operation(summary = "角色列表")
    @RequiresPermission("user:role:view")
    @GetMapping("/roles")
    public Result<List<RoleVO>> list() {
        return Result.ok(roleService.list());
    }

    @Operation(summary = "新增角色")
    @PostMapping("/roles")
    public Result<Long> create(@Valid @RequestBody RoleCreateRequest request) {
        return Result.ok(roleService.create(request));
    }

    @Operation(summary = "修改角色", description = "编码不可修改；禁用角色后拥有它的管理员立即失去对应权限")
    @PutMapping("/roles/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        roleService.update(id, request);
        return Result.ok();
    }

    @Operation(summary = "角色已分配的权限 ID")
    @RequiresPermission("user:role:view")
    @GetMapping("/roles/{id}/permissions")
    public Result<List<Long>> permissionIds(@PathVariable Long id) {
        return Result.ok(roleService.permissionIds(id));
    }

    @Operation(summary = "分配权限", description = "全量覆盖；在线管理员的权限立即生效")
    @PutMapping("/roles/{id}/permissions")
    public Result<Void> assignPermissions(@PathVariable Long id, @Valid @RequestBody PermissionAssignRequest request) {
        roleService.assignPermissions(id, request.permissionIds());
        return Result.ok();
    }
}
