package com.booth.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseEntity;
import com.booth.product.enums.StockLockStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("stock_lock_record")
public class StockLockRecord extends BaseEntity {

    private String orderSn;
    private Long skuId;
    private Integer quantity;
    private StockLockStatus status;
}
