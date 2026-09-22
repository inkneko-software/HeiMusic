package com.inkneko.heimusic.service;

import com.inkneko.heimusic.errorcode.LyricServiceErrorCode;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.model.entity.Lyric;
import com.inkneko.heimusic.model.entity.LyricFetchLog;
import com.inkneko.heimusic.model.entity.Music;
import com.inkneko.heimusic.model.vo.LyricFetchVo;
import com.inkneko.heimusic.service.impl.LyricServiceImpl;
import com.inkneko.heimusic.util.lrclib.LrclibClient;
import com.inkneko.heimusic.util.lrclib.LrclibTrack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LRCLIB 拉取流程集成测试：mock LrclibClient 隔离外部调用，验证结局语义、
 * "仅服务无歌词音乐"约束、纯音乐联动仅 NULL 时写入、locale 自动判定与显式覆盖、任务日志写入。
 * <p>
 * 不用 @MockitoBean（会 fork 新的 Spring 上下文，多上下文并存时 Netty 建循环连接易超时），
 * 而是直接替换共享 LyricServiceImpl 单例内的 LrclibClient 字段，测试后恢复
 */
@SpringBootTest
@Transactional
class LyricFetchServiceTests {

    @Autowired
    LyricService lyricService;

    @Autowired
    MusicService musicService;

    @Autowired
    LyricFetchLogService lyricFetchLogService;

    @Autowired
    CacheManager cacheManager;

    @Autowired
    DataSource dataSource;

    @Autowired
    org.redisson.api.RedissonClient redissonClient;

    LrclibClient originalLrclibClient;
    LrclibClient lrclibClient;

    @BeforeEach
    void mockLrclibClient() {
        LyricServiceImpl impl = (LyricServiceImpl) lyricService;
        originalLrclibClient = (LrclibClient) ReflectionTestUtils.getField(impl, "lrclibClient");
        lrclibClient = mock(LrclibClient.class);
        ReflectionTestUtils.setField(impl, "lrclibClient", lrclibClient);
    }

    @AfterEach
    void clearCachesAndRestore() {
        ReflectionTestUtils.setField((LyricServiceImpl) lyricService, "lrclibClient", originalLrclibClient);
        for (String cacheName : List.of("lyric", "lyricList", "music")) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
        executeOutsideTransaction("DELETE FROM lyric_fetch_log");
        //清理一键扫描节流键（60s TTL），避免短时间重复跑测试被误拒
        redissonClient.getBucket("heimusic:lyric:scan_throttle").delete();
    }

    /**
     * 在测试事务之外执行SQL。任务日志经 REQUIRES_NEW 独立事务提交，
     * 测试事务内的清理与查询既看不到新提交的行、清理也会被回滚，必须用独立连接
     */
    private void executeOutsideTransaction(String sql) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            connection.prepareStatement(sql).executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 在测试事务之外统计日志行数（对 REQUIRES_NEW 提交的日志立即可见）
     */
    private int countLogs(Integer musicId, String outcome) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM lyric_fetch_log WHERE music_id = ? AND outcome = ?");
            statement.setInt(1, musicId);
            statement.setString(2, outcome);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Music createMusic(String title) {
        Music music = new Music();
        music.setTitle(title);
        musicService.save(music);
        return music;
    }

    private LrclibTrack track(Boolean instrumental, String plain, String synced) {
        LrclibTrack track = new LrclibTrack();
        track.setInstrumental(instrumental);
        track.setPlainLyrics(plain);
        track.setSyncedLyrics(synced);
        return track;
    }

    @Test
    void fetchCreatedWithSyncedLyrics() {
        Music music = createMusic("拉取测试-中文-" + UUID.randomUUID());
        music.setArtist("周杰伦");
        music.setDuration("233.500000");
        musicService.updateById(music);
        String chineseLrc = "[00:17.12] 夜空中最亮的星\n[00:21.40] 能否听清";
        when(lrclibClient.getBySignature(any(), any(), any(), any()))
                .thenReturn(track(false, null, chineseLrc));

        LyricFetchVo result = lyricService.fetchFromLrclib(music.getMusicId(), null, LyricFetchLog.SOURCE_MANUAL);

        //outcome=created，歌词入库为 lrc 格式、locale 自动判定为 zh-cn
        assertEquals(LyricFetchLog.OUTCOME_CREATED, result.getOutcome());
        assertNotNull(result.getLyric());
        Lyric lyric = lyricService.getById(result.getLyric().getLyricId());
        assertEquals("lrc", lyric.getFormat());
        assertEquals("zh-cn", lyric.getLocale());
        assertEquals(chineseLrc, lyric.getContent());
        //签名参数：duration 解析为整数秒（233.5 -> 234），无专辑关联时 album 为 null
        verify(lrclibClient).getBySignature(music.getTitle(), "周杰伦", null, 234);
        //任务日志：created 结局留痕（独立连接查询，对 REQUIRES_NEW 提交的行可见）
        assertEquals(1, countLogs(music.getMusicId(), LyricFetchLog.OUTCOME_CREATED));
    }

    @Test
    void fetchCreatedWithPlainLyricsOnly() {
        Music music = createMusic("拉取测试-纯文本-" + UUID.randomUUID());
        when(lrclibClient.getBySignature(any(), any(), any(), any()))
                .thenReturn(track(false, "I feel your breath upon my neck, a soft caress as cold as death", null));

        LyricFetchVo result = lyricService.fetchFromLrclib(music.getMusicId(), null, LyricFetchLog.SOURCE_MQ);

        //无 synced 时回落纯文本 text 格式
        assertEquals(LyricFetchLog.OUTCOME_CREATED, result.getOutcome());
        Lyric lyric = lyricService.getById(result.getLyric().getLyricId());
        assertEquals("text", lyric.getFormat());
        assertEquals("en", lyric.getLocale());
    }

    @Test
    void fetchNotFoundMakesNoWrite() {
        Music music = createMusic("拉取测试-未命中-" + UUID.randomUUID());
        when(lrclibClient.getBySignature(any(), any(), any(), any())).thenReturn(null);

        LyricFetchVo result = lyricService.fetchFromLrclib(music.getMusicId(), null, LyricFetchLog.SOURCE_MANUAL);

        //not_found 为正常结局：无歌词记录、is_instrumental 保持未知，但任务日志要留下痕迹
        assertEquals(LyricFetchLog.OUTCOME_NOT_FOUND, result.getOutcome());
        assertTrue(lyricService.getMusicLyrics(music.getMusicId()).isEmpty());
        assertNull(musicService.getById(music.getMusicId()).getIsInstrumental());
        assertEquals(1, countLogs(music.getMusicId(), LyricFetchLog.OUTCOME_NOT_FOUND));
    }

    @Test
    void fetchInstrumentalUpdatesWhenUnknown() {
        Music music = createMusic("拉取测试-纯音乐-" + UUID.randomUUID());
        when(lrclibClient.getBySignature(any(), any(), any(), any()))
                .thenReturn(track(true, null, null));

        LyricFetchVo result = lyricService.fetchFromLrclib(music.getMusicId(), null, LyricFetchLog.SOURCE_MANUAL);

        //LRCLIB 标记纯音乐：outcome=instrumental，is_instrumental 由未知更新为 true，不产生歌词记录
        assertEquals(LyricFetchLog.OUTCOME_INSTRUMENTAL, result.getOutcome());
        assertTrue(musicService.getById(music.getMusicId()).getIsInstrumental());
        assertTrue(lyricService.getMusicLyrics(music.getMusicId()).isEmpty());
    }

    @Test
    void fetchInstrumentalDoesNotOverrideManualMark() {
        Music music = createMusic("拉取测试-人工标注-" + UUID.randomUUID());
        music.setIsInstrumental(false);
        musicService.updateById(music);
        when(lrclibClient.getBySignature(any(), any(), any(), any()))
                .thenReturn(track(true, null, null));

        LyricFetchVo result = lyricService.fetchFromLrclib(music.getMusicId(), null, LyricFetchLog.SOURCE_MANUAL);

        //人工标注无条件优先：已有标注（false）时不采信 LRCLIB 的 true
        assertEquals(LyricFetchLog.OUTCOME_INSTRUMENTAL, result.getOutcome());
        assertFalse(musicService.getById(music.getMusicId()).getIsInstrumental());
    }

    @Test
    void fetchRejectedWhenLyricExists() {
        Music music = createMusic("拉取测试-已有歌词-" + UUID.randomUUID());
        lyricService.addLyric(music.getMusicId(), "ja", "lrc", "[00:01.00]既存の歌詞");
        when(lrclibClient.getBySignature(any(), any(), any(), any()))
                .thenReturn(track(false, "some lyrics", null));

        //拉取只服务无歌词的音乐，人工数据无条件优先
        ServiceException exception = assertThrows(ServiceException.class,
                () -> lyricService.fetchFromLrclib(music.getMusicId(), null, LyricFetchLog.SOURCE_MANUAL));
        assertEquals(LyricServiceErrorCode.LYRIC_FETCH_ALREADY_HAS_LYRIC.getCode(), exception.getCode());
        //跳过结局也要在任务日志留痕
        assertEquals(1, countLogs(music.getMusicId(), LyricFetchLog.OUTCOME_SKIPPED));
    }

    @Test
    void fetchWithExplicitLocaleOverridesDetection() {
        Music music = createMusic("拉取测试-显式语言-" + UUID.randomUUID());
        //罗马音日文会被误判为拉丁语系，显式传 locale 跳过自动判定
        when(lrclibClient.getBySignature(any(), any(), any(), any()))
                .thenReturn(track(false, "kimi no namae wo yobu yoru ni kakenukeru", null));

        LyricFetchVo result = lyricService.fetchFromLrclib(music.getMusicId(), "ZH_CN", LyricFetchLog.SOURCE_MANUAL);

        //显式 locale 归一化后入库
        assertEquals(LyricFetchLog.OUTCOME_CREATED, result.getOutcome());
        Lyric lyric = lyricService.getById(result.getLyric().getLyricId());
        assertEquals("zh-cn", lyric.getLocale());
    }

    @Test
    void fetchMusicNotExists() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> lyricService.fetchFromLrclib(-1, null, LyricFetchLog.SOURCE_MANUAL));
        assertEquals(LyricServiceErrorCode.LYRIC_MUSIC_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void scanMissingLyricThrottledWithinWindow() {
        //首次调用通过节流窗口；测试库提交数据为空（各测试类事务回滚），投放数为0，不产生真实队列消息
        int submitted = lyricService.scanMissingLyric();
        assertEquals(0, submitted);
        //60 秒节流窗口内的重复调用被拒绝
        ServiceException exception = assertThrows(ServiceException.class,
                () -> lyricService.scanMissingLyric());
        assertEquals(LyricServiceErrorCode.LYRIC_SCAN_THROTTLED.getCode(), exception.getCode());
    }
}
