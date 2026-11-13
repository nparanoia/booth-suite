package com.booth.trade.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseEntity;
import com.booth.trade.enums.LocalMessageStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("local_message")
public class LocalMessage extends BaseEntity {

    private String messageId;
    private String bizType;
    private String bizKey;
    private String exchange;
    private String routingKey;
    /** 消息体 JSON，原样投递 */
    private String payload;
    private Long delayMs;
    private LocalMessageStatus status;
    private Integer retryCount;
    private LocalDateTime nextRetryTime;
    private String lastError;
}
