package com.booth.common.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.booth.common.core.exception.ErrorCode;
import com.booth.common.core.result.Result;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 过滤器和拦截器在进入 Controller 之前拒绝请求时，全局异常处理接不到，需要自己写出统一格式的响应。
 */
@RequiredArgsConstructor
public class JsonErrorWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, int httpStatus, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(httpStatus);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Result.fail(errorCode, message));
    }
}
