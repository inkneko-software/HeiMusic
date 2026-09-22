package com.inkneko.heimusic.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 歌词覆盖率统计
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LyricCoverageVo {

    /**
     * 音乐总数
     */
    long totalMusicCount;

    /**
     * 有歌词的音乐数（同音乐多语言/翻译仅计一次）
     */
    long lyricMusicCount;
}
