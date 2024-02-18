package com.inkneko.heimusic.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Music {
    @TableId(type = IdType.AUTO)
    Integer musicId;
    String title;
    String translateTitle;
    String bucket;
    String objectKey;
    String bitrate;
    String codec;
    String duration;
    String size;
    Integer trackNumber;
    Integer trackTotal;
    Integer discNumber;
    Integer discTotal;
    String artist;
    String filePath;
    String fileHash;
    Date createdAt;
    Date updatedAt;
    Date deletedAt;
}
