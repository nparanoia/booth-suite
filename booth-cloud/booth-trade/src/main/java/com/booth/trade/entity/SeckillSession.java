package com.booth.trade.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@TableName("seckill_session")
public class SeckillSession extends BaseEntity {

    private String name;
    /** 每日开始时间 */
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean status;
}
