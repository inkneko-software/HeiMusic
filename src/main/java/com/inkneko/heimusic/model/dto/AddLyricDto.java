package com.inkneko.heimusic.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddLyricDto {

    @Schema(description = "所属音乐id")
    @NotNull(message = "音乐id不能为空")
    private Integer musicId;

    @Schema(description = "语言标签，BCP 47风格，如zh-cn、ja，入库统一小写")
    @NotBlank(message = "语言标签不能为空")
    @Size(max = 32, message = "语言标签最长32字符")
    @Pattern(regexp = "^[A-Za-z]{2,8}(-[A-Za-z0-9]{1,8})*$", message = "语言标签格式非法，如zh-cn、en-us")
    private String locale;

    @Schema(description = "歌词格式标识：text/lrc/lrc_a2/qrc等")
    @NotBlank(message = "歌词格式不能为空")
    @Size(max = 32, message = "歌词格式最长32字符")
    @Pattern(regexp = "^[a-z0-9_]{1,32}$", message = "歌词格式仅支持小写字母、数字与下划线")
    private String format;

    @Schema(description = "歌词全文，格式由format字段解释")
    @NotBlank(message = "歌词内容不能为空")
    @Size(max = 200000, message = "歌词内容过长，最大200000字符")
    private String content;
}
