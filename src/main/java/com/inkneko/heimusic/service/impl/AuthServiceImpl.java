package com.inkneko.heimusic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inkneko.heimusic.config.HeiMusicConfig;
import com.inkneko.heimusic.errorcode.AuthServiceErrorCode;
import com.inkneko.heimusic.errorcode.UserServiceErrorCode;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.mapper.UserAuthMapper;
import com.inkneko.heimusic.mapper.UserDetailMapper;
import com.inkneko.heimusic.model.entity.UserAuth;
import com.inkneko.heimusic.model.entity.UserDetail;
import com.inkneko.heimusic.service.AuthService;
import com.inkneko.heimusic.service.UserService;
import com.inkneko.heimusic.util.mail.AsyncMailSender;
import javafx.util.Pair;
import org.apache.commons.codec.digest.DigestUtils;
import org.redisson.api.RKeys;
import org.redisson.api.RMap;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {

    RedissonClient redissonClient;
    UserAuthMapper userAuthMapper;
    UserDetailMapper userDetailMapper;
    SecureRandom secureRandom;
    AsyncMailSender asyncMailSender;
    HeiMusicConfig heiMusicConfig;

    public AuthServiceImpl(RedissonClient redissonClient,
                           UserAuthMapper userAuthMapper,
                           UserDetailMapper userDetailMapper,
                           AsyncMailSender asyncMailSender,
                           HeiMusicConfig heiMusicConfig) {
        this.redissonClient = redissonClient;
        this.userAuthMapper = userAuthMapper;
        this.userDetailMapper = userDetailMapper;
        secureRandom = new SecureRandom();
        this.asyncMailSender = asyncMailSender;
        this.heiMusicConfig = heiMusicConfig;
    }

    /**
     * 根据密码与盐生成其Hash值
     *
     * @param password 密码
     * @param salt     盐
     * @return Hash值
     */
    private String genAuthHash(String password, String salt) {
        return DigestUtils.sha1Hex(String.format("9527-%s-%s", password, salt));
    }

    /**
     * 生成六位随机认证码，范围为[000000, 999999]
     * @return 认证码
     */
    private String genEmailCode() {
        return String.format("%06d", secureRandom.nextInt(1000000));
    }

    private String genSessionId(Integer uid, String authHash){
        String sessionId = String.format("%d-%s-%s-%d", uid, UUID.randomUUID().toString(), authHash, secureRandom.nextInt(1000000000));
        return DigestUtils.sha1Hex(sessionId);
    }

    @Override
    public String updatePasswordWithOldPassword(Integer userId, String oldPassword, String newPassword) throws ServiceException {
        UserAuth auth = userAuthMapper.selectOne(new LambdaQueryWrapper<UserAuth>().eq(UserAuth::getUserId, userId));
        if (auth == null){
            throw new ServiceException(AuthServiceErrorCode.USER_NOT_EXISTS);
        }
        RMapCache<String, Integer> sessionIdMap = redissonClient.getMapCache("auth_sessionid");
        if (!genAuthHash(oldPassword, auth.getAuthSalt()).equals(auth.getAuthHash())){
            throw new ServiceException(AuthServiceErrorCode.PASSWORD_INCORRECT);
        }
        auth.setAuthHash(genAuthHash(newPassword, auth.getAuthSalt()));
        userAuthMapper.updateById(auth);
        String sessionId = genSessionId(auth.getUserId(), auth.getAuthHash());
        sessionIdMap.putIfAbsent(sessionId, auth.getUserId(), 180, TimeUnit.DAYS);
        return sessionId;
    }

    @Override
    public String updatePassword(Integer userId, String newPassword) {
        return null;
    }

    @Override
    public void logout(Integer userId, String token) {
        RMapCache<String, Integer> sessionIdMap = redissonClient.getMapCache("auth_sessionid");
        if (sessionIdMap.get(token).equals(userId)) {
            sessionIdMap.remove(token);
        }
    }

    @Override
    public void logout(Integer uid){

    }

    @Override
    public Pair<Integer, String>  login(String email, String password) {
        UserDetail userDetail = userDetailMapper.selectOne(new LambdaQueryWrapper<UserDetail>().eq(UserDetail::getEmail, email));
        if (userDetail == null) {
            throw new ServiceException(AuthServiceErrorCode.USER_NOT_EXISTS);
        }

        return login(userDetail.getUserId(), password);
    }

    @Override
    public Pair<Integer, String>  login(Integer uid, String password) {
        UserAuth userAuth = userAuthMapper.selectOne(new LambdaQueryWrapper<UserAuth>().eq(UserAuth::getUserId, uid));
        if (userAuth == null) {
            throw new ServiceException(AuthServiceErrorCode.USER_NOT_EXISTS);
        }
        if (genAuthHash(password, userAuth.getAuthSalt()).equals(userAuth.getAuthHash())) {
            return login(userAuth.getUserId());
        }else{
            throw new ServiceException(AuthServiceErrorCode.PASSWORD_INCORRECT);
        }
    }

    @Override
    public Pair<Integer, String>  loginByEmailCode(String email, String code) throws ServiceException {
        RMapCache<String, String> emailLoginCodeMap = redissonClient.getMapCache("auth_email_login_code");
        String vaildCode = emailLoginCodeMap.get(email);
        if (vaildCode == null || !vaildCode.equals(code)) {
            throw new ServiceException(AuthServiceErrorCode.EMAIL_CODE_INCORRECT);
        }
        emailLoginCodeMap.remove(email);
        UserDetail userDetail = userDetailMapper.selectOne(new LambdaQueryWrapper<UserDetail>().eq(UserDetail::getEmail, email));
        return login(userDetail.getUserId());
    }

    @Override
    public void sendLoginEmail(String email) throws ServiceException {
        if (userDetailMapper.selectOne(new LambdaQueryWrapper<UserDetail>().eq(UserDetail::getEmail, email)) == null) {
            throw new ServiceException(AuthServiceErrorCode.USER_NOT_EXISTS);
        }

        RMapCache<String, String> emailLoginCodeMap = redissonClient.getMapCache("auth_email_login_code");
        if (emailLoginCodeMap.get(email) != null) {
            if (emailLoginCodeMap.remainTimeToLive(email) > 240 * 1000) {
                throw new ServiceException(AuthServiceErrorCode.EMAIL_CODE_OVER_LIMIT);
            }
        }
        String code = genEmailCode();
        emailLoginCodeMap.put(email, code, 5, TimeUnit.MINUTES);
        asyncMailSender.send(
                heiMusicConfig.getMailFrom(),
                email,
                "【HeiMusic】登录验证码",
                String.format("您的登录验证码为<span style='color: #3a62bf;'>%s</span>, 5分钟内有效", code),
                true);

    }

    @Override
    public Pair<Integer, String>  login(Integer uid) throws ServiceException {
        UserAuth userAuth = userAuthMapper.selectOne(new LambdaQueryWrapper<UserAuth>().eq(UserAuth::getUserId, uid));
        if (userAuth == null) {
            throw new ServiceException(AuthServiceErrorCode.USER_NOT_EXISTS);
        }
        String authHash = userAuth.getAuthHash();
        String sessionIdHashed = genSessionId(uid, userAuth.getAuthHash());

        RMapCache<String, Integer> sessionIdMap = redissonClient.getMapCache("auth_sessionid");
        sessionIdMap.putIfAbsent(sessionIdHashed, uid, 180, TimeUnit.DAYS);
        return new Pair<>(userAuth.getUserId(), sessionIdHashed);
    }

    @Override
    public void addUserAuth(Integer uid) throws ServiceException {
        UserAuth userAuth = new UserAuth();
        userAuth.setUserId(uid);
        userAuth.setAuthSalt("-");
        userAuth.setAuthHash("-");
        userAuthMapper.insert(userAuth);
    }

    @Override
    public Integer findUserIdBySessionId(String sessionId) {
        RMapCache<String, Integer> sessionIdMap = redissonClient.getMapCache("auth_sessionid");
        return sessionIdMap.get(sessionId);
    }
}
