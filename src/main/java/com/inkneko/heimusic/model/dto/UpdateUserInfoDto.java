package com.inkneko.heimusic.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "更新用户资料请求参数")
public class UpdateUserInfoDto {

    @Schema(description = "用户名，传null清空")
    @Size(max = 255, message = "用户名长度不能超过255")
    private String username;

    @Schema(description = "生日（yyyy-MM-dd），传null清空")
    @Past(message = "生日不能晚于当前时间")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birth;

    @Schema(description = "性别（m/f），传null清空")
    @Pattern(regexp = "^[mf]$", message = "性别取值仅支持 m/f")
    private String gender;

    @Schema(description = "个性签名，传null视为空串")
    @Size(max = 255, message = "个性签名长度不能超过255")
    private String sign;
}
