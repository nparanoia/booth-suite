package com.booth.trade.controller.inner;

import com.booth.api.trade.dto.OrderStatusDTO;
import com.booth.common.core.result.Result;
import com.booth.trade.entity.TradeOrder;
import com.booth.trade.service.OrderLifecycleService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequestMapping("/inner/orders")
@RequiredArgsConstructor
public class OrderInnerController {

    private final OrderLifecycleService orderLifecycleService;

    @GetMapping("/{orderSn}/status")
    public Result<OrderStatusDTO> status(@PathVariable String orderSn) {
        TradeOrder order = orderLifecycleService.find(orderSn);
        if (order == null) {
            return Result.ok(new OrderStatusDTO(false, null));
        }
        return Result.ok(new OrderStatusDTO(true, order.getStatus().getCode()));
    }
}
