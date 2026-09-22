package com.inkneko.heimusic.service;

import com.inkneko.heimusic.model.vo.LyricFetchVo;

/**
 * LRCLIB 歌词拉取编排：外部数据获取、批量投放与任务日志，
 * 数据存取经 LyricService 完成（跨 bean 调用走代理，缓存/事务注解正常生效）。
 * 核心原则：拉取只服务无歌词的音乐，人工数据无条件优先，不提供覆盖
 */
public interface LyricFetchService {

    /**
     * 从 LRCLIB 拉取歌词，并记录任务日志（含跳过与失败结局）。
     * 日志经独立事务写入，主流程回滚不影响日志留存
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
