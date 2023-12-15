package com.inkneko.heimusic.rabbitmq.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class SplitRequest {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MusicInfo {
        public Integer musicId;
        public String startTime;
        public String endTime;
    }

    private String musicFileBucket;
    private String musicFileObjectKey;
    private List<MusicInfo> musicList;
}
