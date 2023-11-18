package com.inkneko.heimusic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inkneko.heimusic.model.entity.Artist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ArtistMapper extends BaseMapper<Artist> {

    @Select("select * from artist where name like #{name} escape '#'")
    List<Artist> searchArtistByName(String name);
}
