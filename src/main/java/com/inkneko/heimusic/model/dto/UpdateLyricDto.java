package com.inkneko.heimusic.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateLyricDto {

    @Schema(description = "歌词id")
    @NotNull(message = "歌词id不能为空")
    private Integer lyricId;

    @Schema(description = "歌词全文，传null保持不变")
    @Size(max = 200000, message = "歌词内容过长，最大200000字符")
    private String content;

    @Schema(description = "歌词格式标识，传null保持不变")
    @Size(max = 32, message = "歌词格式最长32字符")
    @Pattern(regexp = "^[a-z0-9_]{1,32}$", message = "歌词格式仅支持小写字母、数字与下划线")
    private String format;
}
