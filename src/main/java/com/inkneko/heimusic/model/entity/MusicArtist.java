package com.inkneko.heimusic.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

@Data
public class MusicArtist {
    @TableId
    Integer artistId;
    String name;
    String translateName;
    String avatarUrl;
    Date birth;
    Date createdAt;
    Date updatedAt;
}
