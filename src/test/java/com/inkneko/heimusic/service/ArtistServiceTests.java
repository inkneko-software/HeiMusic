package com.inkneko.heimusic.service;

import com.inkneko.heimusic.model.entity.Artist;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ArtistService 集成测试：覆盖 CRUD 与 @Cacheable/@CachePut/@CacheEvict 的缓存一致性。
 */
@SpringBootTest
@Transactional
class ArtistServiceTests {

    @Autowired
    ArtistService artistService;

    @Autowired
    CacheManager cacheManager;

    @AfterEach
    void clearArtistCache() {
        //数据库由事务回滚，但内存缓存不会回滚，清空避免影响后续测试
        Cache cache = cacheManager.getCache("artist");
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void crudKeepsCacheConsistent() {
        Artist artist = new Artist();
        artist.setName("测试艺术家-" + UUID.randomUUID());
        artistService.save(artist);
        assertNotNull(artist.getArtistId());

        //第一次查询走数据库并写入缓存
        Artist loaded = artistService.getById(artist.getArtistId());
        assertEquals(artist.getName(), loaded.getName());

        //updateById 带 @CachePut：更新后应直接读到新值而不是缓存旧值
        Artist update = new Artist();
        update.setArtistId(artist.getArtistId());
        update.setName("改名后的艺术家");
        assertTrue(artistService.updateById(update));
        assertEquals("改名后的艺术家", artistService.getById(artist.getArtistId()).getName());

        //removeById 带 @CacheEvict：删除后应查不到
        assertTrue(artistService.removeById(artist));
        assertNull(artistService.getById(artist.getArtistId()));
    }

    @Test
    void getByIdReturnsNullForMissingId() {
        assertNull(artistService.getById(-1));
    }
}
