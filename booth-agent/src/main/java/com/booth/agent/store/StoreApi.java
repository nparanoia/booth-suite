package com.booth.agent.store;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/** 商城网关调用。成功时返回 Result.data，业务失败抛 ToolExecutionException。 */
public interface StoreApi {

    JsonNode getOrder(String token, String orderSn);

    JsonNode listReturns(String token);

    JsonNode applyReturn(String token, String orderSn, Map<String, Object> body);

    /** 取消待付款订单。成功时 data 可为 null。 */
    JsonNode cancelOrder(String token, String orderSn);

    /** 修改待发货订单收货地址。成功时 data 可为 null。 */
    JsonNode updateReceiver(String token, String orderSn, Map<String, Object> body);
}
