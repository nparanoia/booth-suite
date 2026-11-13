package com.booth.api.trade.dto;

/**
 * @param exists 订单号是否已落库
 * @param status 0 待付款，1 待发货，2 已发货，3 已完成，4 已关闭；不存在时为 null
 */
public record OrderStatusDTO(boolean exists, Integer status) {
}
