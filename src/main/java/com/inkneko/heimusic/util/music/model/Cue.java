package com.inkneko.heimusic.util.music.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Cue {
    private String performer;
    private String title;
    private List<MusicFile> musicFiles;

    public Cue(){
        musicFiles = new ArrayList<>();
    }
}
