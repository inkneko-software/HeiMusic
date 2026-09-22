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
    public static final String topicExchangeName = "heimusic-topic-exchange";

    public static class Encode {
        public static final String queueName = "encode-queue";
        //routingKey，参数为音乐ID
        public static final String routingKey = "encode.musicId.%s";
    }

    public static class Probe {
        public static final String queueName = "probe-queue";
        //routingKey，参数为音乐ID
        public static final String routingKey = "probe.musicId.%s";
    }

    public static class Split {
        public static final String queueName = "split-queue";
        //routingKey，参数为音乐ID
        public static final String routingKey = "split.musicId.%s";
    }

    public static class LyricFetch {
        public static final String queueName = "lyric-queue";
        //routingKey，参数为音乐ID
        public static final String routingKey = "lyric.musicId.%s";
    }

    @Value("${heimusic.is-encode-node}")
    public boolean isEncodeNode;

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        //信任 MQ 消息模型所在包：消费端 DefaultJackson2JavaTypeMapper 默认仅信任 java.util/java.lang，
        //消息头 __TypeId__ 携带的应用类（如 LyricFetchRequest/ProbeRequest）会被拒绝反序列化，
        //导致 ListenerExecutionFailedException: Failed to convert message（消息被 ConditionalRejectingErrorHandler 丢弃）。
        //注意 3.2.x 的信任匹配是包名全等（无前缀/通配），必须精确到子包
        return new Jackson2JsonMessageConverter("com.inkneko.heimusic.rabbitmq.model");
    }

    @Bean
    TopicExchange rabbitMQExchange() {
        return new TopicExchange(topicExchangeName);
    }

    @Bean
    Queue encodeQueue() {
        return new Queue(Encode.queueName, true);
    }


    @Bean
    Binding encodeBinding(@Qualifier("encodeQueue") Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(String.format(Encode.routingKey, "#"));
    }

    @Bean
    Queue probeQueue() {
        return new Queue(Probe.queueName, true);
    }

    @Bean
    Binding probeBinding(@Qualifier("probeQueue") Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(String.format(Probe.routingKey, "#"));
    }

    @Bean
    Queue splitQueue(){
        return new Queue(Split.queueName, true);
    }

    @Bean
    Binding splitBinding(@Qualifier("splitQueue") Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(String.format(Split.routingKey, "#"));
    }

    @Bean
    Queue lyricQueue() {
        return new Queue(LyricFetch.queueName, true);
    }

    @Bean
    Binding lyricBinding(@Qualifier("lyricQueue") Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(String.format(LyricFetch.routingKey, "#"));
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
