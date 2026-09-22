package com.inkneko.heimusic.rabbitmq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inkneko.heimusic.config.RabbitMQConfig;
import com.inkneko.heimusic.model.entity.Lyric;
import com.inkneko.heimusic.model.entity.Music;
import com.inkneko.heimusic.rabbitmq.model.LyricFetchRequest;
import com.inkneko.heimusic.service.LyricService;
import com.inkneko.heimusic.service.MusicService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 手动运维脚本：向 lyric-queue 批量投放"无歌词且非纯音乐"的音乐，需要真实数据与拉取节点，不适用于自动化测试。
 * 消费侧幂等（仅服务无歌词音乐），重复投放无害。
 */
@SpringBootTest
@Disabled("手动运维脚本：向 lyric-queue 批量投放无歌词音乐，需要真实数据与拉取节点，不适用于自动化测试")
public class LyricFetchProducerTests {
    @Autowired
    AmqpTemplate template;

    @Autowired
    MusicService musicService;

    @Autowired
    LyricService lyricService;

    @Test
    void injectNoLyricMusic() {
        Set<Integer> hasLyricMusicIds = lyricService.list().stream()
                .map(Lyric::getMusicId)
                .collect(Collectors.toSet());
        //is_instrumental 为 NULL（未知）或 false 的音乐均参与拉取
        List<Music> targets = musicService.list(new LambdaQueryWrapper<Music>()
                        .and(w -> w.ne(Music::getIsInstrumental, true).or().isNull(Music::getIsInstrumental)))
                .stream()
                .filter(m -> !hasLyricMusicIds.contains(m.getMusicId()))
                .toList();
        for (Music music : targets) {
            LyricFetchRequest request = new LyricFetchRequest();
            request.setMusicId(music.getMusicId());
            template.convertAndSend(RabbitMQConfig.topicExchangeName, RabbitMQConfig.LyricFetch.routingKey, request);
        }
    }
}
