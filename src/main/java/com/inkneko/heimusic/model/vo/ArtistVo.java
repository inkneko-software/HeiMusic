package com.inkneko.heimusic.model.vo;

import com.inkneko.heimusic.model.entity.Artist;
import lombok.Data;

import java.util.Date;

@Data
public class ArtistVo {
    private Integer artistId;
    private String name;
    private String translateName;
    private String avatarUrl;
    private Date birth;

    public ArtistVo(Artist artist){
        artistId = artist.getArtistId();
        name = artist.getName();
        translateName = artist.getTranslateName();
        birth = artist.getBirth();
        avatarUrl = artist.getAvatarUrl();
    }

}
