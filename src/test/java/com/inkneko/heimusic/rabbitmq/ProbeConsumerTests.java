package com.inkneko.heimusic.rabbitmq;

import com.inkneko.heimusic.config.RabbitMQConfig;
import com.inkneko.heimusic.model.entity.Music;
import com.inkneko.heimusic.rabbitmq.model.ProbeRequest;
import com.inkneko.heimusic.service.MusicService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ProbeConsumerTests {
    @Autowired
    AmqpTemplate template;

    @Autowired
    MusicService musicService;
    @Test
    void updateAll(){
        for(int i = 0; i < 1000; ++i){
            Music music = musicService.getById(i);
            if (music != null){
                ProbeRequest request = new ProbeRequest();
                request.setMusicId(music.getMusicId());
                request.setBucket(music.getBucket());
                request.setObjectKey(music.getObjectKey());
                template.convertAndSend(RabbitMQConfig.topicExchangeName, RabbitMQConfig.Probe.routingKey, request);
            }
        }
    }
}
