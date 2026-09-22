package com.inkneko.heimusic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inkneko.heimusic.mapper.LyricFetchLogMapper;
import com.inkneko.heimusic.model.entity.LyricFetchLog;
import com.inkneko.heimusic.service.LyricFetchLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LyricFetchLogServiceImpl extends ServiceImpl<LyricFetchLogMapper, LyricFetchLog> implements LyricFetchLogService {
    Logger logger = LoggerFactory.getLogger(LyricFetchLogServiceImpl.class);

    /**
     * detail 字段最大长度，与表定义一致
     */
    private static final int DETAIL_MAX_LENGTH = 512;

    /**
     * 独立事务写入：拉取主流程的事务回滚（如并发校验失败）不影响日志留存；
     * 写入失败仅记录错误日志，绝不向上抛出影响拉取主流程
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String source, Integer musicId, String outcome, String detail) {
        try {
            LyricFetchLog logEntry = new LyricFetchLog(null, musicId, source, outcome,
                    detail == null ? null : detail.substring(0, Math.min(detail.length(), DETAIL_MAX_LENGTH)), null);
            save(logEntry);
        } catch (Exception e) {
            logger.error("歌词拉取日志写入失败：", e);
        }
    }

    /**
     * 分页查询拉取日志，按日志id倒序（时间倒序）
     */
    @Override
    public Page<LyricFetchLog> getLogs(long current, long size, Integer musicId, String outcome) {
        LambdaQueryWrapper<LyricFetchLog> wrapper = new LambdaQueryWrapper<LyricFetchLog>()
                .eq(musicId != null, LyricFetchLog::getMusicId, musicId)
                .eq(outcome != null && !outcome.isBlank(), LyricFetchLog::getOutcome, outcome)
                .orderByDesc(LyricFetchLog::getId);
        return page(new Page<>(current, size), wrapper);
    }
}
