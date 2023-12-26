package com.inkneko.heimusic.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inkneko.heimusic.annotation.auth.UserAuth;
import com.inkneko.heimusic.config.MinIOConfig;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.model.dto.UpdateAlbumInfoDto;
import com.inkneko.heimusic.model.entity.Album;
import com.inkneko.heimusic.model.entity.Music;
import com.inkneko.heimusic.model.vo.*;
import com.inkneko.heimusic.service.AlbumService;
import com.inkneko.heimusic.service.ArtistService;
import com.inkneko.heimusic.service.MinIOService;
import com.inkneko.heimusic.service.MusicService;
import com.inkneko.heimusic.util.auth.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/album")
public class AlbumController {

    AlbumService albumService;
    MusicService musicService;
    ArtistService artistService;
    MinIOService minIOService;
    MinIOConfig minIOConfig;


    public AlbumController(AlbumService albumService, MusicService musicService, ArtistService artistService, MinIOService minIOService, MinIOConfig minIOConfig) {
        this.albumService = albumService;
        this.musicService = musicService;
        this.artistService = artistService;
        this.minIOService = minIOService;
        this.minIOConfig = minIOConfig;
    }

    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "添加专辑")
    public Response<AlbumVo> addAlbum(@RequestParam String title,
                                      @RequestParam(required = false) String translateTitle,
                                      @RequestParam(required = false) List<Integer> artistList,
                                      @RequestParam(required = false) MultipartFile frontCover) {
        //创建专辑
        Album album = new Album();
        album.setTitle(title);
        album.setTranslateTitle(translateTitle);
        albumService.save(album);
        //保存专辑封面至minio，然后更新专辑封面url
        if (frontCover != null) {
            minIOService.upload("heimusic", String.format("cover/%d_front", album.getAlbumId()), frontCover);
            album.setFrontCoverUrl(String.format("%s/heimusic/cover/%d_front", minIOConfig.getEndpoint(), album.getAlbumId()));
            albumService.updateById(album);
        }
        //保存专辑艺术家
        if (artistList != null) {
            albumService.addAlbumArtist(album.getAlbumId(), artistList);
        }


        return getAlbum(album.getAlbumId());
    }

    @PostMapping("/addMusic")
    @Operation(summary = "向专辑添加音乐")
    public Response<?> addAlbumMusic(@RequestParam Integer albumId,
                                     @RequestParam List<Integer> musicIds) {
        albumService.addAlbumMusic(albumId, musicIds);
        return new Response<>(0, "ok");
    }

    @GetMapping("/get")
    @Operation(summary = "查询专辑基础信息")
    public Response<AlbumVo> getAlbum(@RequestParam Integer albumId) {
        Album album = albumService.getById(albumId);
        List<ArtistVo> artistVos = albumService.getAlbumArtist(albumId)
                .stream()
                .map(albumArtist -> new ArtistVo(artistService.getById(albumArtist.getArtistId())))
                .collect(Collectors.toList());
        return new Response<>(0, "ok", new AlbumVo(album, artistVos, albumService.getAlbumMusicNum(albumId)));
    }

    @GetMapping("/getMusicList")
    @Operation(summary = "查询专辑音乐列表")
    @UserAuth(required = false)
    public Response<List<MusicVo>> getAlbumMusicList(@RequestParam Integer albumId) {
        Album album = albumService.getById(albumId);
        Integer userId = AuthUtils.auth();
        if (album == null){
            throw new ServiceException(404, "指定专辑不存在");
        }
        List<MusicVo> musicVos = albumService.getAlbumMusicList(albumId)
                .stream()
                .map(albumMusic -> {
                    Integer musicId = albumMusic.getMusicId();
                    Music music = musicService.getById(musicId);
                    List<ArtistVo> musicArtistVos = musicService.getMusicArtists(musicId)
                            .stream()
                            .map(musicArtist -> new ArtistVo(artistService.getById(musicArtist.getArtistId())))
                            .collect(Collectors.toList());
                    List<MusicResourceVo> musicResourceVos = musicService.getMusicResources(musicId)
                            .stream()
                            .map(musicResource -> new MusicResourceVo(musicResource, minIOConfig.getEndpoint()))
                            .collect(Collectors.toList());
                    boolean isFavorite = userId != null && musicService.isFavorite(userId, musicId);
                    return new MusicVo(music, album, musicArtistVos, musicResourceVos, minIOConfig.getEndpoint(), isFavorite);
                })
                .collect(Collectors.toList());

        return new Response<>(0, "ok", musicVos);
    }

    @GetMapping("/getRecentUpload")
    @Operation(summary = "查询最新上传的专辑，默认10个")
    public Response<List<AlbumVo>> getRecentUpload(@RequestParam(defaultValue = "1") Integer current, @RequestParam(defaultValue = "10") Integer size) {
        List<AlbumVo> result = albumService.page(new Page<Album>(current, size), new LambdaQueryWrapper<Album>().orderByDesc(Album::getAlbumId))
                .getRecords()
                .stream()
                .map(album -> {
                    List<ArtistVo> musicArtistVos = albumService.getAlbumArtist(album.getAlbumId())
                            .stream()
                            .map(musicArtist -> new ArtistVo(artistService.getById(musicArtist.getArtistId())))
                            .collect(Collectors.toList());
                    return new AlbumVo(album, musicArtistVos, albumService.getAlbumMusicNum(album.getAlbumId()));
                })
                .collect(Collectors.toList());
        return new Response<>(0, "ok", result);
    }

    @UserAuth
    @GetMapping("/getAlbumList")
    @Operation(summary = "查询已上传专辑")
    public Response<AlbumListVo> getAlbumList(@RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer size){
        List<AlbumVo> result = albumService.page(new Page<Album>(page, size), new LambdaQueryWrapper<Album>().orderByDesc(Album::getAlbumId))
                .getRecords()
                .stream()
                .map(album -> {
                    List<ArtistVo> musicArtistVos = albumService.getAlbumArtist(album.getAlbumId())
                            .stream()
                            .map(musicArtist -> new ArtistVo(artistService.getById(musicArtist.getArtistId())))
                            .collect(Collectors.toList());
                    return new AlbumVo(album, musicArtistVos, albumService.getAlbumMusicNum(album.getAlbumId()));
                })
                .collect(Collectors.toList());
        return new Response<>(0, "ok", new AlbumListVo(result, albumService.count()));
    }

    @PostMapping(value = "/updateAlbumInfo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "更改专辑基础信息")
    public Response<?> updateAlbumInfo(@ModelAttribute UpdateAlbumInfoDto updateAlbumInfoDto){
        Album album = albumService.getById(updateAlbumInfoDto.getAlbumId());
        if (album == null){
            throw new ServiceException(404, "专辑不存在");
        }

        if (updateAlbumInfoDto.getCover() != null){
            minIOService.upload("heimusic", String.format("cover/%d_front", album.getAlbumId()), updateAlbumInfoDto.getCover());
            album.setFrontCoverUrl(String.format("%s/heimusic/cover/%d_front", minIOConfig.getEndpoint(), album.getAlbumId()));
        }

        if (updateAlbumInfoDto.isDeleteCover()){
            album.setFrontCoverUrl(null);
        }

        album.setTitle(updateAlbumInfoDto.getTitle());
        albumService.updateById(album);
        if (updateAlbumInfoDto.getArtistList() != null){
            albumService.updateAlbumArtist(album.getAlbumId(), updateAlbumInfoDto.getArtistList());
        }
        return new Response<>(0, "ok");
    }

    @PostMapping("/removeAlbum")
    @Operation(summary = "删除专辑")
    @UserAuth(requireRootPrivilege = true)
    public Response<?> removeAlbum(@RequestParam Integer albumId){
        albumService.removeById(albumId);
        return new Response<>(0, "ok");
    }
}
