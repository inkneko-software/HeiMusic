package com.inkneko.heimusic.util.music;

import com.inkneko.heimusic.config.HeiMusicConfig;
import com.inkneko.heimusic.util.music.model.Album;
import com.inkneko.heimusic.util.music.model.Track;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * MusicScanner 单元测试。用纯 Java 生成的 WAV 作为真实音轨（ffprobe 可解析），
 * 覆盖 CUE 索引专辑、已分割专辑与已扫描文件跳过三条主路径。
 */
class MusicScannerTests {

    @TempDir
    Path tempDir;

    static HeiMusicConfig config;

    @BeforeAll
    static void setUp() {
        config = new HeiMusicConfig();
        config.setDomain("localhost");
        config.setLocalApplicationDataDirectory("target/test-app-data");
    }

    private MusicScanner newScanner() {
        return new MusicScanner(config);
    }

    /**
     * 构造一个 CUE 索引专辑目录：album.wav（3 秒）+ album.cue（2 轨）+ cover.jpg。
     */
    private Path createCueAlbumDir(String dirName) throws IOException {
        Path albumDir = tempDir.resolve(dirName);
        Files.createDirectories(albumDir);
        TestMediaFiles.writeSineWav(albumDir.resolve("album.wav"), 3);
        Files.write(albumDir.resolve("cover.jpg"), new byte[0]);
        String cue = """
                TITLE "扫描测试专辑"
                PERFORMER "扫描艺术家"
                FILE "album.wav" WAVE
                  TRACK 01 AUDIO
                    TITLE "第一轨"
                    INDEX 01 00:00:00
                  TRACK 02 AUDIO
                    TITLE "第二轨"
                    INDEX 01 00:01:00
                """;
        Files.write(albumDir.resolve("album.cue"), cue.getBytes(StandardCharsets.UTF_8));
        return albumDir;
    }

    @Test
    void scanCueIndexedAlbum() throws IOException {
        assumeTrue(TestMediaFiles.ffprobeAvailable(), "ffprobe 不可用，跳过");
        Path albumDir = createCueAlbumDir("cue-album");

        List<Album> albums = newScanner().scanDirectory(albumDir.toFile(), f -> false, a -> {
        });

        assertEquals(1, albums.size());
        Album album = albums.get(0);
        assertEquals("扫描测试专辑", album.getTitle());
        assertEquals("扫描艺术家", album.getArtist());
        assertTrue(album.getIsCueIndexed());
        //封面优先选取以 cover 开头的图片
        assertEquals(albumDir.resolve("cover.jpg").toFile().getAbsolutePath(), album.getCoverFilePath());
        assertEquals(2, album.getTrackList().size());

        Track track1 = album.getTrackList().get(0);
        assertEquals(1, track1.getTrackNumber());
        assertEquals("第一轨", track1.getTitle());
        assertEquals(albumDir.resolve("album.wav").toFile().getAbsolutePath(), track1.getFilepath());
        //CUE 声明了 PERFORMER 为空时回退到专辑艺术家
        assertEquals("扫描艺术家", track1.getArtist());
        //单文件多轨：使用 CUE 的起始/结束时间而非 ffprobe 时长
        assertEquals("0.0", track1.getDiskStartTime());
        assertEquals("1.0", track1.getDiskEndTime());

        Track track2 = album.getTrackList().get(1);
        //最后一轨的结束时间由 ffprobe 时长兜底（3 秒的 WAV）
        assertEquals("1.0", track2.getDiskStartTime());
        assertNotNull(track2.getDiskEndTime());
        assertTrue(track2.getDiskEndTime().startsWith("3.0"), "结束时间应为 ffprobe 返回的 3 秒，实际: " + track2.getDiskEndTime());
    }

    @Test
    void scanSplitedAlbumWithoutCue() throws IOException {
        assumeTrue(TestMediaFiles.ffprobeAvailable(), "ffprobe 不可用，跳过");
        Path albumDir = tempDir.resolve("split-album");
        Files.createDirectories(albumDir);
        TestMediaFiles.writeSineWav(albumDir.resolve("track01.wav"), 2);
        TestMediaFiles.writeSineWav(albumDir.resolve("track02.wav"), 2);
        Files.write(albumDir.resolve("booklet.jpg"), new byte[0]);

        List<Album> albums = newScanner().scanDirectory(albumDir.toFile(), f -> false, a -> {
        });

        assertEquals(1, albums.size());
        Album album = albums.get(0);
        //WAV 没有专辑 tag，专辑名回退为目录名；无 cover 开头图片时取第一张图
        assertEquals("split-album", album.getTitle());
        assertFalse(album.getIsCueIndexed());
        assertEquals(albumDir.resolve("booklet.jpg").toFile().getAbsolutePath(), album.getCoverFilePath());
        assertEquals(2, album.getTrackList().size());

        Track track1 = album.getTrackList().get(0);
        //无 tag 时曲名回退为文件名
        assertEquals("track01.wav", track1.getTitle());
        assertEquals("wav", track1.getFormatName());
        assertTrue(track1.getDuration().startsWith("2.0"), "时长应为 2 秒，实际: " + track1.getDuration());
        assertNull(track1.getDiskStartTime());
    }

    @Test
    void skipsAlreadyScannedMusicFiles() throws IOException {
        Path albumDir = createCueAlbumDir("scanned-album");

        //所有音乐文件标记为已扫描 -> 不产出任何专辑
        List<Album> albums = newScanner().scanDirectory(albumDir.toFile(), f -> true, a -> {
        });

        assertTrue(albums.isEmpty());
    }

    @Test
    void albumConsumerReceivesEachAlbum() throws IOException {
        assumeTrue(TestMediaFiles.ffprobeAvailable(), "ffprobe 不可用，跳过");
        Path albumDir = createCueAlbumDir("consumer-album");
        AtomicInteger received = new AtomicInteger();

        newScanner().scanDirectory(albumDir.toFile(), f -> false, album -> received.incrementAndGet());

        assertEquals(1, received.get());
    }

    @Test
    void parseArtistsSplitsOnSeparators() {
        List<String> artists = MusicScanner.parseArtists("Artist A, Artist B、Artist C & Artist D feat. Artist E");
        assertEquals(List.of("Artist A", "Artist B", "Artist C", "Artist D", "Artist E"), artists);
    }

    @Test
    void parseArtistsHandlesNullAndSingle() {
        assertTrue(MusicScanner.parseArtists(null).isEmpty());
        assertEquals(List.of("Solo Artist"), MusicScanner.parseArtists("Solo Artist"));
    }
}
