package com.booth.trade.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseEntity;
import com.booth.trade.enums.ReleaseStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@TableName("seckill_activity")
public class SeckillActivity extends BaseEntity {

    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private ReleaseStatus status;
}
