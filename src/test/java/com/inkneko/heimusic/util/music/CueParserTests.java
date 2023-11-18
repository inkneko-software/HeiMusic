package com.inkneko.heimusic.util.music;

import com.inkneko.heimusic.util.music.model.Cue;
import com.inkneko.heimusic.util.music.model.MusicFile;
import com.inkneko.heimusic.util.music.model.Track;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
public class CueParserTests {

    @Test
    void testParse(){
        CueParser parser = new CueParser();
        try {
            Cue cue = parser.parse("D:\\12345.cue");
            MusicFile musicFile = cue.getMusicFiles().get(0);
            for (Track track : musicFile.getTracks()){
                if (track.getEndTimeString() != null){
                    System.out.printf("ffmpeg -i %s -vn -ss %s -to %s -metadata artist=\"%s\" -metadata title=\"%s\" \"%s.flac\"%n", musicFile.getFilename(), track.getStartTimeString(), track.getEndTimeString(),track.getPerformer(), track.getTitle(), track.getTitle());
                }else {
                    System.out.printf("ffmpeg -i %s -vn -ss %s -metadata artist=\"%s\" -metadata title=\"%s\" \"%s.flac\"%n", musicFile.getFilename(), track.getStartTimeString(), track.getPerformer(), track.getTitle(), track.getTitle());
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
