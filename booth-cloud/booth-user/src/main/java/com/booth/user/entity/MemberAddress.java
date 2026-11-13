package com.booth.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("member_address")
public class MemberAddress extends BaseEntity {

    private Long memberId;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String region;
    private String detailAddress;
    private String postCode;
    private Boolean isDefault;
}
