package com.inkneko.heimusic.rabbitmq.model;

import lombok.Data;

/**
 * 请求为某音乐从 LRCLIB 拉取歌词
 */
@Data
public class LyricFetchRequest {
    //音乐id
    private Integer musicId;
}
