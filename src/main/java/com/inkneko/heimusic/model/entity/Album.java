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
public class Album {
    @TableId(type = IdType.AUTO)
    Integer albumId;
    String title;
    String translateTitle;
    String frontCoverUrl;
    String backCoverUrl;
    Date createdAt;
    Date updatedAt;
}
