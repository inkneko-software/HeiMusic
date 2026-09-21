package com.inkneko.heimusic.model.vo;

import com.inkneko.heimusic.config.MinIOConfig;
import com.inkneko.heimusic.model.entity.UserDetail;
import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

@Data
public class UserDetailVo {
    private Integer userId;
    private String username;
    private String email;
    private String avatarUrl;
    private LocalDate birth;
    private String gender;
    private String sign;
    private Date createdAt;
    private Date updatedAt;

    public UserDetailVo(UserDetail userDetail, MinIOConfig minIOConfig) {
        this.userId = userDetail.getUserId();
        this.username = userDetail.getUsername();
        this.email = userDetail.getEmail();
        this.avatarUrl = UserBasicVo.assembleAvatarUrl(userDetail, minIOConfig);
        this.birth = userDetail.getBirth();
        this.gender = userDetail.getGender();
        this.sign = userDetail.getSign();
        this.createdAt = userDetail.getCreatedAt();
        this.updatedAt = userDetail.getUpdatedAt();
    }
}
