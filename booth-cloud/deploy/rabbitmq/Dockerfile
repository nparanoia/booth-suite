FROM rabbitmq:3.13-management

# 延迟消息插件版本需与 RabbitMQ 主版本一致
ARG DELAYED_PLUGIN_VERSION=3.13.0
ADD https://github.com/rabbitmq/rabbitmq-delayed-message-exchange/releases/download/v${DELAYED_PLUGIN_VERSION}/rabbitmq_delayed_message_exchange-${DELAYED_PLUGIN_VERSION}.ez /opt/rabbitmq/plugins/
RUN chmod 644 /opt/rabbitmq/plugins/rabbitmq_delayed_message_exchange-${DELAYED_PLUGIN_VERSION}.ez \
    && rabbitmq-plugins enable --offline rabbitmq_delayed_message_exchange
