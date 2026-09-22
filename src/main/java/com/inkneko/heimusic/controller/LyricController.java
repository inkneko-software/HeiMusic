package com.inkneko.heimusic.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inkneko.heimusic.annotation.auth.UserAuth;
import com.inkneko.heimusic.errorcode.LyricServiceErrorCode;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.model.dto.AddLyricDto;
import com.inkneko.heimusic.model.dto.UpdateLyricDto;
import com.inkneko.heimusic.model.entity.Lyric;
import com.inkneko.heimusic.model.entity.LyricFetchLog;
import com.inkneko.heimusic.model.entity.Music;
import com.inkneko.heimusic.model.vo.LyricCoverageVo;
import com.inkneko.heimusic.model.vo.LyricFetchVo;
import com.inkneko.heimusic.model.vo.LyricVo;
import com.inkneko.heimusic.model.vo.Response;
import com.inkneko.heimusic.service.LyricFetchLogService;
import com.inkneko.heimusic.service.LyricFetchService;
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
    LyricFetchService lyricFetchService;
    MusicService musicService;
    LyricFetchLogService lyricFetchLogService;

    public LyricController(LyricService lyricService, LyricFetchService lyricFetchService,
                           MusicService musicService, LyricFetchLogService lyricFetchLogService) {
        this.lyricService = lyricService;
        this.lyricFetchService = lyricFetchService;
        this.musicService = musicService;
        this.lyricFetchLogService = lyricFetchLogService;
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

    @Operation(summary = "从LRCLIB拉取歌词",
            description = "仅服务无歌词的音乐，人工数据无条件优先。outcome：created=已创建歌词（lyric字段非空）/"
                    + "instrumental=LRCLIB标记纯音乐/not_found=LRCLIB暂无该曲目（后台会补录，可重试）。"
                    + "locale不传时按歌词文本自动判定语言，无法判定存und")
    @PostMapping("/fetchFromLrclib")
    @UserAuth(requireRootPrivilege = true)
    public Response<LyricFetchVo> fetchFromLrclib(@RequestParam Integer musicId,
                                                  @RequestParam(required = false) String locale) {
        return new Response<>(0, "ok", lyricFetchService.fetchFromLrclib(musicId, locale, LyricFetchLog.SOURCE_MANUAL));
    }

    @Operation(summary = "一键扫描缺失歌词",
            description = "查询全部无歌词且非纯音乐的音乐，投放批量拉取队列（消费端以2秒/首串行执行，"
                    + "不覆盖已有歌词）。60秒节流窗口内重复调用返回5006。data=本次投放的音乐数")
    @PostMapping("/scanMissingLyric")
    @UserAuth(requireRootPrivilege = true)
    public Response<Integer> scanMissingLyric() {
        return new Response<>(0, "ok", lyricFetchService.scanMissingLyric());
    }

    @Operation(summary = "歌词覆盖率统计",
            description = "返回音乐总数与有歌词的音乐数（同音乐多语言仅计一次），用于歌词拉取进度展示")
    @GetMapping("/getCoverage")
    @UserAuth(requireRootPrivilege = true)
    public Response<LyricCoverageVo> getCoverage() {
        return new Response<>(0, "ok", lyricService.getLyricCoverage());
    }

    @Operation(summary = "分页查询歌词拉取任务日志",
            description = "按时间倒序，记录每次拉取尝试的结局（含批量任务与手动拉取）。"
                    + "outcome：created=已创建歌词/instrumental=纯音乐/not_found=暂无曲目/"
                    + "skipped=跳过（已有歌词等）/failed=失败")
    @GetMapping("/fetchLog/list")
    @UserAuth(requireRootPrivilege = true)
    public Response<Page<LyricFetchLog>> fetchLogList(@RequestParam(defaultValue = "1") long page,
                                                      @RequestParam(defaultValue = "20") long pageSize,
                                                      @RequestParam(required = false) Integer musicId,
                                                      @RequestParam(required = false) String outcome) {
        return new Response<>(0, "ok", lyricFetchLogService.getLogs(page, pageSize, musicId, outcome));
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
