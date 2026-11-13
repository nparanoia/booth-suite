package com.booth.search.mq;

import com.rabbitmq.client.Channel;
import com.booth.search.service.CanalSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class CanalProductListener {

    private final CanalSyncService canalSyncService;

    @RabbitListener(queues = CanalTopology.QUEUE)
    public void onMessage(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            canalSyncService.accept(body);
            channel.basicAck(tag, false);
        } catch (IllegalArgumentException e) {
            log.error("丢弃无法解析的商品变更消息", e);
            channel.basicAck(tag, false);
        } catch (RuntimeException e) {
            log.error("处理商品变更失败", e);
            channel.basicNack(tag, false, false);
        }
    }
}
