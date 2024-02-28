package com.inkneko.heimusic.controller;

import com.inkneko.heimusic.config.MinIOConfig;
import com.inkneko.heimusic.config.RabbitMQConfig;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.model.dto.MusicDto;
import com.inkneko.heimusic.model.entity.Music;
import com.inkneko.heimusic.model.vo.Response;
import com.inkneko.heimusic.rabbitmq.model.ProbeRequest;
import com.inkneko.heimusic.rabbitmq.model.SplitRequest;
import com.inkneko.heimusic.service.MinIOService;
import com.inkneko.heimusic.service.MusicService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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

    @Operation(summary = "新建音乐")
    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<Music> addMusic(@RequestParam String title,
                                    @RequestParam(required = false) String translateTitle,
                                    @RequestParam(required = false) List<String> artistList,
                                    @RequestPart MultipartFile file) {
        Music music = new Music();
        music.setTitle(title);
        music.setTranslateTitle(translateTitle);
        musicService.save(music);

        String bucket = minIOConfig.getBucket();
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
        amqpTemplate.convertAndSend(RabbitMQConfig.topicExchangeName, RabbitMQConfig.Probe.routingKey, probeRequest);
        return new Response<>(0, "ok", music);
    }

    @Operation(
            summary = "通过cue信息添加音乐",
            description = "cue文件中声明了一个或多个FILE，每个FILE包含了一组TRACK（单曲的音乐信息）。" +
                    "通过上传file与该文件所拥有的单曲信息musicList，后端将自动将整个音轨切为对应的单曲，并添加到指定专辑")
    @PostMapping(value = "/addMusicFromCue", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<List<Music>> addMusicFromCue(@RequestPart List<MusicDto> musicDtoList,
                                                 @RequestPart MultipartFile file,
                                                 BindingResult result) {
        if (result.hasErrors()) {
            System.out.println(result);
            throw new ServiceException(400, "请求参数有误");
        }
        //保存CUE文件
        String objectKey = String.format("upload/cue/%s", UUID.randomUUID());
        minIOService.upload(minIOConfig.getBucket(), objectKey, file);

        List<SplitRequest.MusicInfo> musicInfoList = new ArrayList<>();
        List<Music> musicList = new ArrayList<>();
        //为每个音乐信息创建音乐
        for (MusicDto musicDto : musicDtoList) {
            Music music = new Music();
            music.setTitle(musicDto.getTitle());
            music.setTranslateTitle(musicDto.getTranslatedTitle());
            musicService.save(music);
            musicService.addMusicArtistsWithName(music.getMusicId(), musicDto.getArtists());
            musicList.add(music);
            musicInfoList.add(new SplitRequest.MusicInfo(music.getMusicId(), musicDto.getStartTime(), musicDto.getEndTime()));
        }
        //发布切片任务
        SplitRequest splitRequest = new SplitRequest();
        splitRequest.setMusicFileBucket(minIOConfig.getBucket());
        splitRequest.setMusicFileObjectKey(objectKey);
        splitRequest.setMusicList(musicInfoList);
        amqpTemplate.convertAndSend(RabbitMQConfig.topicExchangeName, String.format(RabbitMQConfig.Split.routingKey, 12450), splitRequest);
        return new Response<>(0, "ok", musicList);
    }

    @Operation(summary = "更新音乐的艺术家信息")
    @PostMapping("/updateMusicArtists")
    public Response<?> updateMusicArtists(@RequestParam Integer musicId, @RequestParam List<Integer> artistIds) {
        musicService.updateMusicArtists(musicId, artistIds);
        return new Response<>(0, "ok");
    }

    @Operation(summary = "获取音乐文件")
    @GetMapping("/getMusicFile/{musicId}")
    public ResponseEntity<FileSystemResource> getMusicFile(@PathVariable Integer musicId, WebRequest request) {
        Music music = musicService.getById(musicId);
        if (music == null || music.getFilePath().isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        File musicFile = new File(music.getFilePath());
        if (!musicFile.exists()){
            return ResponseEntity.notFound().build();
        }
        //如果未更改，则直接返回未修改
        if (request.checkNotModified(musicFile.lastModified())){
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        try {
            final HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add("Content-Type", Files.probeContentType(Paths.get(music.getFilePath())));
            //缓存策略
            responseHeaders.setLastModified(musicFile.lastModified());
            responseHeaders.setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS));
            return new ResponseEntity<>(new FileSystemResource(musicFile), responseHeaders, HttpStatus.OK);
        } catch (IOException ig) {
            return ResponseEntity.notFound().build();
        }
    }

}
