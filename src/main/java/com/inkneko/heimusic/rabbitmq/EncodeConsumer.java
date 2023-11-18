package com.inkneko.heimusic.rabbitmq;

import com.inkneko.heimusic.config.RabbitMQConfig;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
@ConditionalOnProperty(
        value = "${heimusic.is-encode-node}",
        havingValue = "true",
        matchIfMissing = true
)
public class EncodeConsumer {

    Logger logger = LoggerFactory.getLogger(EncodeConsumer.class);

    @RabbitListener(queues = RabbitMQConfig.Encode.queueName, ackMode = "MANUAL")
    public void encode(Channel channel, Message message) {
        //String routingKey =  message.getMessageProperties().getReceivedRoutingKey();
        try {
            String command = String.format("ffmpeg -hide_banner -y -i %s -vn -b:a 320k -f mp3 out.mp3", new String(message.getBody(), StandardCharsets.UTF_8));
            logger.info("接收到转码请求，参数：{}", command);
            Process process = Runtime.getRuntime().exec(command);
            try{
                if (process.waitFor() == 0){
                    logger.info("转码完成");
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                    return;
                }
            }catch (InterruptedException e){
                logger.info("收到终止信号，转码终止");
                process.destroyForcibly();
                return;
            }catch (IOException ignored) {
                //若无法ack则认为当前未执行转码，消息留存在队列中，不进行ack重试
            }
            //命令执行的错误处理，从stderr读取消息
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder stringBuilder = new StringBuilder();
            String tmp;
            while((tmp = bufferedReader.readLine()) != null){
                stringBuilder.append(tmp);
            }
            logger.error("转码失败，输出：{}", stringBuilder.toString());

        }catch (IOException e){
            logger.error("转码执行错误，异常：", e);
        }
    }

}