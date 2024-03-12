package com.inkneko.heimusic.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlbumMusic implements Serializable {
    Integer albumId;
    Integer musicId;
}
