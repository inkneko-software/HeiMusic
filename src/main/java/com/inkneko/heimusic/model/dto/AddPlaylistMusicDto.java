package com.inkneko.heimusic.model.dto;

import lombok.Data;
import java.util.List;
@Data
public class AddPlaylistMusicDto {
    private Integer playlistId;
    private List<Integer> musicIdList;
}
