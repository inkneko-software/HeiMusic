package com.inkneko.heimusic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inkneko.heimusic.model.entity.Artist;
import com.inkneko.heimusic.model.entity.MusicArtist;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MusicArtistMapper extends BaseMapper<MusicArtist> {
}
