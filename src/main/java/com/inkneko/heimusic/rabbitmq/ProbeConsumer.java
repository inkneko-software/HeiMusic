package com.inkneko.heimusic.rabbitmq;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inkneko.heimusic.config.RabbitMQConfig;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.model.entity.Music;
import com.inkneko.heimusic.rabbitmq.model.ProbeRequest;
import com.inkneko.heimusic.service.MinIOService;
import com.inkneko.heimusic.service.MusicService;
import com.rabbitmq.client.Channel;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(
        value = "${heimusic.is-encode-node}",
        havingValue = "true",
        matchIfMissing = true
)
public class ProbeConsumer {
    Logger logger = LoggerFactory.getLogger(ProbeConsumer.class);

    @Autowired
    MinIOService minIOService;
    @Autowired
    MusicService musicService;

    @Data
    public static class ProbeFormat {
        @Data
        public static class Format {
            String filename;
            @JsonProperty("format_name")
            String formatName;
            String duration;
            String size;
            @JsonProperty("bit_rate")
            String bitrate;
            @JsonProperty("probe_score")
            String probeScore;

            @JsonAnySetter
            public void ignore(String name, Object value) {
            }
        }

        Format format;
    }

    @RabbitListener(queues = RabbitMQConfig.Probe.queueName, ackMode = "MANUAL")
    public void probe(Channel channel, Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        File musicFile = null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ProbeRequest probeRequest = objectMapper.readValue(message.getBody(), ProbeRequest.class);
            try {
                musicFile =  minIOService.download(probeRequest.getBucket(), probeRequest.getObjectKey());
            } catch (Exception e){
                logger.error("分析执行错误，出现业务异常：", e);
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }
            String command = String.format("ffprobe -v quiet -of json -show_format \"%s\"", musicFile.getAbsolutePath());
            logger.info("接收到probe请求，参数：{}", command);
//            Process process = Runtime.getRuntime().exec(command);
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffprobe",
                    "-v", "quiet",
                    "-of", "json",
                    "-show_format",
                    musicFile.getAbsolutePath()
            );
            Process process = processBuilder.start();
            try {
                int retCode = process.waitFor();
                if (retCode == 0) {
                    logger.info("分析完成");
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String tmp;
                    while ((tmp = bufferedReader.readLine()) != null) {
                        stringBuilder.append(tmp);
                    }
                    ProbeFormat probeFormat = objectMapper.readValue(stringBuilder.toString(), ProbeFormat.class);
                    logger.info(String.format("格式：%s", probeFormat.toString()));
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                    Music music = musicService.getById(probeRequest.getMusicId());
                    music.setDuration(Float.valueOf(probeFormat.getFormat().duration).intValue());

                    music.setCodec(probeFormat.getFormat().formatName);
                    music.setBitrate(probeFormat.getFormat().getBitrate());
                    musicService.updateById(music);
                    return;
                }

                logger.error("执行ffprobe失败，返回码：{}", retCode);
                //命令执行的错误处理，从stderr读取消息
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String tmp;
                while ((tmp = bufferedReader.readLine()) != null) {
                    stringBuilder.append(tmp);
                }
                logger.error(String.format("分析失败，stderr输出：%s", stringBuilder));

            } catch (InterruptedException e) {
                logger.info("收到终止信号，分析终止");
                process.destroyForcibly();
                return;
            } catch (IOException e) {
                logger.error("错误，异常：", e);
                //若无法ack则认为当前未执行转码，消息留存在队列中，不进行ack重试
            }

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            logger.error("分析执行错误，异常：", e);
        }finally {
            if( musicFile != null){
                musicFile.deleteOnExit();
            }
        }
    }
}
