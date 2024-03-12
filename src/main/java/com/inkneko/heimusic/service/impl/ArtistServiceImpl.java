package com.inkneko.heimusic.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inkneko.heimusic.mapper.ArtistMapper;
import com.inkneko.heimusic.model.entity.Artist;
import com.inkneko.heimusic.service.ArtistService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@Service
public class ArtistServiceImpl extends ServiceImpl<ArtistMapper, Artist> implements ArtistService {
//    MusicArtistMapper musicArtistMapper;
//
//    public MusicArtistServiceImpl(MusicArtistMapper musicArtistMapper) {
//        this.musicArtistMapper = musicArtistMapper;
//    }
//
//    public List<MusicArtist> searchAritstByName(String name) {
//        name = name.replaceAll("#", "##");
//        name = name.replaceAll("%", "%%");
//        return musicArtistMapper.searchArtistByName(name);
//    }


    /**
     * 根据 ID 查询
     *
     * @param id 主键ID
     */
    @Override
    @Cacheable("getArtistById")
    public Artist getById(Serializable id) {
        return super.getById(id);
    }
}
