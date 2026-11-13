package com.booth.api.trade;

import com.booth.api.trade.dto.OrderStatusDTO;
import com.booth.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "booth-trade", contextId = "orderInnerClient", path = "/inner/orders")
public interface OrderInnerClient {

    @GetMapping("/{orderSn}/status")
    Result<OrderStatusDTO> status(@PathVariable("orderSn") String orderSn);
}
