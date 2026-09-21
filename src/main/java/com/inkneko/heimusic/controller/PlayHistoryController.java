package com.inkneko.heimusic.controller;

import com.inkneko.heimusic.annotation.auth.UserAuth;
import com.inkneko.heimusic.config.MinIOConfig;
import com.inkneko.heimusic.model.entity.Album;
import com.inkneko.heimusic.model.entity.Music;
import com.inkneko.heimusic.model.entity.PlayHistory;
import com.inkneko.heimusic.model.vo.*;
import com.inkneko.heimusic.service.AlbumService;
import com.inkneko.heimusic.service.ArtistService;
import com.inkneko.heimusic.service.MusicService;
import com.inkneko.heimusic.service.PlayHistoryService;
import com.inkneko.heimusic.util.auth.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/playHistory")
public class PlayHistoryController {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;

    PlayHistoryService playHistoryService;
    MusicService musicService;
    AlbumService albumService;
    ArtistService artistService;
    MinIOConfig minIOConfig;

    public PlayHistoryController(PlayHistoryService playHistoryService, MusicService musicService, AlbumService albumService, ArtistService artistService, MinIOConfig minIOConfig) {
        this.playHistoryService = playHistoryService;
        this.musicService = musicService;
        this.albumService = albumService;
        this.artistService = artistService;
        this.minIOConfig = minIOConfig;
    }

    @Operation(summary = "上报一次播放", description = "实际开始播放时调用；首次插入记录，之后播放次数+1并刷新最近播放时间")
    @PostMapping("/report")
    @UserAuth
    public Response<?> report(@RequestParam Integer musicId) {
        playHistoryService.reportPlay(AuthUtils.auth(), musicId);
        return new Response<>(0, "ok");
    }

    @Operation(summary = "查询最近播放列表", description = "按最近播放时间倒序")
    @GetMapping("/getRecentList")
    @UserAuth
    public Response<List<PlayHistoryVo>> getRecentList(@RequestParam(required = false, defaultValue = "" + DEFAULT_LIMIT) Integer limit) {
        List<PlayHistoryVo> voList = toVoList(playHistoryService.getRecentList(AuthUtils.auth(), clampLimit(limit)));
        return new Response<>(0, "ok", voList);
    }

    @Operation(summary = "查询最常播放列表", description = "按播放次数倒序，并列时按最近播放时间倒序")
    @GetMapping("/getMostPlayedList")
    @UserAuth
    public Response<List<PlayHistoryVo>> getMostPlayedList(@RequestParam(required = false, defaultValue = "" + DEFAULT_LIMIT) Integer limit) {
        List<PlayHistoryVo> voList = toVoList(playHistoryService.getMostPlayedList(AuthUtils.auth(), clampLimit(limit)));
        return new Response<>(0, "ok", voList);
    }

    @Operation(summary = "删除单条播放历史")
    @PostMapping("/removeHistory")
    @UserAuth
    public Response<?> removeHistory(@RequestParam Integer musicId) {
        playHistoryService.removeHistory(AuthUtils.auth(), musicId);
        return new Response<>(0, "ok");
    }

    /**
     * limit钳制：非法值回落默认，超出上限取上限
     *
     * @param limit 前端传入的条数
     * @return 安全的条数
     */
    private int clampLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 组装播放历史VO列表（MusicVo组装链与PlaylistController.getMyFavoriteMusicList一致）
     *
     * @param histories 播放历史实体列表
     * @return 播放历史VO列表，保持输入顺序
     */
    private List<PlayHistoryVo> toVoList(List<PlayHistory> histories) {
        if (histories.isEmpty()) {
            return new ArrayList<>();
        }
        Integer userId = AuthUtils.auth();
        List<Integer> musicIdList = histories.stream().map(PlayHistory::getMusicId).collect(Collectors.toList());
        //listByIds无法保证与输入列表同序，用map存储结果
        HashMap<Integer, Music> musicIdMap = new HashMap<>();
        for (Music music : musicService.listByIds(musicIdList)) {
            musicIdMap.put(music.getMusicId(), music);
        }
        List<PlayHistoryVo> voList = new ArrayList<>(histories.size());
        for (PlayHistory history : histories) {
            Music music = musicIdMap.get(history.getMusicId());
            if (music == null) {
                //悬挂历史（理论上已被级联删除覆盖）跳过不抛错
                continue;
            }
            Integer musicId = history.getMusicId();
            Album album = albumService.getAlbumMusicByMusicId(musicId);
            List<ArtistVo> artistVoList = musicService.getMusicArtists(musicId)
                    .stream()
                    .map(musicArtist -> new ArtistVo(artistService.getById(musicArtist.getArtistId())))
                    .collect(Collectors.toList());
            List<MusicResourceVo> resourceVoList = musicService.getMusicResources(musicId)
                    .stream()
                    .map(musicResource -> new MusicResourceVo(musicResource, minIOConfig))
                    .collect(Collectors.toList());
            boolean isFavorite = musicService.isFavorite(userId, musicId);
            MusicVo musicVo = new MusicVo(music, album, artistVoList, resourceVoList, minIOConfig, isFavorite);
            voList.add(new PlayHistoryVo(musicVo, history));
        }
        return voList;
    }
}
