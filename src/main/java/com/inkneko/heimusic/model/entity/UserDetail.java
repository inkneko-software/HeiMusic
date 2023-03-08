package com.inkneko.heimusic.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Date;


@Data
@Validated
public class UserDetail {
    @TableId
    private Integer userId;
    private String username;
    private String email;
    private String avatarUrl;
    private Date birth;
    private String gender;
    private String sign;
    private Date createdAt;
    private Date updatedAt;
}
