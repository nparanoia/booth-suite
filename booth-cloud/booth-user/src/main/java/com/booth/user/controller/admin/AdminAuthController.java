package com.booth.user.controller.admin;

import com.booth.common.core.constant.AuthHeaders;
import com.booth.common.core.result.Result;
import com.booth.common.satoken.StpKit;
import com.booth.user.service.AdminAuthService;
import com.booth.user.vo.AdminProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任何已登录的管理员都可以访问，不需要权限码。
 */
@Tag(name = "后台-当前管理员")
@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @Operation(summary = "当前管理员信息", description = "包含角色和权限码，前端据此控制菜单和按钮")
    @GetMapping("/me")
    public Result<AdminProfileVO> me(@RequestHeader(AuthHeaders.ADMIN_ID) Long adminId) {
        return Result.ok(adminAuthService.me(adminId));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        adminAuthService.logout(StpKit.bearerToken(authorization));
        return Result.ok();
    }
}
