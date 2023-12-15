package com.inkneko.heimusic.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MusicDto {
    private String title;
    private String translatedTitle;
    private List<String> artists;
    private String startTime;
    private String endTime;
}
