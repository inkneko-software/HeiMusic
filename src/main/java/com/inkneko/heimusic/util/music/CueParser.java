package com.inkneko.heimusic.util.music;

import com.inkneko.heimusic.util.music.model.Cue;
import com.inkneko.heimusic.util.music.model.MusicFile;
import com.inkneko.heimusic.util.music.model.CueTrack;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CueParser {

    private enum State {
        GLOBAL, FILE, TRACK
    }

    private static final Pattern FILE_PATTERN = Pattern.compile("^FILE \"(.*)\" WAVE$");
    private static final Pattern TRACK_PATTERN = Pattern.compile("^TRACK (\\d{2}) AUDIO$");
    private static final Pattern TITLE_PATTERN = Pattern.compile("^TITLE \"(.*)\"$");
    private static final Pattern PERFORMER_PATTERN = Pattern.compile("^PERFORMER \"(.*)\"$");
    private static final Pattern INDEX_PATTERN = Pattern.compile("^INDEX 01 (\\d{2}:\\d{2}:\\d{2})$");

    public Cue parse(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        Cue cue = new Cue();
        MusicFile musicFile = null;
        CueTrack cueTrack = null;
        CueTrack lastCueTrack = null;
        State state = State.GLOBAL;

        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            Matcher matcher;

            if (state == State.GLOBAL) {
                matcher = PERFORMER_PATTERN.matcher(line);
                if (matcher.matches()) {
                    cue.setPerformer(matcher.group(1));
                    continue;
                }

                matcher = TITLE_PATTERN.matcher(line);
                if (matcher.matches()) {
                    cue.setTitle(matcher.group(1));
                    continue;
                }

                matcher = FILE_PATTERN.matcher(line);
                if (matcher.matches()) {
                    musicFile = new MusicFile();
                    musicFile.setFilename(matcher.group(1));
                    musicFile.setCueTracks(new ArrayList<>());
                    cue.getMusicFiles().add(musicFile);
                    state = State.FILE;
                }
            } else if (state == State.FILE) {
                matcher = TRACK_PATTERN.matcher(line);
                if (matcher.matches()) {
                    lastCueTrack = cueTrack;
                    cueTrack = new CueTrack();
                    musicFile.getCueTracks().add(cueTrack);
                    state = State.TRACK;
                }
            } else if (state == State.TRACK) {
                matcher = TITLE_PATTERN.matcher(line);
                if (matcher.matches()) {
                    cueTrack.setTitle(matcher.group(1));
                    continue;
                }

                matcher = PERFORMER_PATTERN.matcher(line);
                if (matcher.matches()) {
                    cueTrack.setPerformer(matcher.group(1));
                    continue;
                }

                matcher = INDEX_PATTERN.matcher(line);
                if (matcher.matches()) {
                    cueTrack.setStartTimeString(replaceLast(matcher.group(1), ':', "."));
                    if (lastCueTrack != null){
                        lastCueTrack.setEndTimeString(replaceLast(matcher.group(1), ':', "."));
                    }
                    state = State.FILE;
                }
            }
        }

        reader.close();
        return cue;
    }

    private String replaceLast(String source , char c, String replacement){
        StringBuilder b = new StringBuilder(source);
        int pos = source.lastIndexOf(c);
        b.replace(pos, pos + 1, replacement );
        return b.toString();
    }
}