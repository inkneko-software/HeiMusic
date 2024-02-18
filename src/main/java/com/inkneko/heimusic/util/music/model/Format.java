package com.inkneko.heimusic.util.music.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Format {
    @JsonProperty("filename")
    String filename;

    @JsonProperty("format_name")
    String formatName;

    @JsonProperty("duration")
    String duration;

    @JsonProperty("size")
    String size;

    @JsonProperty("bit_rate")
    String bitrate;

    @JsonProperty("probe_score")
    String probeScore;

    @JsonProperty("tags")
    Tags tags;

    @JsonAnySetter
    public void ignore(String name, Object value) {
    }
}