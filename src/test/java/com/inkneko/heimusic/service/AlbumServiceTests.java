package com.inkneko.heimusic.service;

import com.inkneko.heimusic.model.entity.Album;
import com.inkneko.heimusic.model.entity.Artist;
import com.inkneko.heimusic.model.entity.Music;
import com.inkneko.heimusic.model.entity.MusicArtist;
import com.inkneko.heimusic.service.MusicService;
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
 * AlbumService 集成测试：专辑 CRUD/缓存、专辑-音乐/专辑-艺术家关联、分页。
 */
@SpringBootTest
@Transactional
class AlbumServiceTests {

    @Autowired
    AlbumService albumService;

    @Autowired
    MusicService musicService;

    @Autowired
    ArtistService artistService;

    @Autowired
    CacheManager cacheManager;

    @AfterEach
    void clearCaches() {
        for (String cacheName : List.of("album", "albumMusicList", "albumMusicNum", "albumArtistList")) {
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

    @Test
    void albumCrudKeepsCacheConsistent() {
        Album album = new Album("测试专辑");
        albumService.save(album);
        assertEquals("测试专辑", albumService.getById(album.getAlbumId()).getTitle());

        Album update = new Album("改名专辑");
        update.setAlbumId(album.getAlbumId());
        assertTrue(albumService.updateById(update));
        assertEquals("改名专辑", albumService.getById(album.getAlbumId()).getTitle());

        assertTrue(albumService.removeById(album));
        assertNull(albumService.getById(album.getAlbumId()));
    }

    @Test
    void albumMusicManagement() {
        Album album = new Album("收录专辑");
        albumService.save(album);
        Music music1 = createMusic("专辑曲1");
        Music music2 = createMusic("专辑曲2");

        albumService.addAlbumMusic(album.getAlbumId(), List.of(music1.getMusicId(), music2.getMusicId()));
        assertEquals(2, albumService.getAlbumMusicList(album.getAlbumId()).size());
        assertEquals(2L, albumService.getAlbumMusicNum(album.getAlbumId()));

        albumService.removeAlbumMusic(album.getAlbumId(), List.of(music1.getMusicId()));
        assertEquals(1L, albumService.getAlbumMusicNum(album.getAlbumId()));

        //通过音乐反查专辑
        assertEquals(album.getAlbumId(), albumService.getAlbumMusicByMusicId(music2.getMusicId()).getAlbumId());
        assertNull(albumService.getAlbumMusicByMusicId(music1.getMusicId()));
    }

    @Test
    void albumArtistManagement() {
        Album album = new Album("艺术家专辑");
        albumService.save(album);

        //按名称添加：不存在的自动创建
        String artistName = "专辑艺术家-" + UUID.randomUUID();
        albumService.addAlbumArtistWithNames(album.getAlbumId(), List.of(artistName));
        assertEquals(1, albumService.getAlbumArtist(album.getAlbumId()).size());

        //重复添加同一艺术家被忽略（复合主键冲突）
        albumService.addAlbumArtistWithNames(album.getAlbumId(), List.of(artistName));
        assertEquals(1, albumService.getAlbumArtist(album.getAlbumId()).size());

        //updateAlbumArtist 为全量替换
        Artist replacement = new Artist();
        replacement.setName("替换艺术家-" + UUID.randomUUID());
        artistService.save(replacement);
        albumService.updateAlbumArtist(album.getAlbumId(), List.of(replacement.getArtistId()));
        assertEquals(1, albumService.getAlbumArtist(album.getAlbumId()).size());
        assertEquals(replacement.getArtistId(), albumService.getAlbumArtist(album.getAlbumId()).get(0).getArtistId());

        albumService.removeAlbumArtist(album.getAlbumId(), List.of(replacement.getArtistId()));
        assertTrue(albumService.getAlbumArtist(album.getAlbumId()).isEmpty());
    }

    @Test
    void recentUploadIsPaginated() {
        Album album1 = new Album("专辑一");
        albumService.save(album1);
        Album album2 = new Album("专辑二");
        albumService.save(album2);
        Album album3 = new Album("专辑三");
        albumService.save(album3);

        List<Album> firstPage = albumService.getRecentUpload(1, 2);
        List<Album> secondPage = albumService.getRecentUpload(2, 2);
        assertEquals(2, firstPage.size());
        assertEquals(1, secondPage.size());
        //按 albumId 降序，最新创建的在前
        assertEquals(album3.getAlbumId(), firstPage.get(0).getAlbumId());
        assertEquals(album2.getAlbumId(), firstPage.get(1).getAlbumId());
        assertEquals(album1.getAlbumId(), secondPage.get(0).getAlbumId());
    }
}
