package com.inkneko.heimusic.util.music.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Tags {
    @JsonProperty("ALBUM")
    String album;

    @JsonProperty("ARTIST")
    String artist;

    @JsonProperty("album_artist")
    String albumArtist;

    @JsonProperty("track")
    String track;

    @JsonProperty("TRACKTOTAL")
    String trackTotal;

    @JsonProperty("disc")
    String disc;

    @JsonProperty("DISCTOTAL")
    String discTotal;

    @JsonProperty("TITLE")
    String title;

    @JsonProperty("COMMENT")
    String comment;

    @JsonAnySetter
    public void capabilityForDifferentFfprobeMuxers(String name, Object value) {
        //不同的muxer会产生不通的tag，如艺术家artist，正常情况下是ARTIST，但有时会是Artist
        if (value instanceof String) {
            if (name.compareToIgnoreCase("artist") == 0) {
                artist = (String) value;
            } else if (name.compareToIgnoreCase("album_artist") == 0) {
                albumArtist = (String) value;
            }
        }
    }
}
