package com.inkneko.heimusic.service.impl;

import com.inkneko.heimusic.config.RabbitMQConfig;
import com.inkneko.heimusic.service.EncodeService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class EncodeServiceImpl implements EncodeService {
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public EncodeServiceImpl(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void requestEncode(String file) {
        System.out.println("Sending message...");
        rabbitTemplate.convertAndSend(RabbitMQConfig.Encode.topicExchangeName, "encode.file_id.233", file);
    }

    @Override
    public void requestProbe(String filePath) {
        System.out.println("Sending message...");
        rabbitTemplate.convertAndSend(RabbitMQConfig.Probe.topicExchangeName, String.format(RabbitMQConfig.Probe.routingKey, 12450), filePath);
    }
}
