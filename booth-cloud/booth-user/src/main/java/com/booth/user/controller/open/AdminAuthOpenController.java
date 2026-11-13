package com.booth.user.controller.open;

import com.booth.common.core.result.Result;
import com.booth.user.dto.LoginRequest;
import com.booth.user.service.AdminAuthService;
import com.booth.user.vo.TokenVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-登录")
@RestController
@RequestMapping("/open/admin-auth")
@RequiredArgsConstructor
public class AdminAuthOpenController {

    private final AdminAuthService adminAuthService;

    @Operation(summary = "管理员登录", description = "返回的 token 放在请求头 Authorization: Bearer {token}")
    @PostMapping("/login")
    public Result<TokenVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return Result.ok(adminAuthService.login(request, clientIp(httpRequest), httpRequest.getHeader(HttpHeaders.USER_AGENT)));
    }

    /**
     * 网关会把它看到的客户端地址追加到 X-Forwarded-For 末尾；前面的部分客户端可以自己伪造，只取最后一段。
     */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            String[] parts = forwarded.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
