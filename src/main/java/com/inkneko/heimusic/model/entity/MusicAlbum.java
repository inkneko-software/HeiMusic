package com.inkneko.heimusic.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

@Data
public class MusicAlbum {
    @TableId
    Integer albumId;
    String name;
    String translateName;
    String frontCoverUrl;
    String backCoverUrl;
    Date createdAt;
    Date updatedAt;
}
