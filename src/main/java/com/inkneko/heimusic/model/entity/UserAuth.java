package com.inkneko.heimusic.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserAuth {
    @TableId
    Integer userId;
    String authHash;
    String authSalt;
    Date createdAt;
    Date updatedAt;
}
