package com.inkneko.heimusic.util.music;

import com.inkneko.heimusic.config.HeiMusicConfig;
import com.inkneko.heimusic.util.music.model.Album;
import com.inkneko.heimusic.util.music.model.Track;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Slf4j
@SpringBootTest
public class MusicScannerTests {
    @Autowired
    HeiMusicConfig heiMusicConfig;

    String basedir = "F:\\pt\\保种";
    @Test
    void testScanDir(){
//        MusicScanner musicScanner = new MusicScanner(heiMusicConfig);
//        List<Album> albums =  musicScanner.scanDirectory(new File(heiMusicConfig.getLocalDataDirectory()));
//        for(Album album : albums){
//            log.info("{}", album.getTitle());
//            if (album.getIsCueIndexed()){
//                for (Track track : album.getTrackList()) {
//                    log.info("cue track: {}", track);
//                    if (track.getTitle() == null){
//                        log.error("null track");
//                    }
//                }
//            }
//        }
    }

    @Test
    void testCopy(){
        Map<String, Album> map = new HashMap<>();
        List<Album> list = new ArrayList<>();
        Album album = new Album();
        album.setTitle("beforePut");
        map.put("test", album);
        album.setTitle("afterPut");
        list.add(album);

        log.info("当前album {}", album.getTitle());
        log.info("当前map {}", map.get("test").getTitle());
        log.info("当前list {}", list.get(0).getTitle());
    }

    public static void main(String[] args) {
    }
}
