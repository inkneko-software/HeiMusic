package com.inkneko.heimusic.util.music;

import com.inkneko.heimusic.util.music.model.Cue;
import com.inkneko.heimusic.util.music.model.CueTrack;
import com.inkneko.heimusic.util.music.model.MusicFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CueParser 单元测试。测试不再依赖本机音乐库路径，所有 CUE 样例在 @TempDir 中动态生成。
 */
class CueParserTests {

    @TempDir
    Path tempDir;

    /**
     * 标准 CUE（REM + TITLE + PERFORMER + FILE + 多 TRACK + INDEX），UTF-8 编码。
     */
    private Path writeCue(String filename, String content, Charset charset) throws IOException {
        Path cue = tempDir.resolve(filename);
        Files.write(cue, content.getBytes(charset));
        return cue;
    }

    private static final String UTF8_CUE = """
            REM GENRE Anime
            REM DATE 2024
            TITLE "初音ミク テストアルバム"
            PERFORMER "テストアーティスト"
            FILE "album.wav" WAVE
              TRACK 01 AUDIO
                TITLE "曲目一"
                PERFORMER "テストアーティスト"
                INDEX 01 00:00:00
              TRACK 02 AUDIO
                TITLE "曲目二"
                INDEX 01 00:01:00
              TRACK 03 AUDIO
                TITLE "曲目三"
                INDEX 01 02:30:25
            """;

    @Test
    void parseUtf8Cue() throws IOException {
        Path cue = writeCue("utf8.cue", UTF8_CUE, StandardCharsets.UTF_8);

        Cue result = new CueParser().parse(cue.toString());

        assertEquals("初音ミク テストアルバム", result.getTitle());
        assertEquals("テストアーティスト", result.getPerformer());
        assertEquals(1, result.getMusicFiles().size());

        MusicFile musicFile = result.getMusicFiles().get(0);
        assertEquals("album.wav", musicFile.getFilename());
        assertEquals(3, musicFile.getCueTracks().size());

        CueTrack track1 = musicFile.getCueTracks().get(0);
        assertEquals(1, track1.getTrackNumber());
        assertEquals("曲目一", track1.getTitle());
        //显式声明的 PERFORMER 应覆盖专辑级 PERFORMER
        assertEquals("テストアーティスト", track1.getPerformer());
        assertEquals("0.0", track1.getStartTimeString());
        //上一轨的结束时间 = 下一轨 INDEX 的起始时间
        assertEquals("1.0", track1.getEndTimeString());

        CueTrack track2 = musicFile.getCueTracks().get(1);
        assertEquals(2, track2.getTrackNumber());
        assertEquals("曲目二", track2.getTitle());
        //TRACK 02 没有声明 PERFORMER，保持为 null，由上层 MusicScanner 回退到专辑 PERFORMER
        assertNull(track2.getPerformer());
        assertEquals("1.0", track2.getStartTimeString());

        CueTrack track3 = musicFile.getCueTracks().get(2);
        assertEquals("150.40", track3.getStartTimeString());
        //最后一轨没有后续 INDEX，结束时间为 null（上层用 ffprobe 时长兜底）
        assertNull(track3.getEndTimeString());
    }

    @Test
    void parseGbkCue() throws IOException {
        //GBK 编码的中文 CUE：解析器应通过编码探测正确解码（GB18030 为 GBK 超集，两种猜测都能正确还原）
        String gbkCue = """
                TITLE "中文标题测试专辑名称"
                PERFORMER "测试艺术家"
                FILE "专辑音轨.wav" WAVE
                  TRACK 01 AUDIO
                    TITLE "第一首歌名"
                    INDEX 01 00:00:00
                """;
        Path cue = writeCue("gbk.cue", gbkCue, Charset.forName("GBK"));

        Cue result = new CueParser().parse(cue.toString());

        assertEquals("中文标题测试专辑名称", result.getTitle());
        assertEquals("测试艺术家", result.getPerformer());
        MusicFile musicFile = result.getMusicFiles().get(0);
        assertEquals("专辑音轨.wav", musicFile.getFilename());
        assertEquals("第一首歌名", musicFile.getCueTracks().get(0).getTitle());
    }

    @Test
    void timeTranslationFollowsCurrentFormula() throws IOException {
        //记录当前实现的时间换算行为：mm:ss:ff 中 ff 为帧号，实现取 1000/ff 作为毫秒部分。
        //注意：CUE 规范中 1 秒 = 75 帧，规范换算应为 ff*1000/75 毫秒，与当前实现不一致，
        //此处先固化现状，若未来修正 translateTime 需同步更新断言。
        //INDEX 01 02:30:25 -> 150 秒 + 1000/25=40 毫秒 -> "150.40"
        String cueContent = """
                TITLE "time"
                FILE "a.wav" WAVE
                  TRACK 01 AUDIO
                    INDEX 01 02:30:25
                """;
        Path cue = writeCue("time.cue", cueContent, StandardCharsets.US_ASCII);

        Cue result = new CueParser().parse(cue.toString());

        List<CueTrack> tracks = result.getMusicFiles().get(0).getCueTracks();
        assertEquals("150.40", tracks.get(0).getStartTimeString());
    }
}
