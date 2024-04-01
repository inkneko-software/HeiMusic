package com.inkneko.heimusic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.mapper.MusicArtistMapper;
import com.inkneko.heimusic.mapper.MusicFavoriteMapper;
import com.inkneko.heimusic.mapper.MusicMapper;
import com.inkneko.heimusic.mapper.MusicResourceMapper;
import com.inkneko.heimusic.model.entity.*;
import com.inkneko.heimusic.service.ArtistService;
import com.inkneko.heimusic.service.MusicService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;

@Service
public class MusicServiceImpl extends ServiceImpl<MusicMapper, Music> implements MusicService {
    MusicResourceMapper musicResourceMapper;
    MusicArtistMapper musicArtistMapper;
    ArtistService artistService;
    MusicFavoriteMapper musicFavoriteMapper;

    public MusicServiceImpl(MusicResourceMapper musicResourceMapper, MusicArtistMapper musicArtistMapper, ArtistService artistService, MusicFavoriteMapper musicFavoriteMapper) {
        this.musicResourceMapper = musicResourceMapper;
        this.musicArtistMapper = musicArtistMapper;
        this.artistService = artistService;
        this.musicFavoriteMapper = musicFavoriteMapper;
    }

    /**
     * 保存音乐资源
     *
     * @param musicResource
     * @return
     */
    @Override
    public void saveMusicResource(MusicResource musicResource) {
        musicResourceMapper.insert(musicResource);
    }

    /**
     * 查询音乐资源
     *
     * @param resourceId 资源id
     * @return 音乐资源
     */
    @Override
    @Cacheable(cacheNames = "musicResource", key = "#resourceId")
    public MusicResource getMusicResource(Integer resourceId) {
        return musicResourceMapper.selectById(resourceId);
    }

    /**
     * 删除音乐资源
     *
     * @param resourceId 资源id
     */
    @Override
    @CacheEvict(cacheNames = "musicResource", key = "#resourceId")
    public void removeMusicResource(Integer resourceId) {
        musicResourceMapper.deleteById(resourceId);
    }

    /**
     * 更新音乐资源
     *
     * @param musicResource 音乐资源
     */
    @Override
    @CacheEvict(cacheNames = "musicResource", key = "#musicResource.musicResourceId")
    public void updateMusicResource(MusicResource musicResource) {
        musicResourceMapper.updateById(musicResource);
    }

    /**
     * 查询某音乐的资源列表
     *
     * @param musicId 音乐id
     * @return 该音乐下所有的资源
     */
    @Override
    public List<MusicResource> getMusicResources(Integer musicId) {
        return musicResourceMapper.selectList(new LambdaQueryWrapper<MusicResource>().eq(MusicResource::getMusicId, musicId));
    }

    /**
     * 查询音乐的艺术家列表
     *
     * @param musicId 音乐id
     * @return 艺术家列表
     */
    @Override
    @Cacheable(cacheNames = "musicArtistList", key = "#musicId")
    public List<MusicArtist> getMusicArtists(Integer musicId) {
        return musicArtistMapper.selectList(new LambdaQueryWrapper<MusicArtist>().eq(MusicArtist::getMusicId, musicId));
    }

    /**
     * 向音乐添加艺术家
     *
     * @param musicId
     * @param artistIds
     */
    @Override
    @CacheEvict(cacheNames = "musicArtistList", key = "#musicId")
    public void addMusicArtists(Integer musicId, List<Integer> artistIds) {
        artistIds.forEach(artistId -> musicArtistMapper.insert(new MusicArtist(musicId, artistId)));
    }

    /**
     * 向音乐添加艺术家，若艺术家名称不存在，则自动创建
     *
     * @param musicId     音乐id
     * @param artistNames 艺术家名称列表
     */
    @Override
    @CacheEvict(cacheNames = "musicArtistList", key = "#musicId")
    public void addMusicArtistsWithName(Integer musicId, List<String> artistNames) {
        artistNames.forEach(artistName -> {
            Artist artist = artistService.getOne(new LambdaQueryWrapper<Artist>().eq(Artist::getName, artistName));
            if (artist == null) {
                artist = new Artist();
                artist.setName(artistName);
                artistService.save(artist);
            }
            musicArtistMapper.insert(new MusicArtist(musicId, artist.getArtistId()));
        });
    }

    /**
     * 删除音乐的艺术家
     *
     * @param musicId
     * @param artistIds
     */
    @Override
    @CacheEvict(cacheNames = "musicArtistList", key = "#musicId")
    public void removeMusicArtists(Integer musicId, List<Integer> artistIds) {
        artistIds.forEach(artistId -> musicArtistMapper.delete(new LambdaQueryWrapper<MusicArtist>().eq(MusicArtist::getArtistId, artistId).eq(MusicArtist::getMusicId, musicId)));
    }

    /**
     * 查询是否为用户收藏音乐
     *
     * @param userId  用户id
     * @param musicId 音乐id
     * @return 是否为收藏音乐
     */
    @Override
    @Cacheable(cacheNames = "musicFavorite", key = "#userId + '_' + #musicId")
    public boolean isFavorite(Integer userId, Integer musicId) {
        return musicFavoriteMapper.exists(new LambdaQueryWrapper<MusicFavorite>().eq(MusicFavorite::getUserId, userId).eq(MusicFavorite::getMusicId, musicId));
    }

    /**
     * 查询用户收藏音乐列表
     *
     * @param userId 用户id
     * @return 收藏音乐列表
     */
    @Override
    public List<MusicFavorite> getUserMusicFavoriteList(Integer userId) {
        return musicFavoriteMapper.selectList(new LambdaQueryWrapper<MusicFavorite>().eq(MusicFavorite::getUserId, userId).orderByDesc(MusicFavorite::getCreatedAt));
    }

    /**
     * 添加某用户收藏的音乐
     *
     * @param userId  用户id
     * @param musicId 音乐id
     */
    @Override
    @CacheEvict(cacheNames = "musicFavorite", key = "#userId + '_' + #musicId")
    public void addUserMusicFavorite(Integer userId, Integer musicId) {
        if (getById(musicId) == null){
            throw new ServiceException(404, "指定音乐不存在");
        }

        try {
            musicFavoriteMapper.insert(new MusicFavorite(musicId, userId, null, null));
        } catch (DuplicateKeyException ignored) {}
    }

    /**
     * 移除某用户收藏的音乐
     *
     * @param userId  用户id
     * @param musicId 音乐id
     */
    @Override
    @CacheEvict(cacheNames = "musicFavorite", key = "#userId + '_' + #musicId")
    public void removeUserMusicFavorite(Integer userId, Integer musicId) {
        musicFavoriteMapper.delete(new LambdaQueryWrapper<MusicFavorite>().eq(MusicFavorite::getUserId, userId).eq(MusicFavorite::getMusicId, musicId));
    }

    /**
     * 设定音乐的艺术家列表
     *
     * @param musicId   音乐id
     * @param artistIds 欲指定的艺术家id
     */
    @Transactional
    @Override
    @CacheEvict(cacheNames = "musicArtistList", key = "#musicId")
    public void updateMusicArtists(Integer musicId, List<Integer> artistIds) {
        Music music = getById(musicId);
        if (music == null) {
            throw new ServiceException(404, "指定音乐不存在");
        }
        musicArtistMapper.delete(new LambdaQueryWrapper<MusicArtist>().eq(MusicArtist::getMusicId, musicId));
        MusicArtist musicArtist = new MusicArtist();
        musicArtist.setMusicId(musicId);
        for (Integer artistId : artistIds) {
            musicArtist.setArtistId(artistId);
            try {
                musicArtistMapper.insert(musicArtist);
            } catch (DuplicateKeyException ignored) {
            }
        }
    }

    /**
     * 设定音乐的艺术家列表
     *
     * @param musicId     音乐id
     * @param artistNames 欲指定的艺术家id
     */
    @Override
    @CacheEvict(cacheNames = "musicArtistList", key = "#musicId")
    public void updateMusicArtistsWithName(Integer musicId, List<String> artistNames) {
        Music music = getById(musicId);
        if (music == null) {
            throw new ServiceException(404, "指定音乐不存在");
        }
        List<Integer> artistIds = new ArrayList<>();
        for (String artistName : artistNames) {
            Artist artist = artistService.getOne(new LambdaQueryWrapper<Artist>().eq(Artist::getName, artistName));
            if (artist == null) {
                artist = new Artist();
                artist.setName(artistName);
                artistService.save(artist);
            }
            artistIds.add(artist.getArtistId());
        }
        updateMusicArtists(musicId, artistIds);
    }

    /**
     * 查询包含指定艺术家的音乐
     *
     * @param artistIds 艺术家列表
     * @return 满足条件的音乐，以及音乐对应的艺术家列表
     */
    @Override
    public Map<Music, List<MusicArtist>> getByContainsArtist(List<Integer> artistIds) {
        Map<Music, List<MusicArtist>> result = new LinkedHashMap<>();
        Set<Integer> matchedMusicSet = new TreeSet<>();
        for (Integer artistId : artistIds) {
            List<MusicArtist> temp = musicArtistMapper.selectList(new LambdaQueryWrapper<MusicArtist>().eq(MusicArtist::getArtistId, artistId));
            for (MusicArtist musicArtist : temp) {
                matchedMusicSet.add(musicArtist.getMusicId());
            }
        }
        for (Integer musicId : matchedMusicSet) {
            Music music = getById(musicId);
            List<MusicArtist> musicArtists = musicArtistMapper.selectList(new LambdaQueryWrapper<MusicArtist>().eq(MusicArtist::getMusicId, musicId));
            result.put(music, musicArtists);
        }
        return result;
    }

    /**
     * 根据 ID 查询
     *
     * @param id 主键ID
     */
    @Override
    @Cacheable(cacheNames = "music", key = "#id")
    public Music getById(Serializable id) {
        return super.getById(id);
    }


    /**
     * 根据实体(ID)删除
     *
     * @param entity 实体
     * @since 3.4.4
     */
    @Override
    @CacheEvict(cacheNames = "music", key = "#entity.musicId")
    public boolean removeById(Music entity) {
        return super.removeById(entity);
    }

    /**
     * 根据 ID 选择修改
     *
     * @param entity 实体对象
     */
    @Override
    @CachePut(cacheNames = "music", key = "#entity.musicId")
    public boolean updateById(Music entity) {
        return super.updateById(entity);
    }
}
