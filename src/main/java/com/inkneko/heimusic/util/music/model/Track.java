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
    //CUE文件中的起始时间，特指"INDEX 01"项
    String diskStartTime;
    //音乐的结束时间。由相邻的INDEX 01计算得到
    String diskEndTime;
}
