package com.inkneko.heimusic.service;

import com.inkneko.heimusic.errorcode.LyricServiceErrorCode;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.model.entity.Lyric;
import com.inkneko.heimusic.model.entity.Music;
import com.inkneko.heimusic.model.vo.LyricCoverageVo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LyricService 集成测试：歌词增删改查、同语言唯一约束、默认歌词引用清理、纯音乐标记共存。
 */
@SpringBootTest
@Transactional
class LyricServiceTests {

    @Autowired
    LyricService lyricService;

    @Autowired
    MusicService musicService;

    @Autowired
    CacheManager cacheManager;

    @AfterEach
    void clearCaches() {
        for (String cacheName : List.of("lyric", "lyricList", "music")) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    private Music createMusic(String title) {
        Music music = new Music();
        music.setTitle(title);
        musicService.save(music);
        return music;
    }

    private Lyric createLyric(Music music, String locale) {
        return lyricService.addLyric(music.getMusicId(), locale, "lrc", "歌词内容-" + UUID.randomUUID());
    }

    @Test
    void addLyricAndQueryByLocale() {
        Music music = createMusic("歌词增查测试-" + UUID.randomUUID());

        Lyric lyric = lyricService.addLyric(music.getMusicId(), "zh-CN", "lrc", "[00:01.00]测试歌词");
        assertNotNull(lyric.getLyricId());

        //locale归一化：zh-CN入库为zh-cn
        Lyric fetched = lyricService.getMusicLyricByLocale(music.getMusicId(), "ZH_CN");
        assertNotNull(fetched);
        assertEquals("zh-cn", fetched.getLocale());
        assertEquals("[00:01.00]测试歌词", fetched.getContent());
        assertEquals("lrc", fetched.getFormat());

        //音乐不存在时报错
        ServiceException exception = assertThrows(ServiceException.class,
                () -> lyricService.addLyric(-1, "zh-cn", "lrc", "内容"));
        assertEquals(LyricServiceErrorCode.LYRIC_MUSIC_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void addDuplicatedLocaleRejected() {
        Music music = createMusic("重复语言测试-" + UUID.randomUUID());
        createLyric(music, "ja");

        //同音乐同语言二次添加被唯一键拦截
        ServiceException exception = assertThrows(ServiceException.class,
                () -> lyricService.addLyric(music.getMusicId(), "JA", "text", "重复内容"));
        assertEquals(LyricServiceErrorCode.LYRIC_ALREADY_EXISTS.getCode(), exception.getCode());

        //换语言可以成功（翻译=另一条locale记录）
        Lyric translation = createLyric(music, "zh-cn");
        assertNotNull(translation.getLyricId());
    }

    @Test
    void updateLyricContent() {
        Music music = createMusic("歌词更新测试-" + UUID.randomUUID());
        Lyric lyric = createLyric(music, "en");

        Lyric updated = lyricService.updateLyric(lyric.getLyricId(), "text", "更新后的歌词");
        assertEquals("更新后的歌词", updated.getContent());
        assertEquals("text", updated.getFormat());

        //更新后查询（缓存已驱逐）应反映新值
        assertEquals("更新后的歌词", lyricService.getById(lyric.getLyricId()).getContent());

        //歌词不存在时报错
        ServiceException notFound = assertThrows(ServiceException.class,
                () -> lyricService.updateLyric(-1, null, "内容"));
        assertEquals(LyricServiceErrorCode.LYRIC_NOT_FOUND.getCode(), notFound.getCode());

        //内容与格式同时为空时报参数错误
        ServiceException empty = assertThrows(ServiceException.class,
                () -> lyricService.updateLyric(lyric.getLyricId(), null, null));
        assertEquals(LyricServiceErrorCode.LYRIC_UPDATE_EMPTY.getCode(), empty.getCode());
    }

    @Test
    void removeLyricClearsDefaultReference() {
        Music music = createMusic("删除默认引用测试-" + UUID.randomUUID());
        Lyric defaultLyric = createLyric(music, "zh-cn");
        Lyric otherLyric = createLyric(music, "ja");

        lyricService.setDefaultLyric(music.getMusicId(), defaultLyric.getLyricId());
        assertEquals(defaultLyric.getLyricId(), musicService.getById(music.getMusicId()).getDefaultLyricId());

        //删除默认歌词后引用被清除
        lyricService.removeLyric(defaultLyric.getLyricId());
        assertNull(musicService.getById(music.getMusicId()).getDefaultLyricId());

        //删除非默认歌词不影响默认引用
        lyricService.setDefaultLyric(music.getMusicId(), otherLyric.getLyricId());
        lyricService.removeLyric(createLyric(music, "en").getLyricId());
        assertEquals(otherLyric.getLyricId(), musicService.getById(music.getMusicId()).getDefaultLyricId());

        //删除不存在的歌词报错
        ServiceException notFound = assertThrows(ServiceException.class,
                () -> lyricService.removeLyric(-1));
        assertEquals(LyricServiceErrorCode.LYRIC_NOT_FOUND.getCode(), notFound.getCode());
    }

    @Test
    void setDefaultLyricValidatesOwnership() {
        Music musicA = createMusic("默认歌词属主测试A-" + UUID.randomUUID());
        Music musicB = createMusic("默认歌词属主测试B-" + UUID.randomUUID());
        Lyric lyricB = createLyric(musicB, "zh-cn");

        //跨音乐设定被拒绝
        ServiceException mismatch = assertThrows(ServiceException.class,
                () -> lyricService.setDefaultLyric(musicA.getMusicId(), lyricB.getLyricId()));
        assertEquals(LyricServiceErrorCode.LYRIC_MUSIC_MISMATCH.getCode(), mismatch.getCode());

        //不存在的歌词被拒绝
        ServiceException notFound = assertThrows(ServiceException.class,
                () -> lyricService.setDefaultLyric(musicA.getMusicId(), -1));
        assertEquals(LyricServiceErrorCode.LYRIC_NOT_FOUND.getCode(), notFound.getCode());

        //正常设定与取消
        Lyric lyricA = createLyric(musicA, "zh-cn");
        lyricService.setDefaultLyric(musicA.getMusicId(), lyricA.getLyricId());
        assertEquals(lyricA.getLyricId(), musicService.getById(musicA.getMusicId()).getDefaultLyricId());

        lyricService.setDefaultLyric(musicA.getMusicId(), null);
        assertNull(musicService.getById(musicA.getMusicId()).getDefaultLyricId());
    }

    @Test
    void getMusicLyricListReturnsAllLocales() {
        Music music = createMusic("多语言列表测试-" + UUID.randomUUID());
        createLyric(music, "ja");
        createLyric(music, "zh-cn");
        createLyric(music, "en-us");

        List<Lyric> lyrics = lyricService.getMusicLyrics(music.getMusicId());
        assertEquals(3, lyrics.size());
        //按歌词id升序
        assertTrue(lyrics.get(0).getLyricId() < lyrics.get(1).getLyricId());

        //空音乐返回空列表而非null
        Music emptyMusic = createMusic("无歌词音乐-" + UUID.randomUUID());
        assertTrue(lyricService.getMusicLyrics(emptyMusic.getMusicId()).isEmpty());
    }

    @Test
    void instrumentalFlagCoexistsWithLyrics() {
        Music music = createMusic("纯音乐共存测试-" + UUID.randomUUID());
        //is_instrumental三态：设为纯音乐
        musicService.updateInstrumental(music.getMusicId(), true);
        assertEquals(Boolean.TRUE, musicService.getById(music.getMusicId()).getIsInstrumental());

        //纯音乐标记不阻止歌词增删（数据来源与生命周期不同，展示层提示而非完整性约束）
        Lyric lyric = createLyric(music, "zh-cn");
        lyricService.removeLyric(lyric.getLyricId());
        assertTrue(lyricService.getMusicLyrics(music.getMusicId()).isEmpty());

        //清除标记回到未知态
        musicService.updateInstrumental(music.getMusicId(), null);
        assertNull(musicService.getById(music.getMusicId()).getIsInstrumental());
    }

    @Test
    void getLyricCoverageCountsDistinctMusic() {
        LyricCoverageVo before = lyricService.getLyricCoverage();

        Music music = createMusic("覆盖率统计测试-" + UUID.randomUUID());
        createLyric(music, "ja");
        //同音乐多语言仅计一次
        createLyric(music, "zh-cn");

        LyricCoverageVo after = lyricService.getLyricCoverage();
        assertEquals(before.getTotalMusicCount() + 1, after.getTotalMusicCount());
        assertEquals(before.getLyricMusicCount() + 1, after.getLyricMusicCount());
    }
}
