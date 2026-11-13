package com.booth.user.controller.admin;

import com.booth.common.core.constant.AuthHeaders;
import com.booth.common.core.result.PageResult;
import com.booth.common.core.result.Result;
import com.booth.common.web.security.RequiresPermission;
import com.booth.user.dto.AdminCreateRequest;
import com.booth.user.dto.AdminQuery;
import com.booth.user.dto.RoleAssignRequest;
import com.booth.user.dto.StatusUpdateRequest;
import com.booth.user.service.AdminUserService;
import com.booth.user.vo.AdminUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-管理员账号")
@RequiresPermission("user:admin:edit")
@RestController
@RequestMapping("/admin/admins")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "分页查询管理员", description = "keyword 匹配用户名或昵称")
    @RequiresPermission("user:admin:view")
    @GetMapping
    public Result<PageResult<AdminUserVO>> page(@Valid AdminQuery query) {
        return Result.ok(adminUserService.page(query));
    }

    @Operation(summary = "新增管理员", description = "新账号没有任何角色，需要再分配角色")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody AdminCreateRequest request) {
        return Result.ok(adminUserService.create(request));
    }

    @Operation(summary = "启用或禁用管理员", description = "禁用后立即踢下线；不能禁用自己")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@RequestHeader(AuthHeaders.ADMIN_ID) Long operatorId,
                                     @PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        adminUserService.updateStatus(operatorId, id, request.status());
        return Result.ok();
    }

    @Operation(summary = "分配角色", description = "全量覆盖；在线管理员的权限立即生效")
    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody RoleAssignRequest request) {
        adminUserService.assignRoles(id, request.roleIds());
        return Result.ok();
    }
}
