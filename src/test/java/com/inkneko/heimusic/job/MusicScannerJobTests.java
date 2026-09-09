package com.inkneko.heimusic.job;

import com.inkneko.heimusic.config.HeiMusicConfig;
import com.inkneko.heimusic.util.music.MusicScanner;
import com.inkneko.heimusic.util.music.model.Album;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootTest
@Slf4j
public class MusicScannerJobTests {
    @Autowired
    MusicScannerJob musicScannerJob;

    @Autowired
    HeiMusicConfig heiMusicConfig;
    @Test
    void test() {
        musicScannerJob.process();
    }

    private static final Pattern ARTIST_PATTERN = Pattern.compile("(.+?)([,、&;]|feat.|Feat.)|(.+?)$");

    @Test
    void testParseArtist() {
//        MusicScanner musicScanner = new MusicScanner(heiMusicConfig);
//        List<Album> albumList = musicScanner.scanDirectory(new File(heiMusicConfig.getLocalDataDirectory()));
//        List<String> artistList = new ArrayList<>();
//        albumList.forEach(album -> {
//            album.getTrackList().forEach(track -> {
//                artistList.add(track.getArtist());
//            });
//        });
//
//        for (String artist : artistList) {
//            if (artist != null) {
//                Matcher matcher = ARTIST_PATTERN.matcher(artist);
//                List<String> resolvedArtist = new ArrayList<>();
//                while (matcher.find()) {
//                    String start = matcher.group(1);
//                    String tail = matcher.group(3);
//                    resolvedArtist.add(start != null ? start.strip() : tail.strip());
//                }
//                log.info("artist: {}, matches: {}", artist, String.join(" | ", resolvedArtist));
//            }
//        }

    } 
}
