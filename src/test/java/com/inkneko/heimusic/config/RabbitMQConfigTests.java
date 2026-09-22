package com.inkneko.heimusic.config;

import com.inkneko.heimusic.rabbitmq.model.LyricFetchRequest;
import com.inkneko.heimusic.rabbitmq.model.ProbeRequest;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * RabbitMQConfig 单元测试：Jackson 转换器信任 MQ 消息模型包。
 * 修复前消费端反序列化应用类抛 IllegalArgumentException（not in the trusted packages），
 * 消息被 ConditionalRejectingErrorHandler 当作致命错误直接丢弃，此处对生产/消费往返做回归。
 * 3.2.x 的信任匹配是包名全等：trustedPackages 含 com.inkneko.heimusic 而类在
 * com.inkneko.heimusic.rabbitmq.model 时仍会被拒，回归测试防止此错误再次出现。
 */
class RabbitMQConfigTests {

    @Test
    void converterTrustsApplicationPackages() {
        Jackson2JsonMessageConverter converter = new RabbitMQConfig().jsonMessageConverter();

        LyricFetchRequest lyricRequest = new LyricFetchRequest();
        lyricRequest.setMusicId(42);
        Message lyricMessage = converter.toMessage(lyricRequest, new MessageProperties());
        Object convertedLyric = converter.fromMessage(lyricMessage);
        assertInstanceOf(LyricFetchRequest.class, convertedLyric);
        assertEquals(42, ((LyricFetchRequest) convertedLyric).getMusicId());

        //既有 probe 队列同一共享转换器，一并回归
        ProbeRequest probeRequest = new ProbeRequest();
        probeRequest.setMusicId(7);
        Message probeMessage = converter.toMessage(probeRequest, new MessageProperties());
        assertInstanceOf(ProbeRequest.class, converter.fromMessage(probeMessage));
    }
}
