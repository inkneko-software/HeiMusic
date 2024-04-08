package com.inkneko.heimusic.model.vo;

import com.inkneko.heimusic.model.entity.Playlist;
import lombok.Data;

import java.util.Date;

@Data
public class PlaylistVo {
    private Integer playlistId;
    private UserBasicVo uploader;
    private String title;
    private String description;
    private Integer sequenceNumber;
    private String coverUrl;
    private Integer playCount;
    private Date createdAt;

    public PlaylistVo(Playlist playlist, UserBasicVo uploader){
        this.playlistId = playlist.getPlaylistId();
        this.title = playlist.getTitle();
        this.description = playlist.getDescription();
        this.sequenceNumber = playlist.getSequenceNumber();
        this.coverUrl = playlist.getCoverUrl();
        this.playCount = playlist.getPlayCount();
        this.createdAt = playlist.getCreatedAt();
        this.uploader = uploader;
    }
}
