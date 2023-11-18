package com.inkneko.heimusic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inkneko.heimusic.config.RabbitMQConfig;
import com.inkneko.heimusic.mapper.MusicArtistMapper;
import com.inkneko.heimusic.mapper.MusicResourceMapper;
import com.inkneko.heimusic.mapper.MusiclMapper;
import com.inkneko.heimusic.model.entity.Artist;
import com.inkneko.heimusic.model.entity.Music;
import com.inkneko.heimusic.model.entity.MusicArtist;
import com.inkneko.heimusic.model.entity.MusicResource;
import com.inkneko.heimusic.model.vo.ArtistVo;
import com.inkneko.heimusic.model.vo.MusicResourceVo;
import com.inkneko.heimusic.model.vo.MusicVo;
import com.inkneko.heimusic.rabbitmq.model.ProbeRequest;
import com.inkneko.heimusic.service.ArtistService;
import com.inkneko.heimusic.service.MusicService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;

@Service
public class MusicServiceImpl extends ServiceImpl<MusiclMapper, Music> implements MusicService {
    MusicResourceMapper musicResourceMapper;
    MusicArtistMapper musicArtistMapper;
    ArtistService artistService;

    public MusicServiceImpl(MusicResourceMapper musicResourceMapper, MusicArtistMapper musicArtistMapper, ArtistService artistService) {
        this.musicResourceMapper = musicResourceMapper;
        this.musicArtistMapper = musicArtistMapper;
        this.artistService = artistService;
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
     * @param resourceId
     * @return
     */
    @Override
    public MusicResource getMusicResource(Integer resourceId) {
        return musicResourceMapper.selectById(resourceId);
    }

    /**
     * 删除音乐资源
     *
     * @param resourceId
     */
    @Override
    public void removeMusicResource(Integer resourceId) {
        musicResourceMapper.deleteById(resourceId);
    }

    /**
     * 更新音乐资源
     *
     * @param musicResource
     */
    @Override
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
    public void addMusicArtistsWithName(Integer musicId, List<String> artistNames) {
        artistNames.forEach(artistName -> {
            Artist artist = artistService.getOne(new LambdaQueryWrapper<Artist>().eq(Artist::getName, artistName));
            if (artist == null){
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
    public void removeMusicArtists(Integer musicId, List<Integer> artistIds) {
        artistIds.forEach(artistId -> musicArtistMapper.deleteById(new MusicArtist(musicId, artistId)));
    }
}
