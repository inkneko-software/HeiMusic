package com.inkneko.heimusic.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRootAccountDto {
    @NotNull
    @Schema(description = "邮箱")
    private String email;
    @NotNull
    @Schema(description = "密码")
    private String password;
}
