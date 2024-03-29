package com.inkneko.heimusic.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Album implements Serializable {
    @TableId(type = IdType.AUTO)
    Integer albumId;
    String title;
    String translateTitle;
    String frontCoverBucket;
    String frontCoverObjectKey;
    String frontCoverFilePath;
    Integer largeTrackNums;
    String albumArtist;
    Date createdAt;
    Date updatedAt;

    public Album(String title) {
        this.title = title;
    }
}
