package com.inkneko.heimusic.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inkneko.heimusic.mapper.ArtistMapper;
import com.inkneko.heimusic.model.entity.Artist;
import com.inkneko.heimusic.service.ArtistService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
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
    @Cacheable(cacheNames = "artist", key = "#id")
    public Artist getById(Serializable id) {
        return super.getById(id);
    }

    /**
     * 根据实体(ID)删除
     *
     * @param entity 实体
     * @since 3.4.4
     */
    @Override
    @CacheEvict(cacheNames = "artist", key = "#entity.artistId")
    public boolean removeById(Artist entity) {
        return super.removeById(entity);
    }

    /**
     * 根据 ID 选择修改
     *
     * @param entity 实体对象
     */
    @Override
    @CachePut(cacheNames = "artist", key = "#entity.artistId")
    public boolean updateById(Artist entity) {
        return super.updateById(entity);
    }
}
