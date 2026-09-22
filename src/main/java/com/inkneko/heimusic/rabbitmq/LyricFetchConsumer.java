package com.inkneko.heimusic.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inkneko.heimusic.config.RabbitMQConfig;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.model.entity.LyricFetchLog;
import com.inkneko.heimusic.model.vo.LyricFetchVo;
import com.inkneko.heimusic.rabbitmq.model.LyricFetchRequest;
import com.inkneko.heimusic.service.LyricFetchLogService;
import com.inkneko.heimusic.service.LyricFetchService;
import com.inkneko.heimusic.util.lrclib.LrclibRateLimitException;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * LRCLIB 歌词批量补全消费者
 * <p>
 * 单消费者串行处理（同时只有一个拉取在执行），每条处理完休眠 2 秒，
 * 以远低于官方节流建议（200~500ms）的速率访问 LRCLIB。
 * 全部结局均 ack 不留死信：拉取仅服务无歌词音乐，重复消息会因业务校验自然跳过（幂等），
 * 失败靠批量投放脚本重投。多实例部署时 heimusic.is-lyric-fetch-node 只开一个节点，
 * 避免竞争消费者并行放大 LRCLIB 请求速率
 */
@Component
@ConditionalOnProperty(
        value = "${heimusic.is-lyric-fetch-node}",
        havingValue = "true",
        matchIfMissing = true
)
public class LyricFetchConsumer {
    Logger logger = LoggerFactory.getLogger(LyricFetchConsumer.class);

    /**
     * 每条消息处理完的休眠间隔（毫秒），控制对 LRCLIB 的请求速率
     */
    private static final long THROTTLE_MILLIS = 2000L;

    /**
     * 限流退避等待上限（秒），避免异常长的 Retry-After 长时间阻塞消费线程
     */
    private static final long RATE_LIMIT_MAX_WAIT_SECONDS = 60L;

    @Autowired
    LyricFetchService lyricFetchService;
    @Autowired
    LyricFetchLogService lyricFetchLogService;

    @RabbitListener(queues = RabbitMQConfig.LyricFetch.queueName, ackMode = "MANUAL")
    public void fetchLyric(Channel channel, Message message) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        Integer musicId = null;
        try {
            LyricFetchRequest request = new ObjectMapper().readValue(message.getBody(), LyricFetchRequest.class);
            musicId = request.getMusicId();
            try {
                LyricFetchVo result = fetchWithRateLimitRetry(musicId);
                logger.info("音乐{}的LRCLIB拉取完成，outcome：{}", musicId, result.getOutcome());
            } catch (ServiceException e) {
                //业务性跳过：音乐不存在/已有歌词（人工数据无条件优先）等，日志已在服务层记录
                logger.info("音乐{}的LRCLIB拉取跳过：{}", musicId, e.getMessage());
            } catch (LrclibRateLimitException e) {
                //二次限流/过载（429/503）：已按 Retry-After 退避重试过一次，丢弃待下轮批量投放
                logger.warn("音乐{}的LRCLIB拉取因限流/过载两次失败，丢弃待重投", musicId);
            } catch (Exception e) {
                //网络异常等：与 probe 消费者语义一致，ack 丢弃，靠批量投放脚本重投；日志已在服务层记录
                logger.error("音乐{}的LRCLIB拉取失败：", musicId, e);
            }
        } catch (IOException e) {
            logger.error("歌词拉取消息解析失败：", e);
            lyricFetchLogService.record(LyricFetchLog.SOURCE_MQ, null, LyricFetchLog.OUTCOME_FAILED, "消息解析失败");
        } finally {
            try {
                channel.basicAck(deliveryTag, false);
            } catch (IOException e) {
                logger.error("歌词拉取消息ack失败：", e);
            }
            try {
                Thread.sleep(THROTTLE_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 繁忙重试包装：限流/过载（429/503）时按 Retry-After 退避（上限 60 秒）后重试一次，
     * 二次繁忙仍抛出 LrclibRateLimitException 由调用方丢弃处理
     */
    private LyricFetchVo fetchWithRateLimitRetry(Integer musicId) throws InterruptedException {
        try {
            return lyricFetchService.fetchFromLrclib(musicId, null, LyricFetchLog.SOURCE_MQ);
        } catch (LrclibRateLimitException e) {
            long waitSeconds = Math.min(e.getRetryAfterSeconds(), RATE_LIMIT_MAX_WAIT_SECONDS);
            logger.warn("LRCLIB繁忙（限流/过载），{}秒后重试一次", waitSeconds);
            Thread.sleep(waitSeconds * 1000L);
            return lyricFetchService.fetchFromLrclib(musicId, null, LyricFetchLog.SOURCE_MQ);
        }
    }
}
