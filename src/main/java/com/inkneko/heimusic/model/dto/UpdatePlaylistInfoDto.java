package com.inkneko.heimusic.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;

@Data
public class UpdatePlaylistInfoDto {
    @NotNull
    @Schema(description = "播放列表id")
    private Integer playlistId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "说明信息")
    private String description;

    @Schema(description = "序号")
    private Integer sequenceNumber;

    @Schema(description = "封面图片文件")
    private MultipartFile coverFile;
}
