package com.booth.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.booth.common.core.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关直接拒绝请求时写出与下游服务一致的 Result JSON。
 */
@Component
@RequiredArgsConstructor
public class GatewayResponses {

    private final ObjectMapper objectMapper;

    public Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, Result<?> result) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer body = response.bufferFactory().wrap(toJson(result));
        return response.writeWith(Mono.just(body));
    }

    private byte[] toJson(Result<?> result) {
        try {
            return objectMapper.writeValueAsBytes(result);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
