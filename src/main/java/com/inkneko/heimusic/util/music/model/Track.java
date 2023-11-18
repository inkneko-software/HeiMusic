package com.inkneko.heimusic.util.music.model;

import lombok.Data;

@Data
public class Track {
    private String title;
    private String performer;
    private String startTimeString; // use INDEX 01
    private String endTimeString;   //null if last of the track
}
