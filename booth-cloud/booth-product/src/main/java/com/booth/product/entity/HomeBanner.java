package com.booth.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("home_banner")
public class HomeBanner extends BaseEntity {

    private String name;
    private String pic;
    private String url;
    private Integer sort;
    /** 是否上线 */
    private Boolean status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
