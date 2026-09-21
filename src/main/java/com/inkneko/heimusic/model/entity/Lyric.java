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
public class Lyric implements Serializable {
    @TableId(type = IdType.AUTO)
    Integer lyricId;
    Integer musicId;
    String content;
    String locale;
    String format;
    Date createdAt;
    Date updatedAt;
}
