package com.inkneko.heimusic.util.music.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Stream {
    @JsonProperty("index")
    Integer index;

    @JsonProperty("codec_name")
    String codecName;

    @JsonProperty("codec_type")
    String codecType;

    @JsonAnySetter
    public void ignore(String key, Object value){

    }
}
