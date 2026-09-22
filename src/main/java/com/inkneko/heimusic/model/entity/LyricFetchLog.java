package com.inkneko.heimusic.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * LRCLIB 歌词拉取任务日志：每次拉取尝试一条记录，供管理端分页查询
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LyricFetchLog implements Serializable {

    /**
     * 来源：手动接口
     */
    public static final String SOURCE_MANUAL = "manual";
    /**
     * 来源：MQ 批量任务
     */
    public static final String SOURCE_MQ = "mq";

    /**
     * 结局：已创建歌词
     */
    public static final String OUTCOME_CREATED = "created";
    /**
     * 结局：LRCLIB 标记纯音乐
     */
    public static final String OUTCOME_INSTRUMENTAL = "instrumental";
    /**
     * 结局：LRCLIB 暂无该曲目（其后台会补录，可重试）
     */
    public static final String OUTCOME_NOT_FOUND = "not_found";
    /**
     * 结局：业务性跳过（音乐不存在/已有歌词，人工数据无条件优先）
     */
    public static final String OUTCOME_SKIPPED = "skipped";
    /**
     * 结局：失败（网络异常等，可重投）
     */
    public static final String OUTCOME_FAILED = "failed";

    @TableId(type = IdType.AUTO)
    Integer id;
    Integer musicId;
    String source;
    String outcome;
    String detail;
    Date createdAt;
}
