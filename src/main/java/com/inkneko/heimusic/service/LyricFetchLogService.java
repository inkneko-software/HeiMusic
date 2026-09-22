package com.inkneko.heimusic.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.inkneko.heimusic.model.entity.LyricFetchLog;

public interface LyricFetchLogService extends IService<LyricFetchLog> {

    /**
     * 记录一次歌词拉取任务的结果。独立事务写入（主流程回滚不影响日志留存），
     * 且写入失败仅记录错误日志、不向上抛出，绝不影响拉取主流程
     *
     * @param source  来源：LyricFetchLog.SOURCE_MANUAL / SOURCE_MQ
     * @param musicId 音乐id，消息解析失败等场景可为null
     * @param outcome 结局：LyricFetchLog.OUTCOME_*
     * @param detail  补充信息（locale、错误摘要等），超长自动截断
     */
    void record(String source, Integer musicId, String outcome, String detail);

    /**
     * 分页查询拉取日志，按日志id倒序（时间倒序）
     *
     * @param current 页码，从1开始
     * @param size    每页条数
     * @param musicId 音乐id过滤，null表示不过滤
     * @param outcome 结局过滤，null或空白表示不过滤
     * @return 分页结果
     */
    Page<LyricFetchLog> getLogs(long current, long size, Integer musicId, String outcome);
}
