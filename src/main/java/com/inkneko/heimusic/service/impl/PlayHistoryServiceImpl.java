package com.inkneko.heimusic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inkneko.heimusic.errorcode.PlayHistoryServiceErrorCode;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.mapper.PlayHistoryMapper;
import com.inkneko.heimusic.model.entity.PlayHistory;
import com.inkneko.heimusic.service.MusicService;
import com.inkneko.heimusic.service.PlayHistoryService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class PlayHistoryServiceImpl extends ServiceImpl<PlayHistoryMapper, PlayHistory> implements PlayHistoryService {

    MusicService musicService;

    public PlayHistoryServiceImpl(MusicService musicService) {
        this.musicService = musicService;
    }

    /**
     * 上报一次播放。
     * <p>
     * 与收藏的catch-ignored不同：DuplicateKeyException（并发首播竞态）必须落入递增分支补偿执行，
     * 保证每次打点都生效。
     *
     * @param userId  用户id
     * @param musicId 音乐id
     */
    @Override
    public void reportPlay(Integer userId, Integer musicId) {
        if (musicService.getById(musicId) == null) {
            throw new ServiceException(PlayHistoryServiceErrorCode.HISTORY_MUSIC_NOT_FOUND);
        }
        PlayHistory existing = getOne(new LambdaQueryWrapper<PlayHistory>()
                .eq(PlayHistory::getUserId, userId)
                .eq(PlayHistory::getMusicId, musicId));
        if (existing == null) {
            try {
                //时间戳由数据库默认值填充
                save(new PlayHistory(userId, musicId, 1, null, null, null));
            } catch (DuplicateKeyException e) {
                incrementPlay(userId, musicId);
            }
            return;
        }
        incrementPlay(userId, musicId);
    }

    /**
     * 播放计数+1并刷新最近播放时间
     *
     * @param userId  用户id
     * @param musicId 音乐id
     */
    private void incrementPlay(Integer userId, Integer musicId) {
        update(new LambdaUpdateWrapper<PlayHistory>()
                .eq(PlayHistory::getUserId, userId)
                .eq(PlayHistory::getMusicId, musicId)
                .setSql("play_count = play_count + 1")
                .set(PlayHistory::getLastPlayedAt, new Date()));
    }

    /**
     * 查询用户最近播放列表
     *
     * @param userId 用户id
     * @param limit  最大返回条数
     * @return 播放历史列表，按最近播放时间倒序
     */
    @Override
    public List<PlayHistory> getRecentList(Integer userId, int limit) {
        return page(new Page<>(1, limit), new LambdaQueryWrapper<PlayHistory>()
                .eq(PlayHistory::getUserId, userId)
                .orderByDesc(PlayHistory::getLastPlayedAt)
                //秒级时间戳并列时兜底稳定排序
                .orderByDesc(PlayHistory::getMusicId))
                .getRecords();
    }

    /**
     * 查询用户最常播放列表
     *
     * @param userId 用户id
     * @param limit  最大返回条数
     * @return 播放历史列表，按播放次数倒序，并列时按最近播放时间倒序
     */
    @Override
    public List<PlayHistory> getMostPlayedList(Integer userId, int limit) {
        return page(new Page<>(1, limit), new LambdaQueryWrapper<PlayHistory>()
                .eq(PlayHistory::getUserId, userId)
                .orderByDesc(PlayHistory::getPlayCount)
                .orderByDesc(PlayHistory::getLastPlayedAt))
                .getRecords();
    }

    /**
     * 删除用户的单条播放历史
     *
     * @param userId  用户id
     * @param musicId 音乐id
     */
    @Override
    public void removeHistory(Integer userId, Integer musicId) {
        boolean deleted = remove(new LambdaQueryWrapper<PlayHistory>()
                .eq(PlayHistory::getUserId, userId)
                .eq(PlayHistory::getMusicId, musicId));
        if (!deleted) {
            throw new ServiceException(PlayHistoryServiceErrorCode.HISTORY_NOT_FOUND);
        }
    }

    /**
     * 删除某音乐的全部播放历史（删除音乐时级联调用）
     *
     * @param musicId 音乐id
     */
    @Override
    public void removeByMusicId(Integer musicId) {
        remove(new LambdaQueryWrapper<PlayHistory>().eq(PlayHistory::getMusicId, musicId));
    }
}
