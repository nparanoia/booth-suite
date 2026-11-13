package com.booth.common.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.booth.common.core.constant.AuthHeaders;
import com.booth.common.core.security.RequestSigner;
import com.booth.common.web.handler.GlobalExceptionHandler;
import com.booth.common.web.security.IdentityFilter;
import com.booth.common.web.security.JsonErrorWriter;
import com.booth.common.web.security.PermissionInterceptor;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@AutoConfiguration
@Import(GlobalExceptionHandler.class)
public class WebAutoConfiguration {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer javaTimeFormatCustomizer() {
        return builder -> builder
                .serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME))
                .deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME))
                .serializerByType(LocalDate.class, new LocalDateSerializer(DATE))
                .deserializerByType(LocalDate.class, new LocalDateDeserializer(DATE))
                .serializerByType(LocalTime.class, new LocalTimeSerializer(TIME))
                .deserializerByType(LocalTime.class, new LocalTimeDeserializer(TIME));
    }

    @Bean
    @ConditionalOnMissingBean
    public RequestSigner requestSigner(@Value("${booth.security.secret}") String secret) {
        return new RequestSigner(secret);
    }

    @Bean
    public JsonErrorWriter jsonErrorWriter(ObjectMapper objectMapper) {
        return new JsonErrorWriter(objectMapper);
    }

    @Bean
    public FilterRegistrationBean<IdentityFilter> identityFilter(RequestSigner signer, JsonErrorWriter errorWriter) {
        FilterRegistrationBean<IdentityFilter> registration = new FilterRegistrationBean<>(new IdentityFilter(signer, errorWriter));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 100);
        return registration;
    }

    /**
     * 接口文档里统一用 Authorization 传 Token；网关写入的身份头不需要也不允许客户端传，从文档中隐藏。
     */
    @Bean
    @ConditionalOnMissingBean
    public OpenAPI boothOpenApi() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(HttpHeaders.AUTHORIZATION, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer")))
                .addSecurityItem(new SecurityRequirement().addList(HttpHeaders.AUTHORIZATION));
    }

    @Bean
    public OperationCustomizer hideGatewayHeaders() {
        Set<String> managed = Set.of(AuthHeaders.GATEWAY_MANAGED);
        return (operation, handlerMethod) -> {
            if (operation.getParameters() != null) {
                operation.getParameters().removeIf(p -> "header".equals(p.getIn())
                        && (managed.contains(p.getName()) || HttpHeaders.AUTHORIZATION.equals(p.getName())));
            }
            return operation;
        };
    }

    @Bean
    public WebMvcConfigurer permissionInterceptorConfigurer(JsonErrorWriter errorWriter) {
        PermissionInterceptor interceptor = new PermissionInterceptor(errorWriter);
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor);
            }
        };
    }
}
