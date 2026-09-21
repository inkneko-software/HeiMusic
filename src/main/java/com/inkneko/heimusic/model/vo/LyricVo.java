package com.inkneko.heimusic.model.vo;

import com.inkneko.heimusic.model.entity.Lyric;
import lombok.Data;

import java.util.Date;

@Data
public class LyricVo {
    Integer lyricId;
    Integer musicId;
    String content;
    String locale;
    String format;
    Boolean isDefault;
    Date createdAt;
    Date updatedAt;

    public LyricVo(Lyric lyric, Integer defaultLyricId) {
        this.lyricId = lyric.getLyricId();
        this.musicId = lyric.getMusicId();
        this.content = lyric.getContent();
        this.locale = lyric.getLocale();
        this.format = lyric.getFormat();
        //defaultLyricId可能为null（未指定默认），equals入参取lyricId侧防NPE
        this.isDefault = lyric.getLyricId().equals(defaultLyricId);
        this.createdAt = lyric.getCreatedAt();
        this.updatedAt = lyric.getUpdatedAt();
    }
}
