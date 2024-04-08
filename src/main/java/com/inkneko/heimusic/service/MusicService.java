package com.inkneko.heimusic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.inkneko.heimusic.model.entity.*;

import java.util.List;
import java.util.Map;

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
     *
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
     * @param musicId   音乐id
     * @param artistIds 艺术家id
     */
    void addMusicArtists(Integer musicId, List<Integer> artistIds);

    /**
     * 向音乐添加艺术家，若艺术家名称不存在，则自动创建
     *
     * @param musicId     音乐id
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
     * 设定音乐的艺术家列表
     *
     * @param musicId   音乐id
     * @param artistIds 欲指定的艺术家id
     */
    void updateMusicArtists(Integer musicId, List<Integer> artistIds);

    /**
     * 设定音乐的艺术家列表
     *
     * @param musicId     音乐id
     * @param artistNames 欲指定的艺术家id
     */
    void updateMusicArtistsWithName(Integer musicId, List<String> artistNames);

    /**
     * 查询是否为用户收藏音乐
     *
     * @param userId  用户id
     * @param musicId 音乐id
     * @return 是否为收藏音乐
     */
    boolean isFavorite(Integer userId, Integer musicId);

    /**
     * 查询用户收藏音乐列表
     *
     * @param userId 用户id
     * @return 收藏音乐列表
     */
    List<MusicFavorite> getUserMusicFavoriteList(Integer userId);

    /**
     * 添加某用户收藏的音乐
     *
     * @param userId  用户id
     * @param musicId 音乐id
     */
    void addUserMusicFavorite(Integer userId, Integer musicId);

    /**
     * 移除某用户收藏的音乐
     *
     * @param userId  用户id
     * @param musicId 音乐id
     */
    void removeUserMusicFavorite(Integer userId, Integer musicId);

    /**
     * 查询包含指定艺术家的音乐
     *
     * @param artistIds 艺术家列表
     * @return 满足条件的音乐，以及音乐对应的艺术家列表
     */
    Map<Music, List<MusicArtist>> getByContainsArtist(List<Integer> artistIds);

    /**
     * 创建歌单
     *
     * @param playlist 歌单信息
     */
    void addPlaylist(Playlist playlist);

    /**
     * 删除歌单。
     *
     * @param playlistId 指定的歌单ID
     * @param userId     提交删除请求的用户id
     */
    void removePlaylist(Integer playlistId, Integer userId);

    /**
     * 更新歌单信息
     *
     * @param playlist 歌单信息
     * @param userId   提交更新请求的用户id
     */
    void updatePlaylist(Playlist playlist, Integer userId);

    /**
     * 查询歌单信息
     *
     * @param playlistId 歌单id
     * @return 歌单信息
     */
    Playlist getPlaylist(Integer playlistId);

    /**
     * 添加歌单音乐
     *
     * @param playlistId  歌单id
     * @param musicIdList 音乐id列表
     * @param userId      提交请求的用户id
     */
    void addPlaylistMusic(Integer playlistId, List<Integer> musicIdList, Integer userId);

    /**
     * 移除歌单中的音乐
     *
     * @param playlistId  歌单id
     * @param musicIdList 音乐id列表
     * @param userId      发起者用户id
     */
    void removePlaylistMusic(Integer playlistId, List<Integer> musicIdList, Integer userId);

    /**
     * 查询歌单下的音乐
     *
     * @param playlistId 歌单id
     * @return 指定歌单的音乐列表
     */
    List<Music> getPlaylistMusicList(Integer playlistId);

    /**
     * 查询用户创建的歌单列表
     *
     * @param userId 用户id
     * @return 歌单列表
     */
    List<Playlist> getUserCreatedPlaylist(Integer userId);

    /**
     * 添加歌单收藏
     *
     * @param userId     用户id
     * @param playlistId 歌单id
     */
    void addPlaylistSubscribe(Integer userId, Integer playlistId);

    /**
     * 取消歌单收藏
     *
     * @param userId     用户id
     * @param playlistId 歌单id
     */
    void removePlaylistSubscribe(Integer userId, Integer playlistId);

    /**
     * 查询收藏的歌单列表
     *
     * @param userId 用户id
     * @return 歌单列表
     */
    List<Playlist> getPlaylistSubscribed(Integer userId);

}
