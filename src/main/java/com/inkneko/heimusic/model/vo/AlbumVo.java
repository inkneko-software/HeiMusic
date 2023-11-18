package com.inkneko.heimusic.model.vo;

import com.inkneko.heimusic.model.entity.Album;
import lombok.Data;

import java.util.List;


@Data
public class AlbumVo {
    Integer albumId;
    String title;
    String translateTitle;
    String frontCoverUrl;
    String backCoverUrl;
    Long musicNum;
    List<ArtistVo> artistList;

    public AlbumVo(Album album, List<ArtistVo> artistList, Long musicNum){
        albumId = album.getAlbumId();
        title = album.getTitle();
        translateTitle = album.getTranslateTitle();
        frontCoverUrl = album.getFrontCoverUrl();
        backCoverUrl = album.getBackCoverUrl();
        this.artistList = artistList;
        this.musicNum = musicNum;
    }
}
