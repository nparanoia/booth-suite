package com.booth.trade.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.booth.trade.entity.LocalMessage;
import com.booth.trade.entity.SeckillOrder;
import com.booth.trade.enums.LocalMessageStatus;
import com.booth.trade.mapper.LocalMessageMapper;
import com.booth.trade.mapper.SeckillOrderMapper;
import com.booth.trade.support.SeckillKeys;
import com.booth.trade.support.SeckillMq;
import com.booth.trade.support.SeckillOrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 只处理被消息队列退回的记录。重试耗尽只说明没等到确认，消息可能已经在处理，这时归还会把库存加多。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillRepairService {

    private final LocalMessageMapper localMessageMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillReservationService reservationService;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public void repairUndelivered() {
        List<LocalMessage> messages = localMessageMapper.selectList(Wrappers.<LocalMessage>lambdaQuery()
                .eq(LocalMessage::getBizType, SeckillMq.BIZ_ORDER)
                .eq(LocalMessage::getStatus, LocalMessageStatus.ROUTE_FAILED)
                .ge(LocalMessage::getCreateTime, LocalDateTime.now().minusDays(1))
                .orderByAsc(LocalMessage::getId)
                .last("LIMIT 50"));
        for (LocalMessage message : messages) {
            try {
                SeckillOrderMessage body = objectMapper.readValue(message.getPayload(), SeckillOrderMessage.class);
                long ttl = reservationService.ttlSeconds(body.seckillSkuId());
                redis.opsForValue().setIfAbsent(SeckillKeys.abort(body.requestId()), "1", Duration.ofSeconds(ttl));
                SeckillOrder existing = seckillOrderMapper.selectOne(Wrappers.<SeckillOrder>lambdaQuery()
                        .eq(SeckillOrder::getOrderSn, body.orderSn()));
                if (existing != null) {
                    reservationService.succeed(body.requestId(), body.orderSn());
                    continue;
                }
                if (reservationService.placing(body.requestId())) {
                    continue;
                }
                reservationService.revertRequest(body.requestId(), body.memberId(), body.seckillSkuId(), body.quantity());
                reservationService.fail(body.requestId(), "抢购消息没有送达");
            } catch (Exception e) {
                log.warn("补救秒杀消息失败，messageId={}", message.getMessageId(), e);
            }
        }
    }
}
