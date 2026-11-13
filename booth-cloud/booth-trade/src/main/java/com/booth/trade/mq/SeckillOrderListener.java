package com.booth.trade.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.booth.trade.service.SeckillPlaceService;
import com.booth.trade.support.SeckillMq;
import com.booth.trade.support.SeckillOrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderListener {

    private final ObjectMapper objectMapper;
    private final SeckillPlaceService seckillPlaceService;
    private final RetryTemplate retryTemplate = RetryTemplate.builder().maxAttempts(3).fixedBackoff(200).build();

    @RabbitListener(queues = SeckillMq.QUEUE)
    public void onOrder(Message message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            SeckillOrderMessage body = objectMapper.readValue(
                    new String(message.getBody(), StandardCharsets.UTF_8), SeckillOrderMessage.class);
            retryTemplate.execute(context -> {
                seckillPlaceService.handle(body);
                return null;
            });
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("秒杀下单消息失败，进入死信", e);
            channel.basicNack(tag, false, false);
        }
    }
}
