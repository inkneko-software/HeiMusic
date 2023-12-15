package com.inkneko.heimusic.model.dto;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "更新专辑请求参数")
public class UpdateAlbumInfoDto {
    @NotNull
    @Schema(description = "专辑id")
    private Integer albumId;

    @Schema(description = "专辑标题")
    private String title;

    @Schema(description = "封面")
    private MultipartFile cover;

    @Schema(description = "欲设置的专辑艺术家列表")
    private List<Integer> artistList;

    @Schema(description = "是否删除封面")
    private boolean deleteCover;
}
