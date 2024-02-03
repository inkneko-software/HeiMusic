package com.inkneko.heimusic.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inkneko.heimusic.config.MinIOConfig;
import com.inkneko.heimusic.config.RabbitMQConfig;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.model.entity.Music;
import com.inkneko.heimusic.rabbitmq.model.ProbeRequest;
import com.inkneko.heimusic.rabbitmq.model.SplitRequest;
import com.inkneko.heimusic.service.MinIOService;
import com.inkneko.heimusic.service.MusicService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 将整个CD的音轨（一般为一个CUE文件搭配一个或多个音乐文件）转换为多个单独的音乐文件
 */
@Slf4j
@Component
public class SplitConsumer {

    MinIOConfig minIOConfig;
    MinIOService minIOService;
    MusicService musicService;
    AmqpTemplate amqpTemplate;

    public SplitConsumer(MinIOConfig minIOConfig, MinIOService minIOService, MusicService musicService, AmqpTemplate amqpTemplate) {
        this.minIOConfig = minIOConfig;
        this.minIOService = minIOService;
        this.musicService = musicService;
        this.amqpTemplate = amqpTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.Split.queueName, ackMode = "MANUAL")
    public void split(Channel channel, Message message) {
        ObjectMapper objectMapper = new ObjectMapper();
        File musicFile = null;
        File splitedTargetFile = null;
        try {
            SplitRequest splitRequest = objectMapper.readValue(message.getBody(), SplitRequest.class);
            musicFile = minIOService.download(splitRequest.getMusicFileBucket(), splitRequest.getMusicFileObjectKey());
            for (SplitRequest.MusicInfo musicInfo : splitRequest.getMusicList()) {
                ProcessBuilder processBuilder;
                splitedTargetFile = File.createTempFile("split_consume_" + UUID.randomUUID(), ".flac");
                if (musicInfo.endTime != null) {
                    processBuilder = new ProcessBuilder(
                            "ffmpeg",
                            "-y",
                            "-v", "error",
                            "-i", musicFile.getAbsolutePath(),
                            "-ss", musicInfo.startTime,
                            "-to", musicInfo.endTime,
                            splitedTargetFile.getAbsolutePath()
                    );
                } else {
                    processBuilder = new ProcessBuilder(
                            "ffmpeg",
                            "-y",
                            "-v", "error",
                            "-i", musicFile.getAbsolutePath(),
                            "-ss", musicInfo.startTime,
                            splitedTargetFile.getAbsolutePath()
                    );
                }
                log.info("准备进行切片操作，命令：{}", String.join(" ", processBuilder.command()));

                Process process = processBuilder.start();
                //可能的一个问题，当错误流充满时，ffmpeg会卡主，process.waitFor()不返回
//                try(InputStreamReader isr = new InputStreamReader(process.getErrorStream())) {
//                    int c;
//                    while((c = isr.read()) >= 0) {
//                        System.out.print((char) c);
//                        System.out.flush();
//                    }
//                }

                try {
                    int ret = process.waitFor();
                    if (ret != 0) {
                        log.error("执行ffmpeg时出现错误，错误码：{}, 输出：{}", ret, new String(IOUtils.toByteArray(process.getErrorStream()), StandardCharsets.UTF_8));
                    }
                    String bucket = minIOConfig.getBucket();
                    String objectKey = String.format("transcode/cue_split/%d.flac", musicInfo.musicId);
                    minIOService.upload(bucket, objectKey, splitedTargetFile, "audio/flac");
                    Music music = musicService.getById(musicInfo.musicId);
                    music.setBucket(bucket);
                    music.setObjectKey(objectKey);
                    musicService.updateById(music);
                    //获取时长，码率信息
                    ProbeRequest probeRequest = new ProbeRequest();
                    probeRequest.setMusicId(musicInfo.musicId);
                    probeRequest.setBucket(bucket);
                    probeRequest.setObjectKey(objectKey);
                    amqpTemplate.convertAndSend(RabbitMQConfig.topicExchangeName, RabbitMQConfig.Probe.routingKey, probeRequest);
                } catch (InterruptedException e) {
                    log.error("Interrupted", e);
                }
            }


        } catch (IOException e) {
            log.error("转换为SplitRequest时发生错误，数据 {}", new String(message.getBody(), StandardCharsets.UTF_8), e);
        } catch (ServiceException e) {
            log.error("下载文件时出现异常：", e);
        } finally {
            if (musicFile != null) {
                boolean ignored = musicFile.delete();
            }
            if (splitedTargetFile != null) {
                boolean ignored = splitedTargetFile.delete();
            }
        }

        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            log.error("ack队列消息时发生错误");
        }
    }
}
