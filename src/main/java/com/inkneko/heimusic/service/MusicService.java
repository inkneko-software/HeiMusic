package com.inkneko.heimusic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.inkneko.heimusic.model.entity.Music;
import com.inkneko.heimusic.model.entity.MusicArtist;
import com.inkneko.heimusic.model.entity.MusicResource;

import java.util.List;

public interface MusicService extends IService<Music> {

    /**
     * 保存音乐资源
     *
     * @param musicResource
     */
    void saveMusicResource(MusicResource musicResource);

    /**
     * 查询音乐资源
     *
     * @param resourceId
     * @return
     */
    MusicResource getMusicResource(Integer resourceId);

    /**
     * 删除音乐资源
     *
     * @param resourceId
     */
    void removeMusicResource(Integer resourceId);

    /**
     * 更新音乐资源
     * @param musicResource
     */
    void updateMusicResource(MusicResource musicResource);

    /**
     * 查询某音乐的资源列表
     *
     * @param musicId 音乐id
     * @return 该音乐下所有的资源
     */
    List<MusicResource> getMusicResources(Integer musicId);

    /**
     * 查询音乐的艺术家列表
     *
     * @param musicId 音乐id
     * @return 艺术家列表
     */
    List<MusicArtist> getMusicArtists(Integer musicId);

    /**
     * 向音乐添加艺术家
     *
     * @param musicId 音乐id
     * @param artistIds 艺术家id
     */
    void addMusicArtists(Integer musicId, List<Integer> artistIds);

    /**
     * 向音乐添加艺术家，若艺术家名称不存在，则自动创建
     *
     * @param musicId 音乐id
     * @param artistNames 艺术家名称列表
     */
    void addMusicArtistsWithName(Integer musicId, List<String> artistNames);

    /**
     * 删除音乐的艺术家
     *
     * @param musicId
     * @param artistIds
     */
    void removeMusicArtists(Integer musicId, List<Integer> artistIds);

    /**
     * 查询是否为用户收藏音乐
     * @param userId 用户id
     * @param musicId 音乐id
     * @return 是否为收藏音乐
     */
    boolean isFavorite(Integer userId, Integer musicId);
}
