package com.inkneko.heimusic.controller;

import com.inkneko.heimusic.annotation.auth.UserAuth;
import com.inkneko.heimusic.errorcode.LyricServiceErrorCode;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.model.dto.AddLyricDto;
import com.inkneko.heimusic.model.dto.UpdateLyricDto;
import com.inkneko.heimusic.model.entity.Lyric;
import com.inkneko.heimusic.model.entity.Music;
import com.inkneko.heimusic.model.vo.LyricVo;
import com.inkneko.heimusic.model.vo.Response;
import com.inkneko.heimusic.service.LyricService;
import com.inkneko.heimusic.service.MusicService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/lyric")
public class LyricController {

    LyricService lyricService;
    MusicService musicService;

    public LyricController(LyricService lyricService, MusicService musicService) {
        this.lyricService = lyricService;
        this.musicService = musicService;
    }

    @Operation(summary = "添加歌词", description = "同一音乐同一语言（locale）仅允许一份；翻译=另一条locale记录")
    @PostMapping("/add")
    @UserAuth(requireRootPrivilege = true)
    public Response<LyricVo> add(@Valid @RequestBody AddLyricDto dto) {
        Lyric lyric = lyricService.addLyric(dto.getMusicId(), dto.getLocale(), dto.getFormat(), dto.getContent());
        return new Response<>(0, "ok", toVo(lyric));
    }

    @Operation(summary = "更新歌词", description = "仅更新提供的内容与格式字段；不支持修改语言与所属音乐（改语言=删旧增新）")
    @PostMapping("/updateLyric")
    @UserAuth(requireRootPrivilege = true)
    public Response<LyricVo> updateLyric(@Valid @RequestBody UpdateLyricDto dto) {
        Lyric lyric = lyricService.updateLyric(dto.getLyricId(), dto.getFormat(), dto.getContent());
        return new Response<>(0, "ok", toVo(lyric));
    }

    @Operation(summary = "删除歌词", description = "若为默认歌词则同步清除music表中的默认引用")
    @PostMapping("/removeLyric")
    @UserAuth(requireRootPrivilege = true)
    public Response<?> removeLyric(@RequestParam Integer lyricId) {
        lyricService.removeLyric(lyricId);
        return new Response<>(0, "ok");
    }

    @Operation(summary = "设定/取消默认歌词", description = "lyricId不传表示取消默认")
    @PostMapping("/setDefaultLyric")
    @UserAuth(requireRootPrivilege = true)
    public Response<?> setDefaultLyric(@RequestParam Integer musicId,
                                       @RequestParam(required = false) Integer lyricId) {
        lyricService.setDefaultLyric(musicId, lyricId);
        return new Response<>(0, "ok");
    }

    @Operation(summary = "查询某音乐的全部歌词", description = "返回含content的完整列表，isDefault标识默认歌词")
    @GetMapping("/getList")
    @UserAuth
    public Response<List<LyricVo>> getList(@RequestParam Integer musicId) {
        List<LyricVo> lyricVoList = lyricService.getMusicLyrics(musicId).stream()
                .map(this::toVo)
                .collect(Collectors.toList());
        return new Response<>(0, "ok", lyricVoList);
    }

    @Operation(summary = "按id查询歌词")
    @GetMapping("/get")
    @UserAuth
    public Response<LyricVo> get(@RequestParam Integer lyricId) {
        Lyric lyric = lyricService.getById(lyricId);
        if (lyric == null) {
            throw new ServiceException(LyricServiceErrorCode.LYRIC_NOT_FOUND);
        }
        return new Response<>(0, "ok", toVo(lyric));
    }

    @Operation(summary = "查询某音乐指定语言的歌词")
    @GetMapping("/getByLocale")
    @UserAuth
    public Response<LyricVo> getByLocale(@RequestParam Integer musicId, @RequestParam String locale) {
        Lyric lyric = lyricService.getMusicLyricByLocale(musicId, locale);
        if (lyric == null) {
            throw new ServiceException(LyricServiceErrorCode.LYRIC_NOT_FOUND);
        }
        return new Response<>(0, "ok", toVo(lyric));
    }

    /**
     * 组装歌词VO，附带默认歌词标识（读侧容忍悬挂引用：music不存在或默认歌词已被删除时isDefault=false）
     *
     * @param lyric 歌词实体
     * @return 歌词VO
     */
    private LyricVo toVo(Lyric lyric) {
        Music music = musicService.getById(lyric.getMusicId());
        Integer defaultLyricId = music == null ? null : music.getDefaultLyricId();
        return new LyricVo(lyric, defaultLyricId);
    }
}
