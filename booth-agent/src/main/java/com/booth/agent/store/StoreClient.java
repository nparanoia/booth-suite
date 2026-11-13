package com.booth.agent.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.booth.agent.common.ToolErrorKind;
import com.booth.agent.common.ToolExecutionException;
import com.booth.agent.config.BoothAgentProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * 经 booth-cloud 网关调用前台接口。透传会员 Bearer，由网关鉴权。
 * 写售后经 WriteActionService（指纹/outbox）调用；工具层勿绕过直调。
 */
@Component
@RequiredArgsConstructor
public class StoreClient implements StoreApi {

    private static final String ORDER_SN = "^[0-9]{8,32}$";

    private final RestClient.Builder restClientBuilder;
    private final BoothAgentProperties properties;

    @Override
    public JsonNode getOrder(String token, String orderSn) {
        requireOrderSn(orderSn);
        return StoreErrors.unwrap(exchange(token, client -> client.get().uri("/trade/app/orders/" + orderSn)));
    }

    @Override
    public JsonNode listReturns(String token) {
        return StoreErrors.unwrap(exchange(token, client -> client.get().uri("/trade/app/returns")));
    }

    @Override
    public JsonNode applyReturn(String token, String orderSn, Map<String, Object> body) {
        requireOrderSn(orderSn);
        return StoreErrors.unwrap(exchange(token, client -> client.post()
                .uri("/trade/app/orders/" + orderSn + "/returns")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)));
    }

    @Override
    public JsonNode cancelOrder(String token, String orderSn) {
        requireOrderSn(orderSn);
        return StoreErrors.unwrap(exchange(token, client -> client.post()
                .uri("/trade/app/orders/" + orderSn + "/cancel")));
    }

    @Override
    public JsonNode updateReceiver(String token, String orderSn, Map<String, Object> body) {
        requireOrderSn(orderSn);
        return StoreErrors.unwrap(exchange(token, client -> client.put()
                .uri("/trade/app/orders/" + orderSn + "/receiver")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)));
    }

    private JsonNode exchange(String token, RequestCaller caller) {
        try {
            return caller.call(client(token)).retrieve().body(JsonNode.class);
        } catch (RestClientResponseException exception) {
            throw StoreErrors.response(exception);
        } catch (ResourceAccessException exception) {
            throw StoreErrors.access(exception);
        } catch (RestClientException exception) {
            throw ToolExecutionException.booth(ToolErrorKind.REJECTED, 502, "商城返回无法识别");
        }
    }

    private RestClient client(String token) {
        return restClientBuilder.clone()
                .baseUrl(properties.gatewayBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
    }

    private static void requireOrderSn(String orderSn) {
        if (orderSn == null || !orderSn.matches(ORDER_SN)) {
            throw ToolExecutionException.ready(ToolErrorKind.PARAM, 400, "订单号格式不正确");
        }
    }

    @FunctionalInterface
    private interface RequestCaller {
        RestClient.RequestHeadersSpec<?> call(RestClient client);
    }
}
