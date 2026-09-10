package com.inkneko.heimusic.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inkneko.heimusic.config.HeiMusicConfig;
import com.inkneko.heimusic.model.entity.Album;
import com.inkneko.heimusic.model.entity.Music;
import com.inkneko.heimusic.service.AlbumService;
import com.inkneko.heimusic.service.MusicService;
import com.inkneko.heimusic.util.music.TestMediaFiles;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * MusicScannerJob 端到端测试：在测试数据目录（target/test-music-data）中生成
 * 真实的 WAV + CUE 专辑目录，执行扫描任务，断言专辑/音乐/封面均正确入库，
 * 且重复扫描不会产生重复数据。数据库写入由 @Transactional 回滚。
 */
@SpringBootTest
@Transactional
class MusicScannerJobTests {

    @Autowired
    MusicScannerJob musicScannerJob;

    @Autowired
    HeiMusicConfig heiMusicConfig;

    @Autowired
    AlbumService albumService;

    @Autowired
    MusicService musicService;

    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    void cleanDataDir() throws IOException {
        //清空上一次运行残留的测试音乐目录，避免重复扫描旧文件
        Path dataDir = Path.of(heiMusicConfig.getLocalDataDirectory());
        if (Files.exists(dataDir)) {
            try (var paths = Files.walk(dataDir)) {
                paths.sorted(java.util.Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }

    @AfterEach
    void clearCaches() {
        for (String cacheName : List.of("album", "albumMusicList", "albumMusicNum", "albumArtistList",
                "music", "musicArtistList", "artist")) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    private Path createAlbumDir(String albumTitle, String performer) throws IOException {
        Path dataDir = Path.of(heiMusicConfig.getLocalDataDirectory());
        Path albumDir = dataDir.resolve(albumTitle + "-" + UUID.randomUUID());
        Files.createDirectories(albumDir);
        TestMediaFiles.writeSineWav(albumDir.resolve("album.wav"), 3);
        Files.write(albumDir.resolve("cover.jpg"), new byte[0]);
        String cue = """
                TITLE "%s"
                PERFORMER "%s"
                FILE "album.wav" WAVE
                  TRACK 01 AUDIO
                    TITLE "入库曲目一"
                    INDEX 01 00:00:00
                  TRACK 02 AUDIO
                    TITLE "入库曲目二"
                    INDEX 01 00:01:00
                """.formatted(albumTitle, performer);
        Files.write(albumDir.resolve("album.cue"), cue.getBytes(StandardCharsets.UTF_8));
        return albumDir;
    }

    @Test
    void scanIngestsAlbumAndIsIdempotent() throws IOException {
        assumeTrue(TestMediaFiles.ffprobeAvailable(), "ffprobe 不可用，跳过");
        String albumTitle = "扫描入库专辑";
        String performer = "扫描入库艺术家";
        Path albumDir = createAlbumDir(albumTitle, performer);

        musicScannerJob.process();

        //专辑已创建，标题/艺术家/封面正确
        Album savedAlbum = albumService.getOne(new LambdaQueryWrapper<Album>()
                .eq(Album::getTitle, albumTitle)
                .eq(Album::getAlbumArtist, performer));
        assertNotNull(savedAlbum, "扫描后应能按标题+艺术家查询到专辑");
        assertEquals(albumDir.resolve("cover.jpg").toFile().getAbsolutePath(), savedAlbum.getFrontCoverFilePath());
        assertEquals(2, albumService.getAlbumMusicList(savedAlbum.getAlbumId()).size());

        //音乐入库：路径、曲名、时长、CUE 起止时间
        Music track1 = musicService.getOne(new LambdaQueryWrapper<Music>()
                .eq(Music::getFilePath, albumDir.resolve("album.wav").toFile().getAbsolutePath())
                .eq(Music::getTitle, "入库曲目一"));
        assertNotNull(track1);
        assertEquals(performer, track1.getArtist());
        assertEquals("0.0", track1.getDiscStartTime());
        assertEquals("1.0", track1.getDiscEndTime());
        //多轨 CUE 专辑的 duration/codec 等由后续切片+probe 管线回填，入库时为表默认值 "0"
        assertEquals("0", track1.getDuration());

        //一张CUE专辑的多首曲目共享同一物理文件，按 filePath 查询应恰好返回两首
        List<Music> firstScanResult = musicService.list(new LambdaQueryWrapper<Music>()
                .eq(Music::getFilePath, albumDir.resolve("album.wav").toFile().getAbsolutePath()));
        assertEquals(2, firstScanResult.size());
        List<Integer> firstScanIds = firstScanResult.stream().map(Music::getMusicId).sorted().toList();

        //重复扫描：按 filePath 去重，不产生新的专辑与音乐（曲目 ID 不变）
        musicScannerJob.process();
        assertEquals(1, albumService.list(new LambdaQueryWrapper<Album>()
                .eq(Album::getTitle, albumTitle)
                .eq(Album::getAlbumArtist, performer)).size());
        List<Music> secondScanResult = musicService.list(new LambdaQueryWrapper<Music>()
                .eq(Music::getFilePath, albumDir.resolve("album.wav").toFile().getAbsolutePath()));
        assertEquals(2, secondScanResult.size());
        assertEquals(firstScanIds, secondScanResult.stream().map(Music::getMusicId).sorted().toList());
    }

    @Test
    void processOnEmptyDirectoryIsSafe() {
        //数据目录不存在或为空时，任务应安静返回，不抛异常
        assertDoesNotThrow(musicScannerJob::process);
    }
}
