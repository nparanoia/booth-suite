package com.booth.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseCreateEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("member_integration_log")
public class MemberIntegrationLog extends BaseCreateEntity {

    private Long memberId;
    /** 正数增加，负数扣减 */
    private Integer changeAmount;
    private Integer balanceAfter;
    /** ORDER_DEDUCT / ORDER_REFUND / ORDER_REWARD / ORDER_ABORT / ADMIN_ADJUST */
    private String bizType;
    private String bizNo;
    private String note;
}
