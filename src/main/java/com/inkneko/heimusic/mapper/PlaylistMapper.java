package com.inkneko.heimusic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inkneko.heimusic.model.entity.Playlist;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PlaylistMapper extends BaseMapper<Playlist> {
}
