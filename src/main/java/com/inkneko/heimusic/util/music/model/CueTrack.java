package com.inkneko.heimusic.util.music.model;

import lombok.Data;

@Data
public class CueTrack {
    private String title;
    private String performer;
    private Integer trackNumber;
    private Integer trackTotal;
    private String startTimeString; // use INDEX 01
    private String endTimeString;   //null if last of the track
}
