package com.inkneko.heimusic.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inkneko.heimusic.annotation.auth.UserAuth;
import com.inkneko.heimusic.config.MinIOConfig;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.model.dto.UpdateAlbumInfoDto;
import com.inkneko.heimusic.model.entity.Album;
import com.inkneko.heimusic.model.entity.Music;
import com.inkneko.heimusic.model.entity.MusicArtist;
import com.inkneko.heimusic.model.vo.*;
import com.inkneko.heimusic.service.AlbumService;
import com.inkneko.heimusic.service.ArtistService;
import com.inkneko.heimusic.service.MinIOService;
import com.inkneko.heimusic.service.MusicService;
import com.inkneko.heimusic.util.auth.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
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
        if (album == null) {
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
    public Response<AlbumListVo> getAlbumList(@RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer size) {
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
    public Response<?> updateAlbumInfo(@ModelAttribute UpdateAlbumInfoDto updateAlbumInfoDto) {
        Album album = albumService.getById(updateAlbumInfoDto.getAlbumId());
        if (album == null) {
            throw new ServiceException(404, "专辑不存在");
        }

        if (updateAlbumInfoDto.getCover() != null) {
            minIOService.upload("heimusic", String.format("cover/%d_front", album.getAlbumId()), updateAlbumInfoDto.getCover());
            album.setFrontCoverUrl(String.format("%s/heimusic/cover/%d_front", minIOConfig.getEndpoint(), album.getAlbumId()));
        }

        if (updateAlbumInfoDto.isDeleteCover()) {
            album.setFrontCoverUrl(null);
        }

        album.setTitle(updateAlbumInfoDto.getTitle());
        albumService.updateById(album);
        if (updateAlbumInfoDto.getArtistList() != null) {
            albumService.updateAlbumArtist(album.getAlbumId(), updateAlbumInfoDto.getArtistList());
        }
        return new Response<>(0, "ok");
    }

    @PostMapping("/removeAlbum")
    @Operation(summary = "删除专辑")
    @UserAuth(requireRootPrivilege = true)
    public Response<?> removeAlbum(@RequestParam Integer albumId) {
        albumService.removeById(albumId);
        return new Response<>(0, "ok");
    }

    @Operation(summary = "随机播放一首音乐")
    @PostMapping("/randomMusic")
    @UserAuth
    public Response<MusicVo> randomMusic() {
        Integer userId = AuthUtils.auth();
        if (userId == null) {
            throw new ServiceException(403, "请先登录");
        }
        Music lastMusic = musicService.getOne(new LambdaQueryWrapper<Music>().orderByDesc(Music::getMusicId).last("LIMIT 1"));
        //需要把ID字段的Integer换成Long。数据库设计失误，所有的ID应当使用BIGINT
        Random random = new Random(userId + new Date().getTime());

        Music selectedMusic = null;
        Album album = null;
        while (selectedMusic == null || selectedMusic.getBucket() == null) {
            Integer musicId = (int) random.nextLong() % lastMusic.getMusicId();
            selectedMusic = musicService.getById(musicId);
            if (selectedMusic != null) {
                album = albumService.getAlbumMusicByMusicId(selectedMusic.getMusicId());
                if (album == null) {
                    selectedMusic = null;
                }
            }

        }

        List<ArtistVo> artistVos = musicService
                .getMusicArtists(selectedMusic.getMusicId())
                .stream()
                .map(MusicArtist::getArtistId)
                .map(artistId -> artistService.getById(artistId))
                .map(ArtistVo::new)
                .collect(Collectors.toList());
        List<MusicResourceVo> musicResourceVos = musicService
                .getMusicResources(selectedMusic.getMusicId())
                .stream()
                .map(musicResource -> new MusicResourceVo(musicResource, minIOConfig.getEndpoint()))
                .collect(Collectors.toList());
        return new Response<>(
                0,
                "ok",
                new MusicVo(selectedMusic, album, artistVos, musicResourceVos, minIOConfig.getEndpoint(), musicService.isFavorite(userId, selectedMusic.getMusicId())));


    }

    @Operation(summary = "用户每日推荐30首（随机30首，无个性推荐功能）", description = "每日6点更新")
    @PostMapping("/daily30")
    @UserAuth
    public Response<List<MusicVo>> daily30() {
        Integer userId = AuthUtils.auth();
        if (userId == null) {
            throw new ServiceException(403, "请先登录");
        }
        long count = musicService.count();
        if (count < 100) {
            throw new ServiceException(500, "音乐数量小于100，请添加更多音乐后尝试");
        }

        Music lastMusic = musicService.getOne(new LambdaQueryWrapper<Music>().orderByDesc(Music::getMusicId).last("LIMIT 1"));
        //每日6点更新
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 6);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Random random = new Random(userId + calendar.getTimeInMillis());

        List<MusicVo> recommendMusicList = new ArrayList<>();
        HashSet<Integer> recommendSet = new HashSet<>();
        for (int i = 0; i < 30; i++) {
            Music selectedMusic = null;
            Album album = null;

            while (selectedMusic == null || selectedMusic.getBucket() == null) {
                Integer musicId = (int) random.nextLong() % lastMusic.getMusicId();
                selectedMusic = musicService.getById(musicId);
                if (selectedMusic != null && !recommendSet.contains(selectedMusic.getMusicId())) {
                    album = albumService.getAlbumMusicByMusicId(selectedMusic.getMusicId());
                    if (album != null) {
                        recommendSet.add(selectedMusic.getMusicId());
                        continue;
                    }
                }
                selectedMusic = null;

            }
            List<ArtistVo> artistVos = musicService
                    .getMusicArtists(selectedMusic.getMusicId())
                    .stream()
                    .map(MusicArtist::getArtistId)
                    .map(artistId -> artistService.getById(artistId))
                    .map(ArtistVo::new)
                    .collect(Collectors.toList());
            List<MusicResourceVo> musicResourceVos = musicService
                    .getMusicResources(selectedMusic.getMusicId())
                    .stream()
                    .map(musicResource -> new MusicResourceVo(musicResource, minIOConfig.getEndpoint()))
                    .collect(Collectors.toList());
            recommendMusicList.add(new MusicVo(selectedMusic, album, artistVos, musicResourceVos, minIOConfig.getEndpoint(), musicService.isFavorite(userId, selectedMusic.getMusicId())));
        }
        return new Response<>(0, "ok", recommendMusicList);
    }
}
