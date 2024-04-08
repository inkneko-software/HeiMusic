package com.inkneko.heimusic.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserBasicVo {
    private Integer userId;
    private String username;
    private String avatarUrl;
}
