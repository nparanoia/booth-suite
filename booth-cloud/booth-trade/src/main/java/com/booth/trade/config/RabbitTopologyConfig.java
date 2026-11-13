package com.booth.trade.config;

import com.booth.trade.service.LocalMessageService;
import com.booth.trade.support.OrderMq;
import com.booth.trade.support.SeckillMq;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
public class RabbitTopologyConfig {

    @Bean
    public Declarables orderDeclarables() {
        CustomExchange delay = new CustomExchange(OrderMq.DELAY_EXCHANGE, "x-delayed-message", true, false,
                Map.of("x-delayed-type", "direct"));
        TopicExchange event = new TopicExchange(OrderMq.EVENT_EXCHANGE, true, false);
        DirectExchange dlx = new DirectExchange(OrderMq.DLX, true, false);
        DirectExchange seckill = new DirectExchange(SeckillMq.EXCHANGE, true, false);
        List<org.springframework.amqp.core.Declarable> declarables = new ArrayList<>();
        declarables.add(delay);
        declarables.add(event);
        declarables.add(dlx);
        declarables.add(seckill);
        bind(declarables, delay, dlx, OrderMq.TIMEOUT_QUEUE, OrderMq.RK_TIMEOUT);
        bind(declarables, seckill, dlx, SeckillMq.QUEUE, SeckillMq.ROUTING_KEY);
        bind(declarables, event, dlx, OrderMq.PAID_QUEUE, OrderMq.RK_PAID);
        bind(declarables, event, dlx, OrderMq.CLOSED_QUEUE, OrderMq.RK_CLOSED);
        bind(declarables, event, dlx, OrderMq.COMPLETED_QUEUE, OrderMq.RK_COMPLETED);
        return new Declarables(declarables);
    }

    private static void bind(List<org.springframework.amqp.core.Declarable> declarables,
                             org.springframework.amqp.core.Exchange exchange, DirectExchange dlx,
                             String queueName, String routingKey) {
        Queue queue = QueueBuilder.durable(queueName)
                .deadLetterExchange(OrderMq.DLX)
                .deadLetterRoutingKey(routingKey)
                .build();
        Queue dead = QueueBuilder.durable(queueName + ".dlq").build();
        declarables.add(queue);
        declarables.add(dead);
        declarables.add(BindingBuilder.bind(queue).to(exchange).with(routingKey).noargs());
        declarables.add(BindingBuilder.bind(dead).to(dlx).with(routingKey));
    }

    @Bean
    public RabbitTemplateCustomizer localMessageConfirms(LocalMessageService localMessageService) {
        return template -> {
            // 延迟消息带上 x-booth-mandatory=false。延迟插件会对每条超时消息回一条 return，不能把它当成路由失败
            template.setMandatoryExpressionString("messageProperties.headers['x-booth-mandatory']");
            template.setConfirmCallback((correlation, ack, cause) -> {
                if (correlation == null || correlation.getId() == null || !ack) {
                    return;
                }
                localMessageService.markConfirmed(correlation.getId());
            });
            template.setReturnsCallback(returned -> {
                String messageId = returned.getMessage().getMessageProperties().getCorrelationId();
                localMessageService.markUnroutable(messageId, returned.getReplyText());
            });
        };
    }
}
