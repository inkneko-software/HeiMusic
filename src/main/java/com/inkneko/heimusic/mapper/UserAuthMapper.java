package com.inkneko.heimusic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inkneko.heimusic.model.entity.UserAuth;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserAuthMapper extends BaseMapper<UserAuth> {
}
