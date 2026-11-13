package com.booth.trade.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.booth.trade.entity.LocalMessage;
import com.booth.trade.enums.LocalMessageStatus;
import com.booth.trade.mapper.LocalMessageMapper;
import com.booth.trade.support.TradeMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 和业务数据放在同一个事务里写消息。事务提交后再投递；没收到 Confirm 的由定时任务重投。
 */
@Slf4j
@Service
public class LocalMessageService {

    public static final int MAX_RETRY = 8;

    private final LocalMessageMapper localMessageMapper;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final TradeMetrics tradeMetrics;

    public LocalMessageService(LocalMessageMapper localMessageMapper, ObjectMapper objectMapper,
                               @Lazy RabbitTemplate rabbitTemplate, TradeMetrics tradeMetrics) {
        this.localMessageMapper = localMessageMapper;
        this.objectMapper = objectMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.tradeMetrics = tradeMetrics;
    }

    @Transactional(rollbackFor = Exception.class)
    public String save(String bizType, String bizKey, String exchange, String routingKey, Object payload, long delayMs) {
        LocalMessage message = new LocalMessage();
        message.setMessageId(IdWorker.getIdStr());
        message.setBizType(bizType);
        message.setBizKey(bizKey);
        message.setExchange(exchange);
        message.setRoutingKey(routingKey);
        message.setPayload(toJson(payload));
        message.setDelayMs(delayMs);
        message.setStatus(LocalMessageStatus.PENDING);
        message.setRetryCount(0);
        message.setNextRetryTime(LocalDateTime.now());
        localMessageMapper.insert(message);
        Long id = message.getId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(id);
                }
            });
        } else {
            send(id);
        }
        return message.getMessageId();
    }

    public LocalMessage findByMessageId(String messageId) {
        return localMessageMapper.selectOne(Wrappers.<LocalMessage>lambdaQuery()
                .eq(LocalMessage::getMessageId, messageId));
    }

    public void markConfirmed(String messageId) {
        if (messageId == null) {
            return;
        }
        localMessageMapper.update(null, Wrappers.<LocalMessage>lambdaUpdate()
                .set(LocalMessage::getStatus, LocalMessageStatus.CONFIRMED)
                .eq(LocalMessage::getMessageId, messageId)
                .eq(LocalMessage::getStatus, LocalMessageStatus.PENDING));
    }

    /** 确认回调只能从待确认改成已确认，不能把无法路由改回去。return 则相反，已确认也要改成无法路由。 */
    public void markUnroutable(String messageId, String reason) {
        if (messageId == null) {
            return;
        }
        String error = reason == null || reason.isBlank() ? "无法路由" : reason;
        if (error.length() > 500) {
            error = error.substring(0, 500);
        }
        int updated = localMessageMapper.update(null, Wrappers.<LocalMessage>lambdaUpdate()
                .set(LocalMessage::getStatus, LocalMessageStatus.ROUTE_FAILED)
                .set(LocalMessage::getLastError, error)
                .eq(LocalMessage::getMessageId, messageId)
                .in(LocalMessage::getStatus, LocalMessageStatus.PENDING, LocalMessageStatus.CONFIRMED));
        if (updated == 1) {
            log.error("消息无法路由，messageId={}，原因={}", messageId, error);
        }
    }

    public void retryDue() {
        List<LocalMessage> due = localMessageMapper.selectList(Wrappers.<LocalMessage>lambdaQuery()
                .eq(LocalMessage::getStatus, LocalMessageStatus.PENDING)
                .le(LocalMessage::getNextRetryTime, LocalDateTime.now())
                .orderByAsc(LocalMessage::getId)
                .last("LIMIT 50"));
        for (LocalMessage message : due) {
            if (message.getRetryCount() >= MAX_RETRY) {
                int failed = localMessageMapper.update(null, Wrappers.<LocalMessage>lambdaUpdate()
                        .set(LocalMessage::getStatus, LocalMessageStatus.FAILED)
                        .set(LocalMessage::getLastError, "超过重试上限")
                        .eq(LocalMessage::getId, message.getId())
                        .eq(LocalMessage::getStatus, LocalMessageStatus.PENDING));
                if (failed == 1) {
                    tradeMetrics.localMessageExhausted();
                    log.error("本地消息超过重试上限，messageId={}，bizKey={}", message.getMessageId(), message.getBizKey());
                }
                continue;
            }
            LocalDateTime next = LocalDateTime.now().plusSeconds(backoffSeconds(message.getRetryCount()));
            int claimed = localMessageMapper.update(null, Wrappers.<LocalMessage>lambdaUpdate()
                    .set(LocalMessage::getRetryCount, message.getRetryCount() + 1)
                    .set(LocalMessage::getNextRetryTime, next)
                    .eq(LocalMessage::getId, message.getId())
                    .eq(LocalMessage::getStatus, LocalMessageStatus.PENDING)
                    .eq(LocalMessage::getNextRetryTime, message.getNextRetryTime()));
            if (claimed == 1) {
                tradeMetrics.localMessageRetried();
                send(message.getId());
            }
        }
    }

    public void send(Long id) {
        LocalMessage message = localMessageMapper.selectById(id);
        if (message == null || message.getStatus() != LocalMessageStatus.PENDING) {
            return;
        }
        try {
            boolean immediate = message.getDelayMs() == null || message.getDelayMs() == 0;
            Message amqp = MessageBuilder.withBody(message.getPayload().getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setCorrelationId(message.getMessageId())
                    .setHeader("x-delay", message.getDelayMs())
                    .setHeader("x-booth-mandatory", immediate)
                    .build();
            rabbitTemplate.send(message.getExchange(), message.getRoutingKey(), amqp, new CorrelationData(message.getMessageId()));
        } catch (RuntimeException e) {
            log.warn("投递本地消息失败，messageId={}", message.getMessageId(), e);
        }
    }

    private static long backoffSeconds(int retryCount) {
        return Math.min(300, 1L << Math.min(retryCount, 8));
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
