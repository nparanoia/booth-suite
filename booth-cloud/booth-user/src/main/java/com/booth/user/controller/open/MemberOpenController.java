package com.booth.user.controller.open;

import com.booth.common.core.result.Result;
import com.booth.user.dto.LoginRequest;
import com.booth.user.dto.MemberRegisterRequest;
import com.booth.user.service.MemberService;
import com.booth.user.vo.TokenVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "前台-会员注册与登录")
@RestController
@RequestMapping("/open/members")
@RequiredArgsConstructor
public class MemberOpenController {

    private final MemberService memberService;

    @Operation(summary = "会员注册")
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody MemberRegisterRequest request) {
        return Result.ok(memberService.register(request));
    }

    @Operation(summary = "会员登录", description = "返回的 token 放在请求头 Authorization: Bearer {token}")
    @PostMapping("/login")
    public Result<TokenVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return Result.ok(memberService.login(request, clientIp(httpRequest)));
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            String[] parts = forwarded.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
