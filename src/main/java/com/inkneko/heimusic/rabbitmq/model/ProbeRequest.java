package com.inkneko.heimusic.rabbitmq.model;

import lombok.Data;

/**
 * 请求获取某音乐的元数据信息
 */
@Data
public class ProbeRequest {
    //音乐id
    private Integer musicId;
    //文件所在桶
    private String bucket;
    //文件名称
    private String objectKey;
}
