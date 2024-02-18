package com.inkneko.heimusic.util.music.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MusicFile {
    private String      filename;
    private List<CueTrack> cueTracks;

    public MusicFile(){
        cueTracks = new ArrayList<>();
    }
}
