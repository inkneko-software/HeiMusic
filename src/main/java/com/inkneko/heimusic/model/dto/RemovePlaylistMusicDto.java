package com.inkneko.heimusic.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class RemovePlaylistMusicDto {
    Integer playlistId;
    List<Integer> musicIdList;
}
