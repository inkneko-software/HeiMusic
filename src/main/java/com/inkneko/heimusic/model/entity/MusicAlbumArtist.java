package com.inkneko.heimusic.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.List;

@Data
public class MusicAlbumArtist {
    @TableId
    Integer albumId;
    List<MusicArtist> artists;
}
