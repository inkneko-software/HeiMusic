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
    //redis maps:
    RMapCache<String, Integer> sessionIdMap;
    RMapCache<Integer, List<String>> uidSessionIdsMap;
    RMapCache<String, String> emailLoginCodeMap;
    RMapCache<String, String> emailPasswordResetCodeMap;

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

        uidSessionIdsMap  = redissonClient.getMapCache("auth_uid_sessionIds");
        sessionIdMap =  redissonClient.getMapCache("auth_session_uid");;
        emailLoginCodeMap = redissonClient.getMapCache("auth_email_login_code");
        emailPasswordResetCodeMap = redissonClient.getMapCache("auth_email_password_reset_code");
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
        if (!genAuthHash(oldPassword, auth.getAuthSalt()).equals(auth.getAuthHash())){
            throw new ServiceException(AuthServiceErrorCode.PASSWORD_INCORRECT);
        }
        return updatePassword(userId, newPassword);
    }

    @Override
    public String updatePasswordWithEmailCode(Integer userId, String emailCode, String newPassword) throws ServiceException {
        UserDetail userDetail = userDetailMapper.selectOne(new LambdaQueryWrapper<UserDetail>().eq(UserDetail::getUserId, userId));
        if (userDetail == null){
            throw new ServiceException(AuthServiceErrorCode.USER_NOT_EXISTS);
        }
        String code = emailPasswordResetCodeMap.get(userDetail.getEmail());
        if (code == null || !code.equals(emailCode)){
            throw new ServiceException(AuthServiceErrorCode.EMAIL_CODE_INCORRECT);
        }

        return updatePassword(userId, newPassword);
    }

    @Override
    public void sendPasswordResetEmail(String email) throws ServiceException {
        if (userDetailMapper.selectOne(new LambdaQueryWrapper<UserDetail>().eq(UserDetail::getEmail, email)) == null) {
            throw new ServiceException(AuthServiceErrorCode.USER_NOT_EXISTS);
        }

        if (emailPasswordResetCodeMap.get(email) != null) {
            if (emailPasswordResetCodeMap.remainTimeToLive(email) > 240 * 1000) {
                throw new ServiceException(AuthServiceErrorCode.EMAIL_CODE_OVER_LIMIT);
            }
        }
        String code = genEmailCode();
        emailPasswordResetCodeMap.put(email, code, 5, TimeUnit.MINUTES);
        asyncMailSender.send(
                heiMusicConfig.getMailFrom(),
                email,
                "【HeiMusic】密码重置",
                String.format("您的验证码为<span style='color: #3a62bf;'>%s</span>, 5分钟内有效", code),
                true);
    }

    @Override
    public void sendPasswordResetEmail(Integer userId) throws ServiceException {
        UserDetail userDetail = userDetailMapper.selectOne(new LambdaQueryWrapper<UserDetail>().eq(UserDetail::getUserId, userId));
        if ( userDetail == null) {
            throw new ServiceException(AuthServiceErrorCode.USER_NOT_EXISTS);
        }
        String email = userDetail.getEmail();
        if (emailPasswordResetCodeMap.get(email) != null) {
            if (emailPasswordResetCodeMap.remainTimeToLive(email) > 240 * 1000) {
                throw new ServiceException(AuthServiceErrorCode.EMAIL_CODE_OVER_LIMIT);
            }
        }
        String code = genEmailCode();
        emailPasswordResetCodeMap.put(email, code, 5, TimeUnit.MINUTES);
        asyncMailSender.send(
                heiMusicConfig.getMailFrom(),
                email,
                "【HeiMusic】密码重置",
                String.format("您的验证码为<span style='color: #3a62bf;'>%s</span>, 5分钟内有效", code),
                true);
    }

    @Override
    public String updatePassword(Integer userId, String newPassword) {
        UserAuth auth = userAuthMapper.selectOne(new LambdaQueryWrapper<UserAuth>().eq(UserAuth::getUserId, userId));
        auth.setAuthHash(genAuthHash(newPassword, auth.getAuthSalt()));
        userAuthMapper.updateById(auth);
        String sessionId = genSessionId(auth.getUserId(), auth.getAuthHash());
        sessionIdMap.putIfAbsent(sessionId, auth.getUserId(), 180, TimeUnit.DAYS);
        List<String> sessionIds = uidSessionIdsMap.get(userId);
        if (sessionIds== null){
            sessionIds = new LinkedList<String>();
        }
        sessionIds.add(sessionId);
        uidSessionIdsMap.put(userId, sessionIds);
        return sessionId;
    }

    @Override
    public void logout(Integer userId, String sessionId) {

        if (sessionIdMap.get(sessionId).equals(userId)) {
            sessionIdMap.remove(sessionId);
            List<String> sessionIds =  uidSessionIdsMap.get(userId);
            sessionIds.remove(sessionId);
            uidSessionIdsMap.put(userId, sessionIds);
        }
    }

    @Override
    public void logout(Integer uid){
        List<String> sessionIds =  uidSessionIdsMap.get(uid);
        for(String sessionId: sessionIds){
            sessionIdMap.remove(sessionId);
        }
        uidSessionIdsMap.remove(uid);
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
        String sessionId = genSessionId(uid, userAuth.getAuthHash());

        sessionIdMap.putIfAbsent(sessionId, uid, 180, TimeUnit.DAYS);
        List<String> sessionIds = uidSessionIdsMap.get(uid);
        if (sessionIds== null){
            sessionIds = new LinkedList<String>();
        }
        sessionIds.add(sessionId);
        uidSessionIdsMap.put(uid, sessionIds);
        return new Pair<>(userAuth.getUserId(), sessionId);
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
        return sessionIdMap.get(sessionId);
    }
}
