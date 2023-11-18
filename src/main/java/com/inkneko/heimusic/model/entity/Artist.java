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
public class Artist {
    @TableId(type = IdType.AUTO)
    Integer artistId;
    String name;
    String translateName;
    String avatarUrl;
    Date birth;
    Date createdAt;
    Date updatedAt;
}
