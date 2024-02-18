package com.inkneko.heimusic.util.music.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Track {
    String title;
    String filepath;
    String duration;
    String formatName;
    String size;
    String bitrate;
    String artist;
    Integer trackNumber;
    Integer trackTotal;
    Integer discNumber;
    Integer discTotal;
}
