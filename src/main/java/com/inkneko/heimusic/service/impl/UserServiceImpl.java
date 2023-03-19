package com.inkneko.heimusic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.inkneko.heimusic.errorcode.AuthServiceErrorCode;
import com.inkneko.heimusic.errorcode.UserServiceErrorCode;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.mapper.UserAuthMapper;
import com.inkneko.heimusic.mapper.UserDetailMapper;
import com.inkneko.heimusic.model.entity.UserAuth;
import com.inkneko.heimusic.model.entity.UserDetail;
import com.inkneko.heimusic.service.UserService;
import com.inkneko.heimusic.util.mail.AsyncMailSender;
import javafx.util.Pair;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {
    private final SecureRandom secureRandom = new SecureRandom();

    private final AsyncMailSender mailSender;
    private final UserDetailMapper userDetailMapper;
    private final UserAuthMapper userAuthMapper;
    private final RedissonClient redissonClient;

    @Autowired
    public UserServiceImpl(UserDetailMapper userDetailMapper, UserAuthMapper userAuthMapper, AsyncMailSender mailSender, RedissonClient redissonClient) {
        this.userDetailMapper = userDetailMapper;
        this.userAuthMapper = userAuthMapper;
        this.mailSender = mailSender;
        this.redissonClient = redissonClient;

    }

    @Override
    public boolean isEmailRegistered(String email) throws ServiceException {
        LambdaQueryWrapper<UserDetail> condition = new LambdaQueryWrapper<>();
        condition.eq(UserDetail::getEmail, email);
        return userDetailMapper.selectOne(condition) != null;
    }

    @Override
    public void sendRegisterEmail(String targetEmail) throws ServiceException {
        //检查是否已注册
        LambdaQueryWrapper<UserDetail> condition = new LambdaQueryWrapper<>();
        condition.eq(UserDetail::getEmail, targetEmail);
        if (userDetailMapper.selectOne(condition) != null) {
            throw new ServiceException(UserServiceErrorCode.EMAIL_REGISTERED);
        }
        //检查是否在60秒内重复发送验证码
        RMapCache<String, String> emailRegCode = redissonClient.getMapCache("email_register_code");
        if (emailRegCode.get(targetEmail) != null) {
            if (emailRegCode.remainTimeToLive(targetEmail) > 240 * 1000) {
                throw new ServiceException(UserServiceErrorCode.EMAIL_CODE_OVER_LIMIT);
            }
        }
        //生成验证码并发送
        String code = String.format("%06d", secureRandom.nextInt(1000000));
        emailRegCode.put(targetEmail, code, 5, TimeUnit.MINUTES);
        mailSender.send(
                "HeiMusic <heimusic@inkneko.com>",
                targetEmail,
                "【HeiMusic】注册验证",
                String.format("您的注册验证码为<span style='color: #3a62bf;'>%s</span>, 5分钟内有效", code),
                true
        );
    }

    @Override
    @Transactional
    public void register(UserDetail userDetail, String code) throws ServiceException {
        RMapCache<String, String> emailRegCode = redissonClient.getMapCache("email_register_code");
        if (!emailRegCode.get(userDetail.getEmail()).equals(code)) {
            throw new ServiceException(UserServiceErrorCode.EMAIL_CODE_INCORRENT);
        }
        try {
            userDetailMapper.insert(userDetail);

            UserAuth userAuth = new UserAuth();
            userAuth.setUserId(userDetail.getUserId());
            userAuth.setAuthHash("-");
            userAuth.setAuthSalt("-");
            userAuthMapper.insert(userAuth);
        } catch (DuplicateKeyException e) {
            throw new ServiceException(UserServiceErrorCode.EMAIL_REGISTERED);
        }
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
