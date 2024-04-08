package com.inkneko.heimusic.controller;

import com.inkneko.heimusic.annotation.auth.UserAuth;
import com.inkneko.heimusic.config.HeiMusicConfig;
import com.inkneko.heimusic.config.MinIOConfig;
import com.inkneko.heimusic.model.dto.AddPlaylistMusicDto;
import com.inkneko.heimusic.model.dto.RemovePlaylistMusicDto;
import com.inkneko.heimusic.model.dto.UpdatePlaylistInfoDto;
import com.inkneko.heimusic.model.entity.*;
import com.inkneko.heimusic.model.vo.*;
import com.inkneko.heimusic.service.AlbumService;
import com.inkneko.heimusic.service.ArtistService;
import com.inkneko.heimusic.service.MusicService;
import com.inkneko.heimusic.service.UserService;
import com.inkneko.heimusic.util.auth.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
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
    UserService userService;
    MinIOConfig minIOConfig;
    HeiMusicConfig heiMusicConfig;

    public PlaylistController(AlbumService albumService, MusicService musicService, UserService userService, MinIOConfig minIOConfig, ArtistService artistService, HeiMusicConfig heiMusicConfig) {
        this.albumService = albumService;
        this.musicService = musicService;
        this.userService = userService;
        this.minIOConfig = minIOConfig;
        this.artistService = artistService;
        this.heiMusicConfig = heiMusicConfig;
    }

    private List<ArtistVo> getMusicArtistVoList(Integer musicId) {
        return musicService.getMusicArtists(musicId)
                .stream()
                .map(musicArtist -> new ArtistVo(artistService.getById(musicArtist.getArtistId())))
                .collect(Collectors.toList());
    }

    private List<MusicResourceVo> getMusicResourceVoList(Integer musicId) {
        return musicService.getMusicResources(musicId)
                .stream()
                .map(musicResource -> new MusicResourceVo(musicResource, minIOConfig))
                .collect(Collectors.toList());
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
                    List<ArtistVo> musicArtistVos = getMusicArtistVoList(musicId);
                    //获取音乐的资源信息
                    List<MusicResourceVo> musicResourceVos = getMusicResourceVoList(musicId);
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
        musicService.addUserMusicFavorite(userId, musicId);
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
    public Response<Playlist> addPlaylist(@RequestParam String title, @RequestParam String description) {
        Integer userId = AuthUtils.auth();
        Playlist playlist = new Playlist();
        playlist.setUserId(userId);
        playlist.setTitle(title);
        playlist.setDescription(description);
        musicService.addPlaylist(playlist);
        return new Response<>(0, "ok", playlist);
    }

    @UserAuth
    @Operation(summary = "删除歌单")
    @PostMapping("/removePlaylist")
    public Response<?> removePlaylist(@RequestParam Integer playlistId) {
        Integer userId = AuthUtils.auth();
        musicService.removePlaylist(playlistId, userId);
        return new Response<>(0, "ok");
    }

    @UserAuth
    @Operation(summary = "更新歌单信息")
    @PostMapping("/updatePlaylistInfo")
    public Response<?> updatePlaylistInfo(@ModelAttribute UpdatePlaylistInfoDto dto) {
        Integer userId = AuthUtils.auth();
        Playlist playlist = new Playlist();
        playlist.setPlaylistId(dto.getPlaylistId());
        playlist.setTitle(dto.getTitle());
        playlist.setDescription(dto.getDescription());
        playlist.setSequenceNumber(dto.getSequenceNumber());

        //playlist.setCoverUrl(dto.getCoverFile());
        musicService.updatePlaylist(playlist, userId);
        return new Response<>(0, "ok");
    }

    @UserAuth(required = false)
    @Operation(summary = "查询歌单信息")
    @GetMapping("/getPlaylistInfo")
    public Response<PlaylistVo> getPlaylistInfo(@RequestParam Integer playlistId) {
        Integer userId = AuthUtils.auth();
        Playlist playlist = musicService.getPlaylist(playlistId);
        if (playlist == null) {
            return new Response<>(404, "指定的歌单不存在");
        }
        //非创建者，不显示编号
        if (!playlist.getUserId().equals(userId)) {
            playlist.setSequenceNumber(null);
        }
        UserDetail userDetail = userService.findUser(playlist.getUserId());
        PlaylistVo playlistVo = new PlaylistVo(playlist, new UserBasicVo(userDetail.getUserId(), userDetail.getUsername(), userDetail.getAvatarUrl()));
        return new Response<>(0, "ok", playlistVo);
    }

    @UserAuth
    @Operation(summary = "查询已创建的歌单")
    @GetMapping("/getUserCreatedPlaylist")
    public Response<List<PlaylistVo>> getCreatedPlaylistInfo() {
        Integer userId = AuthUtils.auth();
        List<Playlist> playlistList = musicService.getUserCreatedPlaylist(userId);

        UserDetail userDetail = userService.findUser(userId);
        List<PlaylistVo> playlistVoList = playlistList.stream().map(playlist -> new PlaylistVo(playlist, new UserBasicVo(userDetail.getUserId(), userDetail.getUsername(), userDetail.getAvatarUrl()))).toList();
        return new Response<>(0, "ok", playlistVoList);
    }

    @UserAuth(required = false)
    @Operation(summary = "查询歌单音乐")
    @GetMapping("/getPlaylistMusicList")
    public Response<List<MusicVo>> getPlaylistMusicList(@RequestParam Integer playlistId) {
        Integer userId = AuthUtils.auth();
        List<Music> musicList = musicService.getPlaylistMusicList(playlistId);
        List<MusicVo> musicVoList = musicList.stream().map(music -> {
            Integer musicId = music.getMusicId();
            //查询音乐所在的专辑
            Album album = albumService.getAlbumMusicByMusicId(musicId);
            //获取音乐的艺术家信息
            List<ArtistVo> musicArtistVos = getMusicArtistVoList(musicId);
            //获取音乐的资源信息
            List<MusicResourceVo> musicResourceVos = getMusicResourceVoList(musicId);
            boolean isFavorite = musicService.isFavorite(userId, musicId);
            return new MusicVo(music, album, musicArtistVos, musicResourceVos, heiMusicConfig, minIOConfig, isFavorite);
        }).toList();

        return new Response<>(0, "ok", musicVoList);
    }

    @UserAuth
    @Operation(summary = "向歌单添加音乐")
    @PostMapping("/addPlaylistMusic")
    public Response<?> addPlaylistMusic(@RequestBody AddPlaylistMusicDto dto) {
        Integer userId = AuthUtils.auth();
        musicService.addPlaylistMusic(dto.getPlaylistId(), dto.getMusicIdList(), userId);
        return new Response<>(0, "ok");
    }

    @UserAuth
    @Operation(summary = "删除歌单中的音乐")
    @PostMapping("/removePlaylistMusic")
    public Response<?> removePlaylistMusic(@RequestBody RemovePlaylistMusicDto dto) {
        Integer userId = AuthUtils.auth();
        musicService.removePlaylistMusic(dto.getPlaylistId(), dto.getMusicIdList(), userId);
        return new Response<>(0, "ok");
    }

    @UserAuth
    @Operation(summary = "收藏歌单")
    @PostMapping("/subscribePlaylist")
    public Response<?> subscribePlaylist(@RequestBody Integer playlistId) {
        Integer userId = AuthUtils.auth();
        musicService.addPlaylistSubscribe(playlistId, userId);
        return new Response<>(0, "ok");
    }

    @UserAuth
    @Operation(summary = "取消收藏歌单")
    @PostMapping("/removePlaylistSubscription")
    public Response<?> removePlaylistSubscription(@RequestBody Integer playlistId) {
        Integer userId = AuthUtils.auth();
        musicService.removePlaylistSubscribe(playlistId, userId);
        return new Response<>(0, "ok");
    }

    @UserAuth
    @Operation(summary = "查询已收藏的歌单")
    @GetMapping("/removePlaylistSubscription")
    public Response<List<PlaylistVo>> getPlaylistSubscriptionList() {
        Integer userId = AuthUtils.auth();
        List<Playlist> playlistList = musicService.getPlaylistSubscribed(userId);

        List<PlaylistVo> playlistVoList = playlistList.stream().map(playlist -> {
            UserDetail userDetail = userService.findUser(playlist.getUserId());
            return new PlaylistVo(playlist, new UserBasicVo(userDetail.getUserId(), userDetail.getUsername(), userDetail.getAvatarUrl()));
        }).toList();
        return new Response<>(0, "ok", playlistVoList);
    }
}
