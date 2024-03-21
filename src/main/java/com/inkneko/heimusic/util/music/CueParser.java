package com.inkneko.heimusic.util.music;

import com.inkneko.heimusic.util.music.model.Cue;
import com.inkneko.heimusic.util.music.model.CueTrack;
import com.inkneko.heimusic.util.music.model.MusicFile;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.parser.txt.CharsetDetector;
import org.apache.tika.parser.txt.CharsetMatch;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class CueParser {

    private enum State {
        GLOBAL, FILE, TRACK
    }

    private static final Pattern FILE_PATTERN = Pattern.compile("^FILE \"(.*)\" (WAVE|FLAC)$");
    private static final Pattern TRACK_PATTERN = Pattern.compile("^TRACK (\\d{2}) AUDIO$");
    private static final Pattern TITLE_PATTERN = Pattern.compile("^TITLE \"(.*)\"$");
    private static final Pattern PERFORMER_PATTERN = Pattern.compile("^PERFORMER \"(.*)\"$");
    private static final Pattern INDEX_PATTERN = Pattern.compile("^INDEX 01 (\\d{2}:\\d{2}:\\d{2})$");

    public Cue parse(String filename) throws IOException {
        //编码处理
        String charset = "UTF-8";
        CharsetDetector detector = new CharsetDetector();
        InputStream is = new BufferedInputStream(new FileInputStream(filename));
        detector.setText(is);
        Map<String, Integer> charsetAndConfidence = new HashMap<>();
        for (CharsetMatch match : detector.detectAll()) {
            charsetAndConfidence.put(match.getName(), match.getConfidence());
        }
        log.info("路径：{}，guess: {}, allGuess: {}", filename, detector.detect(), detector.detectAll());
        CharsetMatch charsetMatch = detector.detect();
        //如果不是100%的UTF8
        if (!(charsetMatch.getName().compareTo("UTF-8") == 0 && charsetMatch.getConfidence() == 100)) {
            //SHIFT_JIS vs GBK/GB18030
            Integer gb18030 = charsetAndConfidence.getOrDefault("GB18030", 0);
            Integer gbk = charsetAndConfidence.getOrDefault("GBK", 0);
            Integer jis = charsetAndConfidence.getOrDefault("Shift_JIS", 0);

            if (jis > gbk && jis > gb18030) {
                charset = "Shift_JIS";
            } else {
                if (gbk > gb18030) {
                    charset = "GBK";
                } else {
                    charset = "GB18030";
                }
            }
        }


        BufferedReader reader = new BufferedReader(new FileReader(filename, Charset.forName(charset)));

        Cue cue = new Cue();
        MusicFile musicFile = null;
        CueTrack cueTrack = null;
        CueTrack lastCueTrack = null;
        State state = State.GLOBAL;

        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();

            //ALBUM TITLE / TRACK TITLE
            Matcher matcher = TITLE_PATTERN.matcher(line);
            if (matcher.matches()) {
                if (state == State.GLOBAL) {
                    cue.setTitle(matcher.group(1));
                    continue;
                }
                if (state == State.TRACK) {
                    matcher = TITLE_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        cueTrack.setTitle(matcher.group(1));
                        continue;
                    }
                }
                continue;
            }
            //TRACK
            matcher = TRACK_PATTERN.matcher(line);
            if (matcher.matches()) {
                lastCueTrack = cueTrack;
                cueTrack = new CueTrack();
                cueTrack.setTrackNumber(Integer.parseInt(matcher.group(1)));
                musicFile.getCueTracks().add(cueTrack);
                state = State.TRACK;
                continue;
            }
            //INDEX
            matcher = INDEX_PATTERN.matcher(line);
            if (matcher.matches()) {
                if (state == State.TRACK) {
                    cueTrack.setStartTimeString(translateTime(matcher.group(1)));
                    if (lastCueTrack != null) {
                        lastCueTrack.setEndTimeString(translateTime(matcher.group(1)));
                    }
                    continue;
                }
                continue;
            }
            //FILE
            matcher = FILE_PATTERN.matcher(line);
            if (matcher.matches()) {
                musicFile = new MusicFile();
                musicFile.setFilename(matcher.group(1));
                musicFile.setCueTracks(new ArrayList<>());
                cue.getMusicFiles().add(musicFile);
                state = State.FILE;
                continue;
            }
            //PERFORMER
            matcher = PERFORMER_PATTERN.matcher(line);
            if (matcher.matches()) {
                if (state == State.GLOBAL) {
                    // ALBUM PERFORMER
                    cue.setPerformer(matcher.group(1));
                } else if (state == State.TRACK) {
                    cueTrack.setPerformer(matcher.group(1));
                }
            }
//
//
//        if (state == State.GLOBAL) {
//            matcher = PERFORMER_PATTERN.matcher(line);
//            if (matcher.matches()) {
//                cue.setPerformer(matcher.group(1));
//                continue;
//            }
//
//            matcher = TITLE_PATTERN.matcher(line);
//            if (matcher.matches()) {
//                cue.setTitle(matcher.group(1));
//                continue;
//            }
//
//            matcher = FILE_PATTERN.matcher(line);
//            if (matcher.matches()) {
//                musicFile = new MusicFile();
//                musicFile.setFilename(matcher.group(1));
//                musicFile.setCueTracks(new ArrayList<>());
//                cue.getMusicFiles().add(musicFile);
//                state = State.FILE;
//            }
//        } else if (state == State.FILE) {
//            matcher = TRACK_PATTERN.matcher(line);
//            if (matcher.matches()) {
//                lastCueTrack = cueTrack;
//                cueTrack = new CueTrack();
//                cueTrack.setTrackNumber(Integer.parseInt(matcher.group(1)));
//                musicFile.getCueTracks().add(cueTrack);
//                state = State.TRACK;
//            }
//        } else if (state == State.TRACK) {
//            matcher = TITLE_PATTERN.matcher(line);
//            if (matcher.matches()) {
//                cueTrack.setTitle(matcher.group(1));
//                continue;
//            }
//
//            matcher = PERFORMER_PATTERN.matcher(line);
//            if (matcher.matches()) {
//                cueTrack.setPerformer(matcher.group(1));
//                continue;
//            }
//
//            matcher = INDEX_PATTERN.matcher(line);
//            if (matcher.matches()) {
//                cueTrack.setStartTimeString(replaceLast(matcher.group(1), ':', "."));
//                if (lastCueTrack != null) {
//                    lastCueTrack.setEndTimeString(replaceLast(matcher.group(1), ':', "."));
//                }
//                state = State.FILE;
//            }
//        }
        }

        reader.close();
        return cue;
    }

    private String replaceLast(String source, char c, String replacement) {
        StringBuilder b = new StringBuilder(source);
        int pos = source.lastIndexOf(c);
        b.replace(pos, pos + 1, replacement);
        return b.toString();
    }

    /**
     * 将 mm:ss:ff (minute-second-frame) 格式转换为秒
     * <p>
     * 参见：https://en.wikipedia.org/wiki/Cue_sheet_(computing)#:~:text=mm%3Ass%3Aff%20(minute%2Dsecond%2Dframe)%20format.
     *
     * @return
     */
    private String translateTime(String time) {
        final Pattern timePattern = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})");
        Matcher matcher = timePattern.matcher(time);
        String result = "0";
        try {
            if (matcher.matches()) {
                int minutes = Integer.parseInt(matcher.group(1));
                int seconds = Integer.parseInt(matcher.group(2));
                int millSeconds = 0;
                if (Integer.parseInt(matcher.group(3)) != 0){
                    millSeconds = 1000 / Integer.parseInt(matcher.group(3));
                }
                return String.format("%d.%d", minutes * 60 + seconds, millSeconds);
            }
        } catch (NumberFormatException e) {
            log.error("未知时间格式：{}", timePattern);
        }
        return result;

    }
}