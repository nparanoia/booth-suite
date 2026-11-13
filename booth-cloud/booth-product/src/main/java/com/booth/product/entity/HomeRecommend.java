package com.booth.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("home_recommend")
public class HomeRecommend extends BaseEntity {

    private Long productId;
    /** 1 新品推荐，2 人气推荐 */
    private Integer type;
    private Integer sort;
    private Boolean status;
}
