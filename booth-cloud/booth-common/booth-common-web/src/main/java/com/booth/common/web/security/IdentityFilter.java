package com.booth.common.web.security;

import com.booth.common.core.constant.AuthHeaders;
import com.booth.common.core.exception.CommonErrorCode;
import com.booth.common.core.security.RequestSigner;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 下游服务的身份校验，不依赖“请求一定经过网关”：
 * <ul>
 *     <li>带身份头的请求必须有网关签名，否则视为伪造</li>
 *     <li>/app/** 要求已验证的会员身份，/admin/** 要求已验证的管理员身份</li>
 *     <li>/inner/** 要求服务间调用签名</li>
 * </ul>
 * 路径用 PathPattern 匹配（会先解码再比较），与 Spring MVC 的路由口径一致，避免 /%61pp/... 这类编码路径绕过。
 */
@RequiredArgsConstructor
public class IdentityFilter extends OncePerRequestFilter {

    /** 当前管理员权限码集合，供 {@link PermissionInterceptor} 读取 */
    public static final String ADMIN_PERMS_ATTR = IdentityFilter.class.getName() + ".ADMIN_PERMS";

    private static final PathPattern APP = PathPatternParser.defaultInstance.parse("/app/**");
    private static final PathPattern ADMIN = PathPatternParser.defaultInstance.parse("/admin/**");
    private static final PathPattern INNER = PathPatternParser.defaultInstance.parse("/inner/**");

    private final RequestSigner signer;
    private final JsonErrorWriter errorWriter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        PathContainer path = RequestPath.parse(request.getRequestURI(), request.getContextPath()).pathWithinApplication();
        long now = System.currentTimeMillis() / 1000;

        if (INNER.matches(path)) {
            if (!verifyInner(request, now)) {
                errorWriter.write(response, HttpStatus.FORBIDDEN.value(), CommonErrorCode.FORBIDDEN, "仅限服务间调用");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        String memberId = request.getHeader(AuthHeaders.MEMBER_ID);
        String adminId = request.getHeader(AuthHeaders.ADMIN_ID);
        String perms = request.getHeader(AuthHeaders.ADMIN_PERMS);
        boolean hasIdentity = memberId != null || adminId != null || perms != null;

        if (hasIdentity && !verifyIdentity(request, memberId, adminId, perms, now)) {
            errorWriter.write(response, HttpStatus.UNAUTHORIZED.value(), CommonErrorCode.UNAUTHORIZED, "身份签名无效");
            return;
        }
        if (APP.matches(path) && memberId == null) {
            errorWriter.write(response, HttpStatus.UNAUTHORIZED.value(), CommonErrorCode.UNAUTHORIZED, null);
            return;
        }
        if (ADMIN.matches(path) && adminId == null) {
            errorWriter.write(response, HttpStatus.UNAUTHORIZED.value(), CommonErrorCode.UNAUTHORIZED, null);
            return;
        }
        if (adminId != null) {
            request.setAttribute(ADMIN_PERMS_ATTR, parsePerms(perms));
        }
        chain.doFilter(request, response);
    }

    private boolean verifyIdentity(HttpServletRequest request, String memberId, String adminId, String perms, long now) {
        Long ts = parseLong(request.getHeader(AuthHeaders.AUTH_TIME));
        if (ts == null || !RequestSigner.withinSkew(ts, now)) {
            return false;
        }
        String expected = signer.signIdentity(memberId, adminId, perms, ts);
        return RequestSigner.matches(expected, request.getHeader(AuthHeaders.AUTH_SIGN));
    }

    private boolean verifyInner(HttpServletRequest request, long now) {
        Long ts = parseLong(request.getHeader(AuthHeaders.INNER_TIME));
        if (ts == null || !RequestSigner.withinSkew(ts, now)) {
            return false;
        }
        return RequestSigner.matches(signer.signInner(ts), request.getHeader(AuthHeaders.INNER_SIGN));
    }

    private static Set<String> parsePerms(String perms) {
        if (perms == null || perms.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(perms.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Long parseLong(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
