package com.inkneko.heimusic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inkneko.heimusic.mapper.AlbumArtistMapper;
import com.inkneko.heimusic.mapper.AlbumMapper;
import com.inkneko.heimusic.mapper.AlbumMusicMapper;
import com.inkneko.heimusic.model.entity.Album;
import com.inkneko.heimusic.model.entity.AlbumArtist;
import com.inkneko.heimusic.model.entity.AlbumMusic;
import com.inkneko.heimusic.model.entity.Artist;
import com.inkneko.heimusic.service.AlbumService;
import com.inkneko.heimusic.service.MusicService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

@Service
public class AlbumServiceImpl extends ServiceImpl<AlbumMapper, Album> implements AlbumService {
    AlbumArtistMapper albumArtistMapper;
    AlbumMusicMapper albumMusicMapper;
    MusicService musicService;
    ArtistServiceImpl artistService;

    public AlbumServiceImpl(AlbumArtistMapper albumArtistMapper, AlbumMusicMapper albumMusicMapper, MusicService musicService, ArtistServiceImpl artistService) {
        this.albumArtistMapper = albumArtistMapper;
        this.albumMusicMapper = albumMusicMapper;
        this.musicService = musicService;
        this.artistService = artistService;
    }

    /**
     * 向指定专辑添加音乐信息
     *
     * @param albumId  指定专辑的id
     * @param musicIds 要添加音乐的id
     */
    @Override
    @CacheEvict(cacheNames = {"albumMusicList", "albumMusicNum"}, key = "#albumId")
    public void addAlbumMusic(Integer albumId, List<Integer> musicIds) {
        for (Integer musicId : musicIds) {
            albumMusicMapper.insert(new AlbumMusic(albumId, musicId));
        }
    }

    /**
     * 查询音乐所属的专辑
     *
     * @param musicId 音乐ID
     */
    @Override
    public Album getAlbumMusicByMusicId(Integer musicId) {
        AlbumMusic albumMusic = albumMusicMapper.selectOne(new LambdaQueryWrapper<AlbumMusic>().eq(AlbumMusic::getMusicId, musicId));
        if (albumMusic == null) {
            return null;
        }
        return getById(albumMusic.getAlbumId());
    }

    /**
     * 删除指定专辑的音乐
     *
     * @param albumId  专辑id
     * @param musicIds 音乐id
     */
    @Override
    @CacheEvict(cacheNames = {"albumMusicList", "albumMusicNum"}, key = "#albumId")
    public void removeAlbumMusic(Integer albumId, List<Integer> musicIds) {
        for (Integer musicId : musicIds) {
            albumMusicMapper.delete(new LambdaQueryWrapper<AlbumMusic>().eq(AlbumMusic::getAlbumId, albumId).eq(AlbumMusic::getMusicId, musicId));
        }
    }

    /**
     * 查询专辑的音乐列表
     *
     * @param albumId 专辑id
     * @return 该专辑的音乐列表
     */
    @Override
    @Cacheable(cacheNames = "albumMusicList", key = "#albumId")
    public List<AlbumMusic> getAlbumMusicList(Integer albumId) {
        return albumMusicMapper.selectList(new LambdaQueryWrapper<AlbumMusic>().eq(AlbumMusic::getAlbumId, albumId));
    }

    /**
     * 向指定专辑添加艺术家信息
     *
     * @param albumId   专辑
     * @param artistIds 艺术家ID
     */
    @Override
    @CacheEvict(cacheNames = "albumArtistList", key = "#albumId")
    public void addAlbumArtist(Integer albumId, List<Integer> artistIds) {
        for (Integer artistId : artistIds) {
            albumArtistMapper.insert(new AlbumArtist(albumId, artistId));
        }
    }

    /**
     * 向指定专辑添加艺术家信息
     *
     * @param albumId     专辑ID
     * @param artistNames 艺术家名称列表
     */
    @Override
    @CacheEvict(cacheNames = "albumArtistList", key = "#albumId")
    public void addAlbumArtistWithNames(Integer albumId, List<String> artistNames) {
        artistNames.forEach(artistName -> {
            Artist artist = artistService.getOne(new LambdaQueryWrapper<Artist>().eq(Artist::getName, artistName));
            if (artist == null) {
                artist = new Artist();
                artist.setName(artistName);
                artistService.save(artist);
            }
            try {
                albumArtistMapper.insert(new AlbumArtist(albumId, artist.getArtistId()));
            } catch (DataIntegrityViolationException ignored) {
                //忽略重复添加
            }
        });
    }

    /**
     * 更新专辑的艺术家信息
     *
     * @param albumId      专辑
     * @param newArtistIds 新的艺术家信息
     */
    @Override
    @CacheEvict(cacheNames = "albumArtistList", key = "#albumId")
    public void updateAlbumArtist(Integer albumId, List<Integer> newArtistIds) {
        albumArtistMapper.delete(new LambdaQueryWrapper<AlbumArtist>().eq(AlbumArtist::getAlbumId, albumId));
        addAlbumArtist(albumId, newArtistIds);
    }

    /**
     * 删除指定专辑的艺术家信息
     *
     * @param albumId   专辑
     * @param artistIds 艺术家
     */
    @Override
    @CacheEvict(cacheNames = "albumArtistList", key = "#albumId")
    public void removeAlbumArtist(Integer albumId, List<Integer> artistIds) {
        for (Integer artistId : artistIds) {
            albumArtistMapper.delete(new LambdaQueryWrapper<AlbumArtist>().eq(AlbumArtist::getAlbumId, albumId).eq(AlbumArtist::getArtistId, artistId));
        }
    }

    /**
     * 查询指定专辑的艺术家
     *
     * @param albumId 专辑id
     * @return 艺术家列表
     */
    @Override
    @Cacheable(cacheNames = "albumArtistList", key = "#albumId")
    public List<AlbumArtist> getAlbumArtist(Integer albumId) {
        return albumArtistMapper.selectList(new LambdaQueryWrapper<AlbumArtist>().eq(AlbumArtist::getAlbumId, albumId));
    }

    /**
     * 获取最近上传
     *
     * @param current 当前页数
     * @param size    页面大小
     * @return 专辑列表
     */
    @Override
    public List<Album> getRecentUpload(Integer current, Integer size) {
        IPage<Album> page = this.page(new Page<>(current, size), new LambdaQueryWrapper<Album>().orderByDesc(Album::getAlbumId));
        return page.getRecords();
    }

    /**
     * 查询专辑的音乐数量
     *
     * @param albumId 专辑id
     * @return 音乐数量
     */
    @Override
    @Cacheable(cacheNames = "albumMusicNum", key = "#albumId")
    public Long getAlbumMusicNum(Integer albumId) {
        return albumMusicMapper.selectCount(new LambdaQueryWrapper<AlbumMusic>().eq(AlbumMusic::getAlbumId, albumId));
    }

    /**
     * 根据 ID 查询
     *
     * @param id 主键ID
     */
    @Override
    @Cacheable(cacheNames = "album", key = "#id")
    public Album getById(Serializable id) {
        return super.getById(id);
    }

    /**
     * 根据实体(ID)删除
     *
     * @param entity 实体
     * @since 3.4.4
     */
    @Override
    @CacheEvict(cacheNames = "album", key = "#entity.albumId")
    public boolean removeById(Album entity) {
        return super.removeById(entity);
    }

    /**
     * 根据 ID 选择修改
     *
     * @param entity 实体对象
     */
    @Override
    @CacheEvict(cacheNames = "album", key = "#entity.albumId")
    public boolean updateById(Album entity) {
        return super.updateById(entity);
    }

}
