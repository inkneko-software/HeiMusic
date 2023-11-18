package com.inkneko.heimusic.util.music.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MusicFile {
    private String      filename;
    private List<Track> tracks;

    public MusicFile(){
        tracks = new ArrayList<>();
    }
}
