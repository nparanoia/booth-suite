package com.booth.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseCreateEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@TableName("member_collection")
public class MemberCollection extends BaseCreateEntity {

    private Long memberId;
    private Long productId;
    private String productName;
    private String productPic;
    private BigDecimal productPrice;
}
