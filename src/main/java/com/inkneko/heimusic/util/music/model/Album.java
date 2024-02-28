package com.inkneko.heimusic.util.music.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Album {
    String title;
    String artist;
    String coverFilePath;
    List<Track> trackList;
    Boolean isCueIndexed;
}
