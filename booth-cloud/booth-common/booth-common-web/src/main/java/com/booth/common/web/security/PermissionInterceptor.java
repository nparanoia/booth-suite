package com.booth.common.web.security;

import com.booth.common.core.exception.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * 按 {@link RequiresPermission} 校验权限码。权限码由网关从 Redis 读出、签名后放在请求头里，
 * {@link IdentityFilter} 验签后解析成集合，这里只做比对，不查库。
 */
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private static final String WILDCARD = "*";

    private final JsonErrorWriter errorWriter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        RequiresPermission required = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), RequiresPermission.class);
        if (required == null) {
            required = AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), RequiresPermission.class);
        }
        if (required == null) {
            return true;
        }

        @SuppressWarnings("unchecked")
        Set<String> perms = (Set<String>) request.getAttribute(IdentityFilter.ADMIN_PERMS_ATTR);
        if (perms == null) {
            errorWriter.write(response, HttpStatus.UNAUTHORIZED.value(), CommonErrorCode.UNAUTHORIZED, null);
            return false;
        }
        if (perms.contains(WILDCARD) || perms.contains(required.value())) {
            return true;
        }
        errorWriter.write(response, HttpStatus.FORBIDDEN.value(), CommonErrorCode.FORBIDDEN,
                "缺少权限：" + required.value());
        return false;
    }
}
