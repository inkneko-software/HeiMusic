package com.inkneko.heimusic.model.dto;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "登录请求参数")
public class LoginDto {
    @NotNull
    @Parameter(description = "邮箱", required = true)
    private String email;
    @Parameter(description = "密码")
    private String password;
    @Parameter(description = "邮箱验证码")
    private String code;
}
