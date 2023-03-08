package com.inkneko.heimusic.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

@Data
public class UserAuth {
    @TableId
    Integer userId;
    String authHash;
    String authSalt;
    Date createdAt;
    Date updatedAt;
}
