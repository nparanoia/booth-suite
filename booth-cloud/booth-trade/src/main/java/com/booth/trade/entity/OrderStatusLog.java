package com.booth.trade.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseCreateEntity;
import com.booth.trade.enums.OrderStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("order_status_log")
public class OrderStatusLog extends BaseCreateEntity {

    private Long orderId;
    private OrderStatus fromStatus;
    private OrderStatus toStatus;
    /** 1 会员，2 系统，3 管理员 */
    private Integer operatorType;
    private String operator;
    private String note;
}
