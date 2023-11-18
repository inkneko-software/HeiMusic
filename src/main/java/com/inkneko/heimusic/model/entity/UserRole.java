package com.inkneko.heimusic.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Max;
import javax.validation.constraints.Pattern;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRole {
    @TableId
    Integer userId;
    @Pattern(regexp = "root|admin")
    String userRole;
    Date createdAt;
    Date updatedAt;
}
