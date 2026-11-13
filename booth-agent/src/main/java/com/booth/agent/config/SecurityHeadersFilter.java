package com.booth.agent.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** 基础浏览器安全头，演示站也要有，避免面试被问「有没有防点击劫持 / MIME 嗅探」。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        // 演示页需要内联脚本；正式前端工程用更严的 CSP（见 booth-web）
        if (!response.containsHeader("Content-Security-Policy")) {
            response.setHeader("Content-Security-Policy",
                    "default-src 'self'; img-src 'self' data: https:; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                            + "font-src 'self' https://fonts.gstatic.com data:; "
                            + "script-src 'self' 'unsafe-inline'; connect-src 'self' http://localhost:8080 http://127.0.0.1:8080; "
                            + "frame-ancestors 'none'; base-uri 'self'; form-action 'self'");
        }
        filterChain.doFilter(request, response);
    }
}
