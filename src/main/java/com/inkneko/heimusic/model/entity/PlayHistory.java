package com.inkneko.heimusic.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 播放历史，每用户每首歌一条（方案B），复合主键不设@TableId
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayHistory implements Serializable {
    Integer userId;
    Integer musicId;
    Integer playCount;
    Date lastPlayedAt;
    Date createdAt;
    Date updatedAt;
}
