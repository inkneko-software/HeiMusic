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
public class Playlist {
    @TableId(type = IdType.AUTO)
    private Integer playlistId;
    private Integer userId;
    private String description;
    private Integer sequenceNumber;
    private String coverUrl;
    private Integer playCount;
    private Date createdAt;
    private Date updatedAt;
}
