package com.inkneko.heimusic.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LRCLIB 拉取结果
 * <p>
 * 三种 outcome 均为正常结局（code=0 返回）：not_found 非操作错误，
 * LRCLIB 会后台补录缺失曲目，之后重试可能命中。结局取值常量见 LyricFetchLog
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LyricFetchVo {

    /**
     * 拉取结局：created=已创建歌词 / instrumental=LRCLIB 标记纯音乐 / not_found=暂无该曲目
     */
    String outcome;

    /**
     * 新创建的歌词，仅 outcome=created 时非空
     */
    LyricVo lyric;
}
