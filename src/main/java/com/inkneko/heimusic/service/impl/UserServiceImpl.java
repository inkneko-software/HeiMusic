package com.inkneko.heimusic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inkneko.heimusic.errorcode.UserServiceErrorCode;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.mapper.UserAuthMapper;
import com.inkneko.heimusic.mapper.UserDetailMapper;
import com.inkneko.heimusic.model.entity.UserAuth;
import com.inkneko.heimusic.model.entity.UserDetail;
import com.inkneko.heimusic.service.AuthService;
import com.inkneko.heimusic.service.UserService;
import com.inkneko.heimusic.util.mail.AsyncMailSender;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {
    private final SecureRandom secureRandom = new SecureRandom();

    private final UserDetailMapper userDetailMapper;

    @Autowired
    public UserServiceImpl(UserDetailMapper userDetailMapper) {
        this.userDetailMapper = userDetailMapper;
    }

    @Override
    public UserDetail findUser(String email) {
        return userDetailMapper.selectOne(new LambdaQueryWrapper<UserDetail>().eq(UserDetail::getEmail, email));
    }

    @Override
    public UserDetail findUser(Integer uid) {
        return userDetailMapper.selectOne(new LambdaQueryWrapper<UserDetail>().eq(UserDetail::getUserId, uid));
    }
}
