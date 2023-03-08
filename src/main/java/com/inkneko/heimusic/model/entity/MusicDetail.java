package com.inkneko.heimusic.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

@Data
public class MusicDetail {
    /**
     * CREATE TABLE music_detail(
     * 	music_id INT PRIMARY KEY AUTO_INCREMENT,
     *     album_id INT NOT NULL,
     *     title VARCHAR(255) NOT NULL,
     *     resource_path VARCHAR(255),
     *     bitrate INT,
     *     codec VARCHAR(20),
     *     duration INT,
     *     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     *     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     *     INDEX(album_id)
     * )Engine=InnoDB default charset=utf8mb4;
     */

    @TableId
    Integer musicId;
    Integer albumId;
    String title;
    String resourcePath;
    Integer bitrate;
    String codec;
    Integer duration;
    Date createdAt;
    Date updatedAt;
}
