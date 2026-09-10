package com.inkneko.heimusic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.mapper.UserDetailMapper;
import com.inkneko.heimusic.model.entity.*;
import com.inkneko.heimusic.service.impl.AuthServiceImpl;
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
 * MusicService 集成测试：音乐 CRUD/缓存、艺术家关联、收藏、歌单（含属主校验）。
 */
@SpringBootTest
@Transactional
class MusicServiceTests {

    @Autowired
    MusicService musicService;

    @Autowired
    ArtistService artistService;

    @Autowired
    AuthServiceImpl authService;

    @Autowired
    UserDetailMapper userDetailMapper;

    @Autowired
    CacheManager cacheManager;

    @AfterEach
    void clearCaches() {
        for (String cacheName : List.of("music", "musicResource", "musicArtistList", "musicFavorite")) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
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

    @Test
    void musicCrudKeepsCacheConsistent() {
        Music music = createMusic("测试音乐");
        assertEquals("测试音乐", musicService.getById(music.getMusicId()).getTitle());

        //updateById 后应读到新值（缓存被驱逐而非被 boolean 污染）
        Music update = new Music();
        update.setMusicId(music.getMusicId());
        update.setTitle("改名音乐");
        assertTrue(musicService.updateById(update));
        assertEquals("改名音乐", musicService.getById(music.getMusicId()).getTitle());

        assertTrue(musicService.removeById(music.getMusicId()));
        assertNull(musicService.getById(music.getMusicId()));
    }

    @Test
    void musicArtistManagement() {
        Music music = createMusic("多艺术家曲目");
        Artist artist1 = new Artist();
        artist1.setName("音乐家A");
        artistService.save(artist1);
        Artist artist2 = new Artist();
        artist2.setName("音乐家B");
        artistService.save(artist2);

        musicService.addMusicArtists(music.getMusicId(), List.of(artist1.getArtistId(), artist2.getArtistId()));
        assertEquals(2, musicService.getMusicArtists(music.getMusicId()).size());

        //updateMusicArtists 为全量替换
        Artist artist3 = new Artist();
        artist3.setName("音乐家C");
        artistService.save(artist3);
        musicService.updateMusicArtists(music.getMusicId(), List.of(artist3.getArtistId()));
        List<MusicArtist> artists = musicService.getMusicArtists(music.getMusicId());
        assertEquals(1, artists.size());
        assertEquals(artist3.getArtistId(), artists.get(0).getArtistId());

        //按名称设定：不存在的艺术家自动创建
        String newArtistName = "自动创建的艺术家-" + UUID.randomUUID();
        musicService.updateMusicArtistsWithName(music.getMusicId(), List.of(newArtistName));
        assertNotNull(artistService.getOne(new LambdaQueryWrapper<Artist>().eq(Artist::getName, newArtistName)));

        musicService.removeMusicArtists(music.getMusicId(), List.of(
                artistService.getOne(new LambdaQueryWrapper<Artist>().eq(Artist::getName, newArtistName)).getArtistId()));
        assertTrue(musicService.getMusicArtists(music.getMusicId()).isEmpty());
    }

    @Test
    void favoriteMusicFlow() {
        Integer userId = createUser();
        Music music = createMusic("被收藏的音乐");

        assertFalse(musicService.isFavorite(userId, music.getMusicId()));

        musicService.addUserMusicFavorite(userId, music.getMusicId());
        assertTrue(musicService.isFavorite(userId, music.getMusicId()));
        assertEquals(1, musicService.getUserMusicFavoriteList(userId).size());

        //重复收藏幂等，不抛异常
        assertDoesNotThrow(() -> musicService.addUserMusicFavorite(userId, music.getMusicId()));
        assertEquals(1, musicService.getUserMusicFavoriteList(userId).size());

        musicService.removeUserMusicFavorite(userId, music.getMusicId());
        assertFalse(musicService.isFavorite(userId, music.getMusicId()));

        //收藏不存在的音乐返回 404
        ServiceException exception = assertThrows(ServiceException.class,
                () -> musicService.addUserMusicFavorite(userId, -1));
        assertEquals(404, exception.getCode());
    }

    @Test
    void playlistOwnershipAndMusicManagement() {
        Integer ownerId = createUser();
        Integer otherUserId = createUser();
        Music music1 = createMusic("歌单曲目1");
        Music music2 = createMusic("歌单曲目2");

        Playlist playlist = new Playlist();
        playlist.setUserId(ownerId);
        playlist.setTitle("我的歌单");
        musicService.addPlaylist(playlist);
        Integer playlistId = playlist.getPlaylistId();

        //非创建者：更新/删除/添加音乐均被拒绝
        Playlist update = new Playlist();
        update.setPlaylistId(playlistId);
        update.setTitle("篡改标题");
        assertOwnershipDenied(() -> musicService.updatePlaylist(update, otherUserId));
        assertOwnershipDenied(() -> musicService.removePlaylist(playlistId, otherUserId));
        assertOwnershipDenied(() -> musicService.addPlaylistMusic(playlistId, List.of(music1.getMusicId()), otherUserId));

        //创建者添加、移除音乐
        musicService.addPlaylistMusic(playlistId, List.of(music1.getMusicId(), music2.getMusicId()), ownerId);
        assertEquals(2, musicService.getPlaylistMusicList(playlistId).size());

        musicService.removePlaylistMusic(playlistId, List.of(music1.getMusicId()), ownerId);
        List<Music> remaining = musicService.getPlaylistMusicList(playlistId);
        assertEquals(1, remaining.size());
        assertEquals(music2.getMusicId(), remaining.get(0).getMusicId());

        //创建者更新与删除
        update.setTitle("改名歌单");
        musicService.updatePlaylist(update, ownerId);
        assertEquals("改名歌单", musicService.getPlaylist(playlistId).getTitle());

        musicService.removePlaylist(playlistId, ownerId);
        ServiceException gone = assertThrows(ServiceException.class, () -> musicService.getPlaylist(playlistId));
        assertEquals(404, gone.getCode());

        //不存在的歌单
        ServiceException notFound = assertThrows(ServiceException.class,
                () -> musicService.addPlaylistMusic(-1, List.of(music1.getMusicId()), ownerId));
        assertEquals(404, notFound.getCode());
    }

    @Test
    void playlistSubscribeFlow() {
        Integer ownerId = createUser();
        Integer subscriberId = createUser();
        Playlist playlist = new Playlist();
        playlist.setUserId(ownerId);
        playlist.setTitle("值得收藏的歌单");
        musicService.addPlaylist(playlist);

        musicService.addPlaylistSubscribe(subscriberId, playlist.getPlaylistId());
        List<Playlist> subscribed = musicService.getPlaylistSubscribed(subscriberId);
        assertEquals(1, subscribed.size());
        assertEquals(playlist.getPlaylistId(), subscribed.get(0).getPlaylistId());

        //重复收藏返回 400
        ServiceException duplicate = assertThrows(ServiceException.class,
                () -> musicService.addPlaylistSubscribe(subscriberId, playlist.getPlaylistId()));
        assertEquals(400, duplicate.getCode());

        musicService.removePlaylistSubscribe(subscriberId, playlist.getPlaylistId());
        assertTrue(musicService.getPlaylistSubscribed(subscriberId).isEmpty());
    }

    @Test
    void musicResourceCacheConsistent() {
        Music music = createMusic("带资源的音乐");
        MusicResource resource = new MusicResource();
        resource.setMusicId(music.getMusicId());
        resource.setCodec("flac");
        musicService.saveMusicResource(resource);
        assertEquals(1, musicService.getMusicResources(music.getMusicId()).size());

        assertEquals("flac", musicService.getMusicResource(resource.getMusicResourceId()).getCodec());

        //更新后缓存被驱逐，读到新值
        MusicResource update = new MusicResource();
        update.setMusicResourceId(resource.getMusicResourceId());
        update.setCodec("mp3");
        musicService.updateMusicResource(update);
        assertEquals("mp3", musicService.getMusicResource(resource.getMusicResourceId()).getCodec());

        musicService.removeMusicResource(resource.getMusicResourceId());
        assertNull(musicService.getMusicResource(resource.getMusicResourceId()));
    }

    private static void assertOwnershipDenied(Runnable action) {
        ServiceException exception = assertThrows(ServiceException.class, action::run);
        assertEquals(403, exception.getCode(), "错误信息: " + exception.getMessage());
    }
}
