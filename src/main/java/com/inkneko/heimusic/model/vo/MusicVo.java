package com.inkneko.heimusic.model.vo;

import com.inkneko.heimusic.config.HeiMusicConfig;
import com.inkneko.heimusic.config.MinIOConfig;
import com.inkneko.heimusic.model.entity.Album;
import com.inkneko.heimusic.model.entity.Music;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

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


        public MusicVo(Music music, Album album, List<ArtistVo> artistList, List<MusicResourceVo> resourceList, String ossEndpoint, Boolean isFavorite) {
            musicId = music.getMusicId();
            title = music.getTitle();
            translateTitle = music.getTranslateTitle();
            resourceUrl =  String.format("%s/%s/%s", ossEndpoint, music.getBucket() ,music.getObjectKey());
            bitrate = music.getBitrate() ;
            codec = music.getCodec();
            duration = music.getDuration();
            this.artistList = artistList;
            this.resourceList = resourceList;
            this.albumId = album.getAlbumId();
            this.albumTitle = album.getTitle();
            this.albumCoverUrl = album.getFrontCoverUrl();
            this.isFavorite = isFavorite;
        }
}
