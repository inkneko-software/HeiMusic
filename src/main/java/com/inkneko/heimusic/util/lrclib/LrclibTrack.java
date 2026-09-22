package com.inkneko.heimusic.util.lrclib;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;

/**
 * LRCLIB 曲目歌词记录，GET /api/get 的响应体与 GET /api/search 的数组条目
 * <p>
 * 响应中的 name（trackName 别名）与 lyricsfile（Lyricsfile YAML 原文）本期不使用，
 * 未知字段经 @JsonAnySetter 忽略，容错 LRCLIB 后续新增字段
 */
@Data
public class LrclibTrack {
    Integer id;
    String trackName;
    String artistName;
    String albumName;
    Double duration;
    Boolean instrumental;
    String plainLyrics;
    String syncedLyrics;

    @JsonAnySetter
    public void ignore(String name, Object value) {
    }
}
