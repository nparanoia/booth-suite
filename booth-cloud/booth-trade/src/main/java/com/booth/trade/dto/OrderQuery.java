package com.booth.trade.dto;

import com.booth.common.mybatis.page.PageQuery;
import com.booth.trade.enums.OrderStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderQuery extends PageQuery {

    private OrderStatus status;
    private String orderSn;
}
