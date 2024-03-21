package com.inkneko.heimusic.util.music.model;

import lombok.Data;

@Data
public class CueTrack {
    private String title;
    private String performer;
    private Integer trackNumber;
    private Integer trackTotal;
    //使用INDEX 01 mm:ss:ff，但被转换为了秒.毫秒格式
    private String startTimeString;
    //同上，但如果是最后一个track，则为null
    private String endTimeString;
}
