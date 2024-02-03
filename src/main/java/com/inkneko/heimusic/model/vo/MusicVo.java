package com.inkneko.heimusic.model.vo;

import com.inkneko.heimusic.config.MinIOConfig;
import com.inkneko.heimusic.model.entity.Album;
import com.inkneko.heimusic.model.entity.Music;
import lombok.Data;

import java.util.List;

@Data
public class MusicVo {
    Integer musicId;
    String title;
    String translateTitle;
    String resourceUrl;
    String bitrate;
    String codec;
    Integer duration;
    Integer albumId;
    String albumTitle;
    String albumCoverUrl;
    List<ArtistVo> artistList;
    List<MusicResourceVo> resourceList;
    Boolean isFavorite;


    public MusicVo(Music music, Album album, List<ArtistVo> artistList, List<MusicResourceVo> resourceList, MinIOConfig minIOConfig, Boolean isFavorite) {
        this.musicId = music.getMusicId();
        this.title = music.getTitle();
        this.translateTitle = music.getTranslateTitle();

        this.bitrate = music.getBitrate();
        this.codec = music.getCodec();
        this.duration = music.getDuration();
        this.artistList = artistList;
        this.resourceList = resourceList;
        this.albumId = album.getAlbumId();
        this.albumTitle = album.getTitle();
        this.isFavorite = isFavorite;

        //若不使用CDN，则url为 endpoint + bucket + objectKey
        if (minIOConfig.getCdn().isEmpty()) {
            if (album.getFrontCoverObjectKey() != null) {
                this.albumCoverUrl = String.format("%s/%s/%s", minIOConfig.getEndpoint(), album.getFrontCoverBucket(), album.getFrontCoverObjectKey());
            }
            if (music.getObjectKey() != null) {
                this.resourceUrl = String.format("%s/%s/%s", minIOConfig.getEndpoint(), music.getBucket(), music.getObjectKey());
            }
        } else {
            //若使用CDN，则默认该CDN指向相应的桶
            if (album.getFrontCoverObjectKey() != null) {
                this.albumCoverUrl = String.format("%s/%s", minIOConfig.getCdn(), album.getFrontCoverObjectKey());
            }
            if (music.getObjectKey() != null) {
                this.resourceUrl = String.format("%s/%s", minIOConfig.getCdn(), music.getObjectKey());
            }
        }
    }
}
