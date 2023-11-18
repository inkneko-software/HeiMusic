package com.inkneko.heimusic.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inkneko.heimusic.annotation.auth.UserAuth;
import com.inkneko.heimusic.errorcode.MusicArtistServiceErrorCode;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.model.vo.ArtistVo;
import com.inkneko.heimusic.model.vo.Response;
import com.inkneko.heimusic.model.entity.Artist;
import com.inkneko.heimusic.service.ArtistService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Max;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/artist")
public class ArtistController extends AbstractController {

    ArtistService artistService;

    public ArtistController(ArtistService artistService) {
        this.artistService = artistService;
    }

    @UserAuth
    @PostMapping("/add")
    @Operation(summary = "添加艺术家", description = "通过名称，添加艺术家")
    Response<ArtistVo> addArtist(@RequestParam String name,
                               @RequestParam(required = false) String translateName,
                               @RequestParam(required = false) String avatarUrl,
                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd")  Date birth) {
        try {
            Artist artist = new Artist();
            artist.setName(name);
            artist.setTranslateName(translateName);
            artist.setBirth(birth);
            artist.setAvatarUrl(avatarUrl);
            artistService.save(artist);

            return new Response<>(0, "添加成功", new ArtistVo(artist));
        } catch (DataIntegrityViolationException e) {
            throw new ServiceException(MusicArtistServiceErrorCode.MUSIC_ARTIST_NAME_DUPLICATED);
        }
    }

    @GetMapping("/getById")
    @Operation(summary = "查询对应id的艺术家")
    Response<ArtistVo> getArtistByid(@RequestParam Integer id) {
        Artist artist  = artistService.getById(id);
        if (artist == null) {
            throw new ServiceException(MusicArtistServiceErrorCode.MUSIC_ARTIST_NOT_FOUND);
        }
        return new Response<>(0, "ok", new ArtistVo(artist));
    }

    @GetMapping("/getByName")
    @Operation(summary = "查询名称全匹配的艺术家")
    Response<ArtistVo> getArtistByName(@RequestParam String name) {
        Artist artist = artistService.getOne(new LambdaQueryWrapper<Artist>().eq(Artist::getName, name));
        if (artist == null) {
            throw new ServiceException(MusicArtistServiceErrorCode.MUSIC_ARTIST_NOT_FOUND);
        }
        return new Response<>(0, "ok", new ArtistVo(artist));
    }


    @PostMapping("/update")
    @Operation(summary = "更新艺术家信息")
    Response<?> updateArtist(@RequestParam Integer artistId,
                          @RequestParam(required = false) String name,
                          @RequestParam(required = false) String translateName,
                          @RequestParam(required = false) String avatarUrl,
                          @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd")  Date birth){
        Artist artist = new Artist();
        artist.setArtistId(artistId);
        artist.setName(name);
        artist.setTranslateName(translateName);
        artist.setBirth(birth);
        artist.setAvatarUrl(avatarUrl);
        artistService.updateById(artist);
        return new Response<>(0, "保存成功");
    }

    @GetMapping("/search")
    @Operation(summary = "模糊匹配艺术家名称")
    Response<List<ArtistVo>> searchArtist(@RequestParam String name,
                          @RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") @Max(50) Integer num){
        Page<Artist> requiredPage = Page.of(page, num);
        LambdaQueryWrapper<Artist> condition = new LambdaQueryWrapper<>();
        condition.like(Artist::getName, name );
        List<ArtistVo> artistVos = artistService.page(requiredPage, condition)
                .getRecords()
                .stream()
                .map(ArtistVo::new)
                .collect(Collectors.toList());
        return new Response<>(0, "ok", artistVos);
    }
}
