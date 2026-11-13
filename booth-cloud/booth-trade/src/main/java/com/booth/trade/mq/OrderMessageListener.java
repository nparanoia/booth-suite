package com.booth.trade.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.booth.common.core.exception.BizException;
import com.booth.trade.service.OrderEventHandler;
import com.booth.trade.support.OrderEvent;
import com.booth.trade.support.OrderMq;
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
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageListener {

    private final ObjectMapper objectMapper;
    private final OrderEventHandler orderEventHandler;
    private final RetryTemplate retryTemplate = RetryTemplate.builder().maxAttempts(3).fixedBackoff(200).build();

    @RabbitListener(queues = OrderMq.TIMEOUT_QUEUE)
    public void timeout(Message message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        consume(message, channel, tag, event -> orderEventHandler.onTimeout(event.orderSn()));
    }

    @RabbitListener(queues = OrderMq.PAID_QUEUE)
    public void paid(Message message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        consume(message, channel, tag, event -> orderEventHandler.onPaid(event.orderSn()));
    }

    @RabbitListener(queues = OrderMq.CLOSED_QUEUE)
    public void closed(Message message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        consume(message, channel, tag, event -> orderEventHandler.onClosed(event.orderSn()));
    }

    @RabbitListener(queues = OrderMq.COMPLETED_QUEUE)
    public void completed(Message message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        consume(message, channel, tag, event -> orderEventHandler.onCompleted(event.orderSn()));
    }

    private void consume(Message message, Channel channel, long tag, Consumer<OrderEvent> handler) throws IOException {
        try {
            OrderEvent event = objectMapper.readValue(new String(message.getBody(), StandardCharsets.UTF_8), OrderEvent.class);
            retryTemplate.execute(context -> {
                handler.accept(event);
                return null;
            });
            channel.basicAck(tag, false);
        } catch (BizException e) {
            log.warn("订单消息业务失败，进入死信：{}", e.getMessage());
            channel.basicNack(tag, false, false);
        } catch (Exception e) {
            log.error("订单消息处理失败，进入死信", e);
            channel.basicNack(tag, false, false);
        }
    }
}
