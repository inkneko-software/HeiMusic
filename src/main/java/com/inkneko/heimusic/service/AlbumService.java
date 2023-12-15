package com.inkneko.heimusic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.inkneko.heimusic.model.entity.*;
import com.inkneko.heimusic.model.vo.AlbumVo;

import java.util.List;

public interface AlbumService extends IService<Album> {

    /**
     * 向指定专辑添加音乐信息
     * @param albumId 指定专辑的id
     * @param musicIds 要添加音乐的id
     */
    void addAlbumMusic(Integer albumId, List<Integer> musicIds);

    /**
     * 删除指定专辑的音乐
     * @param albumId 专辑id
     * @param musicIds 音乐id
     */
    void removeAlbumMusic(Integer albumId, List<Integer> musicIds);

    /**
     * 查询专辑的音乐列表
     * @param albumId 专辑id
     * @return 该专辑的音乐列表
     */
    List<AlbumMusic> getAlbumMusicList(Integer albumId);

    /**
     * 查询专辑的音乐数量
     * @param albumId 专辑id
     * @return 音乐数量
     */
    Long getAlbumMusicNum(Integer albumId);

    /**
     * 向指定专辑添加艺术家信息
     * @param albumId 专辑
     * @param artistIds 艺术家ID
     */
    void addAlbumArtist(Integer albumId, List<Integer> artistIds);

    /**
     * 更新专辑的艺术家信息
     * @param albumId 专辑
     * @param newArtistIds 新的艺术家信息
     */
    void updateAlbumArtist(Integer albumId, List<Integer> newArtistIds);

    /**
     * 删除指定专辑的艺术家信息
     * @param albumId 专辑
     * @param artistIds 艺术家
     */
    void removeAlbumArtist(Integer albumId, List<Integer> artistIds);

    /**
     * 查询指定专辑的艺术家
     *
     * @param albumId 专辑id
     * @return 艺术家列表
     */
    List<AlbumArtist> getAlbumArtist(Integer albumId);

    /**
     * 获取最近上传
     *
     * @param current 当前页数
     * @param size 页面大小
     * @return 专辑列表
     */
    List<Album> getRecentUpload(Integer current, Integer size);
}
