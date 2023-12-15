package com.inkneko.heimusic.model.vo;

import com.inkneko.heimusic.model.entity.MusicResource;
import lombok.Data;

@Data
public class MusicResourceVo {

    private Integer musicResourceId;
    private Integer musicId;
    private String codec;
    private String bitrate;
    private String url;

    public MusicResourceVo(MusicResource resource, String ossEndpoint){
        musicResourceId = resource.getMusicResourceId();
        musicId = resource.getMusicId();
        codec = resource.getCodec();
        bitrate = resource.getCodec();
        url = String.format("%s/%s/%s", ossEndpoint, resource.getBucket(), resource.getObjectKey());
    }
}
