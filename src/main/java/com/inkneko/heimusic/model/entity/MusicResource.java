package com.inkneko.heimusic.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MusicResource {
    /**
     * CREATE TABLE music_resource(
     *     music_resource_id INT PRIMARY KEY AUTO_INCREMENT,
     *     music_id INT NOT NULL,
     *     codec VARCHAR(20),
     *     bitrate INT,
     *     bitrate_str VARCHAR(20),
     *     url VARCHAR(255),
     *     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     *     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     *     INDEX(music_id)
     * )Engine=InnoDB default charset=utf8mb4;
     */

    @TableId(type = IdType.AUTO)
    private Integer musicResourceId;
    private Integer musicId;
    private String codec;
    private String bitrate;
    private String bucket;
    private String objectKey;
    private Date createdAt;
    private Date updatedAt;
}
