package com.inkneko.heimusic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.inkneko.heimusic.model.entity.PlayHistory;

import java.util.List;

public interface PlayHistoryService extends IService<PlayHistory> {

    /**
     * 上报一次播放：首次插入（play_count=1），之后计数+1并刷新最近播放时间
     *
     * @param userId  用户id
     * @param musicId 音乐id
     */
    void reportPlay(Integer userId, Integer musicId);

    /**
     * 查询用户最近播放列表，按最近播放时间倒序
     *
     * @param userId 用户id
     * @param limit  最大返回条数（调用方负责合理上限）
     * @return 播放历史列表
     */
    List<PlayHistory> getRecentList(Integer userId, int limit);

    /**
     * 查询用户最常播放列表，按播放次数倒序，次数并列时按最近播放时间倒序
     *
     * @param userId 用户id
     * @param limit  最大返回条数（调用方负责合理上限）
     * @return 播放历史列表
     */
    List<PlayHistory> getMostPlayedList(Integer userId, int limit);

    /**
     * 删除用户的单条播放历史
     *
     * @param userId  用户id
     * @param musicId 音乐id
     */
    void removeHistory(Integer userId, Integer musicId);

    /**
     * 删除某音乐的全部播放历史（删除音乐时级联调用）
     *
     * @param musicId 音乐id
     */
    void removeByMusicId(Integer musicId);
}
