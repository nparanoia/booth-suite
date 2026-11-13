package com.booth.api.config;

import com.booth.common.core.constant.AuthHeaders;
import com.booth.common.core.security.RequestSigner;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * 调用方引入 openfeign 后自动生效：通过本模块的 Feign 契约调用其他服务时附加服务间签名，提供方由 IdentityFilter 校验。
 * 按接口所在包判断而不是按路径：@FeignClient 的 path 会并入目标地址，拦截器里拿到的 path 不含 /inner 前缀；
 * 同时保证签名不会带给第三方接口。
 */
@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
public class InnerAuthAutoConfiguration {

    private static final String CONTRACT_PACKAGE = "com.booth.api";

    @Bean
    public RequestInterceptor innerAuthRequestInterceptor(@Value("${booth.security.secret}") String secret) {
        RequestSigner signer = new RequestSigner(secret);
        return template -> {
            if (template.feignTarget() == null
                    || !template.feignTarget().type().getPackageName().startsWith(CONTRACT_PACKAGE)) {
                return;
            }
            long now = System.currentTimeMillis() / 1000;
            template.header(AuthHeaders.INNER_TIME, String.valueOf(now));
            template.header(AuthHeaders.INNER_SIGN, signer.signInner(now));
        };
    }
}
