package com.booth.gateway.filter;

import com.booth.common.core.constant.AuthHeaders;
import com.booth.common.core.exception.CommonErrorCode;
import com.booth.common.core.result.Result;
import com.booth.common.core.security.RequestSigner;
import com.booth.common.satoken.AdminPermsCache;
import com.booth.common.satoken.StpKit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 统一鉴权：
 * <ol>
 *     <li>删除客户端自带的全部身份头，防止伪造</li>
 *     <li>/{服务}/admin/** 用管理员体系校验 Token，并从 Redis 读取权限码；其余路径按会员体系解析（/app/** 必须登录）</li>
 *     <li>把身份写入请求头并签名，下游服务验签后使用</li>
 * </ol>
 * Sa-Token 查询 Redis 是阻塞调用，放到 boundedElastic 线程池执行，不占用 Netty 事件循环线程。
 */
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    private static final PathPattern APP = PathPatternParser.defaultInstance.parse("/*/app/**");
    private static final PathPattern ADMIN = PathPatternParser.defaultInstance.parse("/*/admin/**");

    private final RequestSigner signer;
    private final StringRedisTemplate redis;
    private final GatewayResponses responses;

    public AuthFilter(@Value("${booth.security.secret}") String secret, StringRedisTemplate redis, GatewayResponses responses) {
        this.signer = new RequestSigner(secret);
        this.redis = redis;
        this.responses = responses;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        PathContainer path = request.getPath().pathWithinApplication();
        boolean adminPath = ADMIN.matches(path);
        boolean appPath = APP.matches(path);
        String token = StpKit.bearerToken(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));

        return Mono.fromCallable(() -> resolveIdentity(token, adminPath))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(identity -> {
                    if ((adminPath && identity.adminId() == null) || (appPath && identity.memberId() == null)) {
                        return responses.reject(exchange, HttpStatus.UNAUTHORIZED, Result.fail(CommonErrorCode.UNAUTHORIZED));
                    }
                    ServerHttpRequest mutated = request.mutate().headers(headers -> writeIdentity(headers, identity)).build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                });
    }

    private Identity resolveIdentity(String token, boolean adminPath) {
        if (token == null) {
            return Identity.ANONYMOUS;
        }
        if (adminPath) {
            Object adminId = StpKit.ADMIN.getLoginIdByToken(token);
            if (adminId == null) {
                return Identity.ANONYMOUS;
            }
            String perms = redis.opsForValue().get(AdminPermsCache.key(adminId));
            return new Identity(null, adminId.toString(), perms == null ? "" : perms);
        }
        Object memberId = StpKit.MEMBER.getLoginIdByToken(token);
        return memberId == null ? Identity.ANONYMOUS : new Identity(memberId.toString(), null, null);
    }

    private void writeIdentity(HttpHeaders headers, Identity identity) {
        for (String name : AuthHeaders.GATEWAY_MANAGED) {
            headers.remove(name);
        }
        if (identity == Identity.ANONYMOUS) {
            return;
        }
        long now = System.currentTimeMillis() / 1000;
        if (identity.memberId() != null) {
            headers.set(AuthHeaders.MEMBER_ID, identity.memberId());
        }
        if (identity.adminId() != null) {
            headers.set(AuthHeaders.ADMIN_ID, identity.adminId());
            headers.set(AuthHeaders.ADMIN_PERMS, identity.perms());
        }
        headers.set(AuthHeaders.AUTH_TIME, String.valueOf(now));
        headers.set(AuthHeaders.AUTH_SIGN, signer.signIdentity(identity.memberId(), identity.adminId(), identity.perms(), now));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    private record Identity(String memberId, String adminId, String perms) {
        static final Identity ANONYMOUS = new Identity(null, null, null);
    }
}
