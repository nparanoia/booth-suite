package com.booth.trade.config;

import com.booth.common.core.exception.CommonErrorCode;
import com.booth.common.core.result.Result;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 下游服务不可达、超时或返回非 2xx 时 Feign 抛 FeignException，统一转成 503，
 * 优先级高于公共的兜底处理，否则会被当成 500 未知异常。
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class RemoteCallExceptionHandler {

    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(FeignException.class)
    public Result<Void> handleFeign(FeignException e) {
        log.warn("远程调用失败: status={}, {}", e.status(), e.getMessage());
        return Result.fail(CommonErrorCode.SERVICE_UNAVAILABLE);
    }
}
