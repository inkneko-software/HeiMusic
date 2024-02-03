package com.inkneko.heimusic.model.vo;

import com.inkneko.heimusic.config.MinIOConfig;
import com.inkneko.heimusic.model.entity.MusicResource;
import lombok.Data;

@Data
public class MusicResourceVo {

    private Integer musicResourceId;
    private Integer musicId;
    private String codec;
    private String bitrate;
    private String url;

    public MusicResourceVo(MusicResource resource, MinIOConfig minIOConfig) {
        musicResourceId = resource.getMusicResourceId();
        musicId = resource.getMusicId();
        codec = resource.getCodec();
        bitrate = resource.getCodec();
        if (resource.getObjectKey() != null) {
            //若不使用CDN，则url为 endpoint + bucket + objectKey
            if (minIOConfig.getCdn().isEmpty()) {
                url = String.format("%s/%s/%s", minIOConfig.getEndpoint(), resource.getBucket(), resource.getObjectKey());
            } else {
                //若使用CDN，则默认该CDN指向相应的桶
                url = String.format("%s/%s", minIOConfig.getCdn(), resource.getObjectKey());
            }
        } else {
            url = null;
        }
    }
}
