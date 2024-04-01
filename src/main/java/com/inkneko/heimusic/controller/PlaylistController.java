package com.inkneko.heimusic.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inkneko.heimusic.annotation.auth.UserAuth;
import com.inkneko.heimusic.config.HeiMusicConfig;
import com.inkneko.heimusic.config.MinIOConfig;
import com.inkneko.heimusic.mapper.MusicFavoriteMapper;
import com.inkneko.heimusic.model.entity.Album;
import com.inkneko.heimusic.model.entity.Music;
import com.inkneko.heimusic.model.entity.MusicFavorite;
import com.inkneko.heimusic.model.entity.Playlist;
import com.inkneko.heimusic.model.vo.ArtistVo;
import com.inkneko.heimusic.model.vo.MusicResourceVo;
import com.inkneko.heimusic.model.vo.MusicVo;
import com.inkneko.heimusic.model.vo.Response;
import com.inkneko.heimusic.service.AlbumService;
import com.inkneko.heimusic.service.ArtistService;
import com.inkneko.heimusic.service.MusicService;
import com.inkneko.heimusic.util.auth.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/playlist")
public class PlaylistController {
    AlbumService albumService;
    MusicService musicService;
    ArtistService artistService;

    MinIOConfig minIOConfig;
    HeiMusicConfig heiMusicConfig;

    public PlaylistController(AlbumService albumService, MusicService musicService, MusicFavoriteMapper musicFavoriteMapper, MinIOConfig minIOConfig, ArtistService artistService, HeiMusicConfig heiMusicConfig) {
        this.albumService = albumService;
        this.musicService = musicService;
        this.minIOConfig = minIOConfig;
        this.artistService = artistService;
        this.heiMusicConfig = heiMusicConfig;
    }

    @UserAuth
    @Operation(summary = "查询用户收藏的音乐")
    @GetMapping("/getMyFavoriteMusicList")
    public Response<List<MusicVo>> getMyFavoriteMusicList(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        //查询用户收藏的音乐
        List<MusicFavorite> userMusicFavoriteList = musicService.getUserMusicFavoriteList(userId);
        //提取音乐ID
        List<Integer> musicIdList = userMusicFavoriteList.stream().map(MusicFavorite::getMusicId).collect(Collectors.toList());
        if (musicIdList.isEmpty()) {
            return new Response<>(0, "ok", new ArrayList<>());
        }
        //由于listByIds无法返回与输入列表一样的顺序，使用map存储结果
        HashMap<Integer, Music> musicIdMap = new HashMap<>();
        for (Music music : musicService.listByIds(musicIdList)) {
            musicIdMap.put(music.getMusicId(), music);
        }
        //转换为vo
        List<MusicVo> musicVoList = musicIdList
                .stream()
                .map(musicId -> {
                    Music music = musicIdMap.get(musicId);
                    //查询音乐所在的专辑
                    Album album = albumService.getAlbumMusicByMusicId(musicId);
                    //获取音乐的艺术家信息
                    List<ArtistVo> musicArtistVos = musicService.getMusicArtists(musicId)
                            .stream()
                            .map(musicArtist -> new ArtistVo(artistService.getById(musicArtist.getArtistId())))
                            .collect(Collectors.toList());
                    //获取音乐的资源信息
                    List<MusicResourceVo> musicResourceVos = musicService.getMusicResources(musicId)
                            .stream()
                            .map(musicResource -> new MusicResourceVo(musicResource, minIOConfig))
                            .collect(Collectors.toList());
                    boolean isFavorite = musicService.isFavorite(userId, musicId);
                    return new MusicVo(music, album, musicArtistVos, musicResourceVos, heiMusicConfig, minIOConfig, isFavorite);
                })
                .collect(Collectors.toList());

        return new Response<>(0, "ok", musicVoList);
    }

    @UserAuth
    @Operation(summary = "收藏音乐")
    @PostMapping("/addMusicFavorite")
    public Response<?> addMusicFavorite(@RequestParam Integer musicId, HttpServletRequest request) {
        Integer userId = AuthUtils.auth();
        musicService.addUserMusicFavorite(userId,musicId);
        return new Response<>(0, "ok");
    }

    @UserAuth
    @Operation(summary = "取消收藏音乐")
    @PostMapping("/removeMusicFavorite")
    public Response<?> removeMusicFavorite(@RequestParam Integer musicId, HttpServletRequest request) {
        Integer userId = AuthUtils.auth();
        musicService.removeUserMusicFavorite(userId, musicId);
        return new Response<>(0, "ok");
    }

    @UserAuth
    @Operation(summary = "创建歌单")
    @PostMapping("/addPlaylist")
    public Response<Playlist> addPlaylist(){
        
        return new Response<>(0, "ok", null);
    }


}
