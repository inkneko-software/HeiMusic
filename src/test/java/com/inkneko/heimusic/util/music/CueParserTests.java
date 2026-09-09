package com.inkneko.heimusic.util.music;

import com.inkneko.heimusic.util.music.model.Cue;
import com.inkneko.heimusic.util.music.model.MusicFile;
import com.inkneko.heimusic.util.music.model.CueTrack;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.parser.txt.CharsetDetector;
import org.apache.tika.parser.txt.CharsetMatch;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Slf4j
@SpringBootTest
public class CueParserTests {

    @Test
    void testGuessEncoding() throws IOException {
        CharsetDetector detector = new CharsetDetector();
        InputStream is = new BufferedInputStream(new FileInputStream("F:\\\\pt\\\\保种\\\\[jpopsuki_pt]初音ミク\\\\初音ミク - マジカルミライ 2016\\\\初音ミク - マジカルミライ 2016.cue"));
        detector.setText(is);
        CharsetMatch match = detector.detect();
        log.info("confidence: {}, charset name: {}", match.getConfidence(), match.getName());
        log.info("{}", match);
        for(CharsetMatch charsetMatch : detector.detectAll()){
            log.info("{}",charsetMatch);
        }

        List<String> charsetNames = Arrays.stream(detector.detectAll()).toList().stream().map(CharsetMatch::getName).toList();
        if (charsetNames.contains("GB18030")){
            log.info("{}", new String(is.readAllBytes(), "GB18030"));
        } else if (charsetNames.contains("GBK")) {
            log.info("{}", new String(is.readAllBytes(), "GBK"));
        }else {
            log.info("未知编码：{}", charsetNames);
        }
        is.close();
    }

    @Test
    void testParse(){
        CueParser parser = new CueParser();
        try {
            Cue cue = parser.parse("D:\\1-音乐\\New Game!!\\[SP07] Bonus CD - 01\\image.cue");
            MusicFile musicFile = cue.getMusicFiles().get(0);
            for (CueTrack cueTrack : musicFile.getCueTracks()){
                if (cueTrack.getEndTimeString() != null){
                    System.out.printf("ffmpeg -i %s -vn -ss %s -to %s -metadata artist=\"%s\" -metadata title=\"%s\" \"%s.flac\"%n", musicFile.getFilename(), cueTrack.getStartTimeString(), cueTrack.getEndTimeString(), cueTrack.getPerformer(), cueTrack.getTitle(), cueTrack.getTitle());
                }else {
                    System.out.printf("ffmpeg -i %s -vn -ss %s -metadata artist=\"%s\" -metadata title=\"%s\" \"%s.flac\"%n", musicFile.getFilename(), cueTrack.getStartTimeString(), cueTrack.getPerformer(), cueTrack.getTitle(), cueTrack.getTitle());
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
