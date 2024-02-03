package com.inkneko.heimusic.model.vo;

import com.inkneko.heimusic.config.MinIOConfig;
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

    public AlbumVo(Album album, List<ArtistVo> artistList, Long musicNum, MinIOConfig minIOConfig) {
        this.albumId = album.getAlbumId();
        this.title = album.getTitle();
        this.translateTitle = album.getTranslateTitle();
        this.backCoverUrl = album.getBackCoverUrl();
        this.artistList = artistList;
        this.musicNum = musicNum;

        if (album.getFrontCoverObjectKey() != null) {
            //若不使用CDN，则url为 endpoint + bucket + objectKey
            if (minIOConfig.getCdn().isEmpty()) {
                this.frontCoverUrl = String.format("%s/%s/%s", minIOConfig.getEndpoint(), album.getFrontCoverBucket(), album.getFrontCoverObjectKey());
            } else {
                //若使用CDN，则默认该CDN指向相应的桶
                this.frontCoverUrl = String.format("%s/%s", minIOConfig.getCdn(), album.getFrontCoverObjectKey());
            }
        }
    }
}
