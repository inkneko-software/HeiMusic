package com.inkneko.heimusic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.inkneko.heimusic.model.entity.Lyric;
import com.inkneko.heimusic.model.vo.LyricCoverageVo;

import java.util.List;

/**
 * lyric 表数据存取与缓存维护。LRCLIB 拉取编排见 LyricFetchService
 */
public interface LyricService extends IService<Lyric> {

    /**
     * 为音乐添加歌词，同一音乐同一语言（locale）仅允许一份
     *
     * @param musicId 音乐id
     * @param locale  语言标签，BCP 47风格，入库前归一化为小写
     * @param format  歌词格式标识：text/lrc/lrc_a2/qrc等
     * @param content 歌词全文
     * @return 新增的歌词（含自增id）
     */
    Lyric addLyric(Integer musicId, String locale, String format, String content);

    /**
     * 更新歌词的内容与格式，两者至少提供其一；不支持修改locale与所属音乐（改语言=删旧增新）
     *
     * @param lyricId 歌词id
     * @param format  歌词格式，null表示不变更
     * @param content 歌词内容，null表示不变更
     * @return 更新后的歌词
     */
    Lyric updateLyric(Integer lyricId, String format, String content);

    /**
     * 删除歌词；若被music.default_lyric_id引用则同步清除引用
     *
     * @param lyricId 歌词id
     */
    void removeLyric(Integer lyricId);

    /**
     * 删除某音乐下的全部歌词（删除音乐时级联调用），并清理默认歌词引用
     *
     * @param musicId 音乐id
     */
    void removeByMusicId(Integer musicId);

    /**
     * 查询某音乐的全部歌词
     *
     * @param musicId 音乐id
     * @return 歌词列表，按歌词id升序
     */
    List<Lyric> getMusicLyrics(Integer musicId);

    /**
     * 查询某音乐指定语言的歌词
     *
     * @param musicId 音乐id
     * @param locale  语言标签，查询前归一化为小写
     * @return 歌词，不存在时返回null
     */
    Lyric getMusicLyricByLocale(Integer musicId, String locale);

    /**
     * 设定/取消音乐的默认歌词
     *
     * @param musicId 音乐id
     * @param lyricId 默认歌词id，必须属于该音乐；null表示取消默认
     */
    void setDefaultLyric(Integer musicId, Integer lyricId);

    /**
     * 歌词覆盖率统计：音乐总数与有歌词的音乐数（同音乐多语言仅计一次），
     * 用于管理端展示歌词拉取进度
     *
     * @return 覆盖率统计
     */
    LyricCoverageVo getLyricCoverage();
}
