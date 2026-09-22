package com.inkneko.heimusic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.inkneko.heimusic.model.entity.Lyric;
import com.inkneko.heimusic.model.vo.LyricFetchVo;

import java.util.List;

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
     * 从 LRCLIB 拉取歌词。核心原则：拉取只服务无歌词的音乐，人工数据无条件优先，不提供覆盖
     * <p>
     * LRCLIB 标记纯音乐时联动 music.is_instrumental（仅当前为未知时采信）；
     * LRCLIB 暂无该曲目时 outcome 为 not_found（非错误，其后台会补录，可重试）；
     * 该音乐已有歌词时抛 LYRIC_FETCH_ALREADY_HAS_LYRIC。
     * 每次拉取尝试（含跳过与失败）均写入 lyric_fetch_log 任务日志
     *
     * @param musicId 音乐id
     * @param locale  显式语言标签（归一化后入库），null 时按歌词文本逐行投票自动判定，无法判定存 und
     * @param source  来源标识：LyricFetchLog.SOURCE_MANUAL / SOURCE_MQ
     * @return 拉取结果，outcome=created 时 lyric 为新创建的歌词
     */
    LyricFetchVo fetchFromLrclib(Integer musicId, String locale, String source);

    /**
     * 一键扫描：查询全部"无歌词且非纯音乐"的音乐，向 lyric-queue 批量投放拉取任务
     * <p>
     * 幂等性：已创建歌词的音乐不会重复入队（拉取永不覆盖人工数据），
     * 重复扫描的副作用仅为消费端 5005 跳过；
     * 60 秒节流窗口内的重复调用抛 LYRIC_SCAN_THROTTLED，防止 MQ 洪峰
     *
     * @return 本次投放的音乐数
     */
    int scanMissingLyric();
}
