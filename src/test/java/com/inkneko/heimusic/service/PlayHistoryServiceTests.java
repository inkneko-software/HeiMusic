package com.inkneko.heimusic.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.inkneko.heimusic.errorcode.PlayHistoryServiceErrorCode;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.mapper.UserDetailMapper;
import com.inkneko.heimusic.model.entity.Music;
import com.inkneko.heimusic.model.entity.PlayHistory;
import com.inkneko.heimusic.model.entity.UserDetail;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PlayHistoryService 集成测试：打点插入与计数递增、最近/最常播放排序、本人隔离与级联删除。
 */
@SpringBootTest
@Transactional
class PlayHistoryServiceTests {

    @Autowired
    PlayHistoryService playHistoryService;

    @Autowired
    MusicService musicService;

    @Autowired
    UserDetailMapper userDetailMapper;

    @Autowired
    CacheManager cacheManager;

    @AfterEach
    void clearCaches() {
        Cache cache = cacheManager.getCache("music");
        if (cache != null) {
            cache.clear();
        }
    }

    private Integer createUser() {
        UserDetail detail = new UserDetail();
        detail.setEmail(UUID.randomUUID() + "@test.example.com");
        detail.setUsername("用户" + UUID.randomUUID());
        userDetailMapper.insert(detail);
        return detail.getUserId();
    }

    private Music createMusic(String title) {
        Music music = new Music();
        music.setTitle(title);
        musicService.save(music);
        return music;
    }

    /**
     * 手动指定某条历史的最近播放时间（秒级精度下报告时间并列，排序断言需可控时间）
     */
    private void setLastPlayedAt(Integer userId, Integer musicId, Date lastPlayedAt) {
        playHistoryService.update(new LambdaUpdateWrapper<PlayHistory>()
                .eq(PlayHistory::getUserId, userId)
                .eq(PlayHistory::getMusicId, musicId)
                .set(PlayHistory::getLastPlayedAt, lastPlayedAt));
    }

    @Test
    void reportPlayInsertsThenIncrements() {
        Integer userId = createUser();
        Music music = createMusic("打点测试-" + UUID.randomUUID());

        //首次打点：插入记录，计数为1
        playHistoryService.reportPlay(userId, music.getMusicId());
        PlayHistory history = playHistoryService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PlayHistory>()
                        .eq(PlayHistory::getUserId, userId)
                        .eq(PlayHistory::getMusicId, music.getMusicId()));
        assertEquals(1, history.getPlayCount());
        assertNotNull(history.getLastPlayedAt());
        assertNotNull(history.getCreatedAt());

        //再次打点：计数递增
        playHistoryService.reportPlay(userId, music.getMusicId());
        assertEquals(2, playHistoryService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PlayHistory>()
                        .eq(PlayHistory::getUserId, userId)
                        .eq(PlayHistory::getMusicId, music.getMusicId())).getPlayCount());

        //音乐不存在报错
        ServiceException exception = assertThrows(ServiceException.class,
                () -> playHistoryService.reportPlay(userId, -1));
        assertEquals(PlayHistoryServiceErrorCode.HISTORY_MUSIC_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void recentListOrderedByLastPlayedAt() {
        Integer userId = createUser();
        Music musicA = createMusic("最近播放A-" + UUID.randomUUID());
        Music musicB = createMusic("最近播放B-" + UUID.randomUUID());
        Music musicC = createMusic("最近播放C-" + UUID.randomUUID());
        playHistoryService.reportPlay(userId, musicA.getMusicId());
        playHistoryService.reportPlay(userId, musicB.getMusicId());
        playHistoryService.reportPlay(userId, musicC.getMusicId());

        //可控时间：A最早、C其次、B最近 → 期望顺序 B, C, A
        long now = System.currentTimeMillis();
        setLastPlayedAt(userId, musicA.getMusicId(), new Date(now - 3 * 3600_000L));
        setLastPlayedAt(userId, musicC.getMusicId(), new Date(now - 2 * 3600_000L));
        setLastPlayedAt(userId, musicB.getMusicId(), new Date(now - 1 * 3600_000L));

        List<PlayHistory> recentList = playHistoryService.getRecentList(userId, 100);
        assertEquals(3, recentList.size());
        assertEquals(musicB.getMusicId(), recentList.get(0).getMusicId());
        assertEquals(musicC.getMusicId(), recentList.get(1).getMusicId());
        assertEquals(musicA.getMusicId(), recentList.get(2).getMusicId());

        //limit生效
        assertEquals(2, playHistoryService.getRecentList(userId, 2).size());
    }

    @Test
    void mostPlayedListOrderedByCount() {
        Integer userId = createUser();
        Music musicA = createMusic("最常播放A-" + UUID.randomUUID());
        Music musicB = createMusic("最常播放B-" + UUID.randomUUID());
        Music musicC = createMusic("最常播放C-" + UUID.randomUUID());
        Music musicD = createMusic("最常播放D-" + UUID.randomUUID());
        //A播3次、D播2次、C播2次、B播1次
        for (int i = 0; i < 3; i++) playHistoryService.reportPlay(userId, musicA.getMusicId());
        for (int i = 0; i < 2; i++) playHistoryService.reportPlay(userId, musicC.getMusicId());
        for (int i = 0; i < 2; i++) playHistoryService.reportPlay(userId, musicD.getMusicId());
        playHistoryService.reportPlay(userId, musicB.getMusicId());

        //C与D同为2次：C的last_played_at早于D → 期望顺序 A(3), D(2), C(2), B(1)
        long now = System.currentTimeMillis();
        setLastPlayedAt(userId, musicC.getMusicId(), new Date(now - 2 * 3600_000L));
        setLastPlayedAt(userId, musicD.getMusicId(), new Date(now - 1 * 3600_000L));

        List<PlayHistory> mostPlayedList = playHistoryService.getMostPlayedList(userId, 100);
        assertEquals(4, mostPlayedList.size());
        assertEquals(musicA.getMusicId(), mostPlayedList.get(0).getMusicId());
        assertEquals(musicD.getMusicId(), mostPlayedList.get(1).getMusicId());
        assertEquals(musicC.getMusicId(), mostPlayedList.get(2).getMusicId());
        assertEquals(musicB.getMusicId(), mostPlayedList.get(3).getMusicId());
    }

    @Test
    void removeHistoryOnlyOwn() {
        Integer user1 = createUser();
        Integer user2 = createUser();
        Music music = createMusic("删除历史测试-" + UUID.randomUUID());
        playHistoryService.reportPlay(user1, music.getMusicId());
        playHistoryService.reportPlay(user2, music.getMusicId());

        //user1删除自己的历史，user2的不受影响
        playHistoryService.removeHistory(user1, music.getMusicId());
        assertNull(playHistoryService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PlayHistory>()
                        .eq(PlayHistory::getUserId, user1)
                        .eq(PlayHistory::getMusicId, music.getMusicId())));
        assertNotNull(playHistoryService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PlayHistory>()
                        .eq(PlayHistory::getUserId, user2)
                        .eq(PlayHistory::getMusicId, music.getMusicId())));

        //删除不存在的记录报错
        ServiceException exception = assertThrows(ServiceException.class,
                () -> playHistoryService.removeHistory(user1, music.getMusicId()));
        assertEquals(PlayHistoryServiceErrorCode.HISTORY_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void removeByMusicIdCascades() {
        Integer user1 = createUser();
        Integer user2 = createUser();
        Music music1 = createMusic("级联删除1-" + UUID.randomUUID());
        Music music2 = createMusic("级联删除2-" + UUID.randomUUID());
        playHistoryService.reportPlay(user1, music1.getMusicId());
        playHistoryService.reportPlay(user2, music1.getMusicId());
        playHistoryService.reportPlay(user1, music2.getMusicId());

        playHistoryService.removeByMusicId(music1.getMusicId());

        //music1的全部历史被清除，music2不受影响
        assertTrue(playHistoryService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PlayHistory>()
                        .eq(PlayHistory::getMusicId, music1.getMusicId())).isEmpty());
        assertEquals(1, playHistoryService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PlayHistory>()
                        .eq(PlayHistory::getMusicId, music2.getMusicId())).size());
    }
}
