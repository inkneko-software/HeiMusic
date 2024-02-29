package com.inkneko.heimusic.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inkneko.heimusic.config.HeiMusicConfig;
import com.inkneko.heimusic.config.MinIOConfig;
import com.inkneko.heimusic.model.entity.*;
import com.inkneko.heimusic.model.vo.ArtistVo;
import com.inkneko.heimusic.model.vo.MusicResourceVo;
import com.inkneko.heimusic.model.vo.MusicVo;
import com.inkneko.heimusic.model.vo.Response;
import com.inkneko.heimusic.service.AlbumService;
import com.inkneko.heimusic.service.ArtistService;
import com.inkneko.heimusic.service.MusicService;
import com.inkneko.heimusic.util.auth.AuthUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController()
@RequestMapping("/api/v1/search")
public class SearchController {

    ArtistService artistService;
    AlbumService albumService;
    MusicService musicService;
    HeiMusicConfig heiMusicConfig;
    MinIOConfig minIOConfig;

    public SearchController(ArtistService artistService, AlbumService albumService, MusicService musicService, HeiMusicConfig heiMusicConfig, MinIOConfig minIOConfig) {
        this.artistService = artistService;
        this.albumService = albumService;
        this.musicService = musicService;
        this.heiMusicConfig = heiMusicConfig;
        this.minIOConfig = minIOConfig;
    }

    @PostMapping("/searchMusic")
    public Response<List<MusicVo>> searchMusic(@RequestParam String prompt){
        Integer userId = AuthUtils.auth();
        Set<MusicVo> result = new LinkedHashSet<>();
        //首先基于音乐标题进行搜索
        List<Music> matchTitleMusics = musicService.getBaseMapper().selectList(new LambdaQueryWrapper<Music>().like(Music::getTitle, '%' + prompt + '%'));
        for (Music matchTitleMusic : matchTitleMusics){
            result.add(genMusicVo(matchTitleMusic, userId));
        }
        //其次是艺术家名称
        List<Artist> artists = artistService.getBaseMapper().selectList(new LambdaQueryWrapper<Artist>().like(Artist::getName, '%' + prompt + '%'));
        Map<Music, List<MusicArtist>> matchArtistMusics = musicService.getByContainsArtist(artists.stream().map(Artist::getArtistId).collect(Collectors.toList()));
        for(Music music : matchArtistMusics.keySet()){
            result.add(genMusicVo(music, userId));

        }
        //然后是专辑
        List<Album> albums = albumService.getBaseMapper().selectList(new LambdaQueryWrapper<Album>().like(Album::getTitle, '%' + prompt + '%'));
        for (Album album : albums){
            List<AlbumMusic> matchAlbumMusics = albumService.getAlbumMusicList(album.getAlbumId());
            for (AlbumMusic matchAlbumMusic : matchAlbumMusics){
                Music music = musicService.getById(matchAlbumMusic.getMusicId());
                if (music != null){
                    result.add(genMusicVo(music, userId));
                }
            }
        }
        return new Response<>(0, "ok", result.stream().toList());
    }

    private MusicVo genMusicVo(Music music, Integer userId){
        Album album = albumService.getAlbumMusicByMusicId(music.getMusicId());
        List<ArtistVo> artistVos = new ArrayList<>();
        List<MusicArtist> musicArtists = musicService.getMusicArtists(music.getMusicId());
        for( MusicArtist musicArtist : musicArtists){
            Artist artist = artistService.getById(musicArtist.getArtistId());
            artistVos.add(new ArtistVo(artist));
        }
        List<MusicResourceVo> musicResourceVos = new ArrayList<>();
        List<MusicResource> musicResources = musicService.getMusicResources(music.getMusicId());
        for (MusicResource musicResource : musicResources){
            musicResourceVos.add(new MusicResourceVo(musicResource, minIOConfig));
        }
        return new MusicVo(music, album, artistVos,musicResourceVos,heiMusicConfig, minIOConfig, userId != null && musicService.isFavorite(userId, music.getMusicId()));

    }
}
