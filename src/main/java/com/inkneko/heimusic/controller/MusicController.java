package com.inkneko.heimusic.controller;

import com.inkneko.heimusic.config.MinIOConfig;
import com.inkneko.heimusic.config.RabbitMQConfig;
import com.inkneko.heimusic.model.entity.Music;
import com.inkneko.heimusic.model.vo.Response;
import com.inkneko.heimusic.rabbitmq.model.ProbeRequest;
import com.inkneko.heimusic.service.MinIOService;
import com.inkneko.heimusic.service.MusicService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/music")
public class MusicController {
    MusicService musicService;
    MinIOService minIOService;
    MinIOConfig minIOConfig;
    AmqpTemplate amqpTemplate;


    public MusicController(MusicService musicService, MinIOService minIOService, MinIOConfig minIOConfig, AmqpTemplate amqpTemplate) {
        this.musicService = musicService;
        this.minIOService = minIOService;
        this.minIOConfig = minIOConfig;
        this.amqpTemplate = amqpTemplate;
    }

    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "新建音乐")
    public Response<Music> addMusic(@RequestParam String title,
                                    @RequestParam(required = false) String translateTitle,
                                    @RequestParam(required = false) List<String> artistList,
                                    @RequestPart MultipartFile file) {
        Music music = new Music();
        music.setTitle(title);
        music.setTranslateTitle(translateTitle);
        musicService.save(music);

        String bucket = "heimusic";
        String objectKey = String.format("music/%d_source", music.getMusicId());
        minIOService.upload(bucket, objectKey, file);
        music.setBucket(bucket);
        music.setObjectKey(objectKey);
        musicService.updateById(music);
        if (artistList != null) {
            musicService.addMusicArtistsWithName(music.getMusicId(), artistList);
        }
        ProbeRequest probeRequest = new ProbeRequest();
        probeRequest.setMusicId(music.getMusicId());
        probeRequest.setBucket(bucket);
        probeRequest.setObjectKey(objectKey);
        amqpTemplate.convertAndSend(RabbitMQConfig.Probe.topicExchangeName, RabbitMQConfig.Probe.routingKey, probeRequest);
        return new Response<>(0, "ok", music);
    }
}
