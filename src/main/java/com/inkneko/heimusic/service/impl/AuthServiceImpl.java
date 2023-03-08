package com.inkneko.heimusic.service.impl;

import com.inkneko.heimusic.errorcode.AuthServiceErrorCode;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.mapper.UserDetailMapper;
import com.inkneko.heimusic.model.entity.UserDetail;
import com.inkneko.heimusic.service.AuthService;
import com.inkneko.heimusic.util.mail.AsyncMailSender;
import javafx.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private AsyncMailSender mailSender;

    @Autowired
    UserDetailMapper userDetailMapper;

    private final SecureRandom secureRandom = new SecureRandom();
    private static final String redisKeyEmailRegCode =  "email_reg_code_";
    private static final String redisKeySessionToken = "session_token_";
    @Override
    public String register(UserDetail userDetail) throws ServiceException {
//        if(userDetailMapper.selectOne(queryWrapper) != null){
//            throw new AuthServiceException("用户名已注册");
//        };

        return null;
    }

    @Override
    public Boolean isEmailRegistered(String email) {
        return null;
    }

    @Override
    public void sendRegisterEmail(String targetEmail) throws ServiceException {
        String key = redisKeyEmailRegCode + targetEmail;
        if (redisTemplate.opsForValue().get(key) != null){
            throw new ServiceException(AuthServiceErrorCode.AUTH_EMAIL_CODE_OVER_LIMIT);
        }

        String code = String.format("%06d", secureRandom.nextInt(1000000));
        redisTemplate.opsForValue().set(redisKeyEmailRegCode + targetEmail, code, Duration.ofMinutes(5));
        mailSender.send(
                "HeiMusic <heimusic@inkneko.com>",
                targetEmail,
                "【HeiMusic】注册验证",
                String.format("您的验证码为<span style='color: #3a62bf;'>%s</span>, 5分钟内有效", code),
                true
        );

    }

    @Override
    public Pair<UserDetail, String> loginByEmail(String email, String password) {
        return null;
    }

    @Override
    public String updatePassword(Integer userId, String newPassword) {
        return null;
    }

    @Override
    public void logout(Integer userId, String token) {

    }
}
