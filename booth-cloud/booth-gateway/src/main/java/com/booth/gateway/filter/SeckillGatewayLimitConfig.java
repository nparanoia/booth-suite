package com.booth.gateway.filter;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.util.Set;

@Configuration
public class SeckillGatewayLimitConfig {

    private static final String BODY = "{\"code\":30508,\"message\":\"抢购人数太多，请稍后再试\",\"success\":false}";
    private static final PathPattern BUY = PathPatternParser.defaultInstance.parse("/trade/app/seckill/*/orders");

    private final Counter requests;
    private final Counter limited;

    public SeckillGatewayLimitConfig(MeterRegistry registry) {
        this.requests = Counter.builder("booth.seckill.requests")
                .description("经过网关的秒杀提交次数")
                .register(registry);
        this.limited = Counter.builder("booth.seckill.limited")
                .description("秒杀被限流的次数")
                .tag("source", "gateway")
                .register(registry);
    }

    @PostConstruct
    public void load() {
        GatewayFlowRule rule = new GatewayFlowRule("booth-trade-seckill-buy");
        rule.setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_ROUTE_ID);
        rule.setCount(25);
        rule.setIntervalSec(1);
        GatewayRuleManager.loadRules(Set.of(rule));
        GatewayCallbackManager.setBlockHandler((exchange, ex) -> {
            if (isBuy(exchange)) {
                requests.increment();
                limited.increment();
            }
            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Mono.just(BODY), String.class);
        });
    }

    /** 限流发生在更早的过滤器里，被拦住的请求不会走到这里，避免和拦截回调重复计数。 */
    @Bean
    public GlobalFilter seckillRequestCounter() {
        return new GlobalFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                if (isBuy(exchange)) {
                    requests.increment();
                }
                return chain.filter(exchange);
            }
        };
    }

    private static boolean isBuy(ServerWebExchange exchange) {
        if (!HttpMethod.POST.equals(exchange.getRequest().getMethod())) {
            return false;
        }
        PathContainer path = exchange.getRequest().getPath().pathWithinApplication();
        return BUY.matches(path);
    }
}
