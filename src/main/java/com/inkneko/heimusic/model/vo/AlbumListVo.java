package com.inkneko.heimusic.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlbumListVo {
    private List<AlbumVo> albumList;
    private Long total;
}
