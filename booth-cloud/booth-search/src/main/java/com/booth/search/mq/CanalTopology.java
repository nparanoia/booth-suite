package com.booth.search.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CanalTopology {

    public static final String EXCHANGE = "booth.canal";
    public static final String QUEUE = "booth.search.canal";
    public static final String ROUTING_KEY = "booth.product";

    @Bean
    public TopicExchange canalExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue canalQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding canalBinding(Queue canalQueue, TopicExchange canalExchange) {
        return BindingBuilder.bind(canalQueue).to(canalExchange).with(ROUTING_KEY);
    }
}
