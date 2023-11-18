package com.inkneko.heimusic.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 交换机，队列、消息处理函数的设定
 */
@Configuration
public class RabbitMQConfig {
    public static class Encode {
        public static final String topicExchangeName = "heimusic-encode-exchange";
        public static final String queueName = "encode-queue";
    }

    public static class Probe {
        public static final String topicExchangeName = "heimusic-probe-exchange";
        public static final String queueName = "probe-queue";
        public static final String routingKey = "probe.fileId.%s";
    }

    @Value("${heimusic.is-encode-node}")
    public boolean isEncodeNode;

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    Queue encodeQueue() {
        return new Queue(Encode.queueName, true);
    }

    @Bean
    TopicExchange encodeExchange() {
        return new TopicExchange(Encode.topicExchangeName);
    }

    @Bean
    Binding encodeBinding(@Qualifier("encodeQueue") Queue queue, @Qualifier("encodeExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("encode.file_id.#");
    }

    @Bean
    Queue probeQueue() {
        return new Queue(Probe.queueName, true);
    }

    @Bean
    TopicExchange probeExchange() {
        return new TopicExchange(Probe.topicExchangeName);
    }

        @Bean
    Binding probeBinding(@Qualifier("probeQueue") Queue queue, @Qualifier("probeExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(String.format(Probe.routingKey, "#"));
    }


//    @Bean
//    SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
//                                             MessageListenerAdapter listenerAdapter) {
//        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
//        container.setConnectionFactory(connectionFactory);
//        container.setQueueNames(queueName);
//        container.setMessageListener(listenerAdapter);
//        return container;
//    }
//
//    @Bean
//    MessageListenerAdapter listenerAdapter(EncodeConsumer encodeConsumer) {
//        return new MessageListenerAdapter(encodeConsumer, "encode");
//    }
}
