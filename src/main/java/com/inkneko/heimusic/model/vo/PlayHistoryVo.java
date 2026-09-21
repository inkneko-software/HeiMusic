package com.inkneko.heimusic.model.vo;

import com.inkneko.heimusic.model.entity.PlayHistory;
import lombok.Data;

import java.util.Date;

@Data
public class PlayHistoryVo {
    MusicVo music;
    Integer playCount;
    Date lastPlayedAt;

    public PlayHistoryVo(MusicVo music, PlayHistory history) {
        this.music = music;
        this.playCount = history.getPlayCount();
        this.lastPlayedAt = history.getLastPlayedAt();
    }
}
