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
public class PlaylistMusic {
    private Integer playlistId;
    private Integer musicId;
    private Integer sequenceNumber;
    private Date createdAt;
    private Date updatedAt;

}
