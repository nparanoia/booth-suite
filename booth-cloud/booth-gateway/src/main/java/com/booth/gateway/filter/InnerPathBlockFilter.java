package com.booth.gateway.filter;

import com.booth.common.core.exception.CommonErrorCode;
import com.booth.common.core.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

/**
 * 约定 /{服务前缀}/inner/** 为服务间调用接口，只允许内网 Feign 直连，网关一律拒绝。
 * 用 PathPattern 按解码后的路径匹配，/product/%69nner/... 这类编码路径同样会被拦截。
 */
@Component
@RequiredArgsConstructor
public class InnerPathBlockFilter implements GlobalFilter, Ordered {

    private static final PathPattern INNER = PathPatternParser.defaultInstance.parse("/*/inner/**");

    private final GatewayResponses responses;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!INNER.matches(exchange.getRequest().getPath().pathWithinApplication())) {
            return chain.filter(exchange);
        }
        return responses.reject(exchange, HttpStatus.FORBIDDEN, Result.fail(CommonErrorCode.FORBIDDEN));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
