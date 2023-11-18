package com.inkneko.heimusic.model.vo;

import com.inkneko.heimusic.config.HeiMusicConfig;
import com.inkneko.heimusic.config.MinIOConfig;
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
        List<ArtistVo> artistList;
        List<MusicResourceVo> resourceList;


        public MusicVo(Music music, List<ArtistVo> artistList, List<MusicResourceVo> resourceList, String ossEndpoint) {
            musicId = music.getMusicId();
            title = music.getTitle();
            translateTitle = music.getTranslateTitle();
            resourceUrl =  String.format("%s/%s/%s", ossEndpoint, music.getBucket() ,music.getObjectKey());
            bitrate = music.getBitrate() ;
            codec = music.getCodec();
            duration = music.getDuration();
            this.artistList = artistList;
            this.resourceList = resourceList;
        }
}
