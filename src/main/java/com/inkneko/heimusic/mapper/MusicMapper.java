package com.inkneko.heimusic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inkneko.heimusic.model.entity.Music;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MusicMapper extends BaseMapper<Music> {
}
