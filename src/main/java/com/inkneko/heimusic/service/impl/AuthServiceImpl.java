package com.inkneko.heimusic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inkneko.heimusic.config.HeiMusicConfig;
import com.inkneko.heimusic.errorcode.AuthServiceErrorCode;
import com.inkneko.heimusic.errorcode.AuthServiceErrorCode;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.mapper.UserAuthMapper;
import com.inkneko.heimusic.mapper.UserDetailMapper;
import com.inkneko.heimusic.mapper.UserRoleMapper;
import com.inkneko.heimusic.model.entity.UserAuth;
import com.inkneko.heimusic.model.entity.UserDetail;
import com.inkneko.heimusic.model.entity.UserRole;
import com.inkneko.heimusic.service.AuthService;
import com.inkneko.heimusic.service.UserService;
import com.inkneko.heimusic.util.mail.AsyncMailSender;
import org.apache.commons.codec.digest.DigestUtils;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {

    private final RedissonClient redissonClient;
    private final UserAuthMapper userAuthMapper;
    private final UserDetailMapper userDetailMapper;
    private final SecureRandom secureRandom;
    private final AsyncMailSender asyncMailSender;
    private final UserRoleMapper userRoleMapper;
    private final HeiMusicConfig heiMusicConfig;
    //redis maps:
    private final RMapCache<String, Integer> sessionIdMap;
    private final RMapCache<Integer, List<String>> uidSessionIdsMap;
    private final RMapCache<String, String> emailLoginCodeMap;
    private final  RMapCache<String, String> emailPasswordResetCodeMap;


    public AuthServiceImpl(RedissonClient redissonClient,
                           UserAuthMapper userAuthMapper,
                           UserDetailMapper userDetailMapper,
                           UserRoleMapper userRoleMapper,
                           AsyncMailSender asyncMailSender,
                           HeiMusicConfig heiMusicConfig) {
        this.redissonClient = redissonClient;
        this.userAuthMapper = userAuthMapper;
        this.userDetailMapper = userDetailMapper;
        secureRandom = new SecureRandom();
        this.asyncMailSender = asyncMailSender;
        this.heiMusicConfig = heiMusicConfig;
        this.userRoleMapper = userRoleMapper;

        uidSessionIdsMap  = redissonClient.getMapCache("auth_uid_sessionIds");
        sessionIdMap =  redissonClient.getMapCache("auth_session_uid");;
        emailLoginCodeMap = redissonClient.getMapCache("auth_email_login_code");
        emailPasswordResetCodeMap = redissonClient.getMapCache("auth_email_password_reset_code");
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
            throw new ServiceException(AuthServiceErrorCode.EMAIL_REGISTERED);
        }
        //检查是否在60秒内重复发送验证码
        RMapCache<String, String> emailRegCode = redissonClient.getMapCache("email_register_code");
        if (emailRegCode.get(targetEmail) != null) {
            if (emailRegCode.remainTimeToLive(targetEmail) > 240 * 1000) {
                throw new ServiceException(AuthServiceErrorCode.EMAIL_CODE_OVER_LIMIT);
            }
        }
        //生成验证码并发送
        String code = String.format("%06d", secureRandom.nextInt(1000000));
        emailRegCode.put(targetEmail, code, 5, TimeUnit.MINUTES);
        asyncMailSender.send(
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
            throw new ServiceException(AuthServiceErrorCode.EMAIL_CODE_INCORRECT);
        }
        try {
            userDetailMapper.insert(userDetail);

            UserAuth userAuth = new UserAuth();
            userAuth.setUserId(userDetail.getUserId());
            userAuth.setAuthHash("-");
            userAuth.setAuthSalt("-");
            userAuthMapper.insert(userAuth);
        } catch (DuplicateKeyException e) {
            throw new ServiceException(AuthServiceErrorCode.EMAIL_REGISTERED);
        }
    }

    /**
     * 创建管理账户
     *
     * @param email 用户名
     * @param password 密码
     * @throws ServiceException 业务异常
     */
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void createRootAccount(String email, String password) throws ServiceException {
        //检查管理账户是否存在
        if (isRootAccountExists()){
            throw new ServiceException(AuthServiceErrorCode.ROOT_ACCOUNT_EXISTS);
        }
        //尝试创建管理账户
        try {
            UserDetail userDetail =new UserDetail();
            userDetail.setUsername("neptune");
            userDetail.setEmail(email);
            userDetailMapper.insert(userDetail);

            addUserAuth(userDetail.getUserId());
            updatePassword(userDetail.getUserId(), password);
            UserRole userRole = new UserRole();
            userRole.setUserId(userDetail.getUserId());
            userRole.setUserRole("root");
            userRoleMapper.insert(userRole);
        }catch (DuplicateKeyException e){
            throw new ServiceException(AuthServiceErrorCode.EMAIL_REGISTERED);
        }
    }

    /**
     * 查询是否为管理账户
     *
     * @param userId 用户id
     * @return 返回是否为管理账户
     */
    @Override
    public boolean isRootAccount(Integer userId) {
        UserRole userRole = userRoleMapper.selectOne(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        return userRole != null && userRole.getUserRole().compareTo("root") == 0;
    }

    /**
     * 检查是否存在管理账户
     *
     * @return 返回是否存在
     * @throws ServiceException 业务异常
     */
    @Override
    public boolean isRootAccountExists() throws ServiceException {
        return userRoleMapper.selectOne(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserRole, "root")) != null;
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
        emailPasswordResetCodeMap.remove(userDetail.getEmail(), code);
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
        auth.setAuthSalt(UUID.randomUUID().toString().substring(0, 32));
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
    public Map.Entry<Integer, String>  login(String email, String password) {
        UserDetail userDetail = userDetailMapper.selectOne(new LambdaQueryWrapper<UserDetail>().eq(UserDetail::getEmail, email));
        if (userDetail == null) {
            throw new ServiceException(AuthServiceErrorCode.USER_NOT_EXISTS);
        }

        return login(userDetail.getUserId(), password);
    }

    @Override
    public Map.Entry<Integer, String>  login(Integer uid, String password) {
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
    public Map.Entry<Integer, String>  loginByEmailCode(String email, String code) throws ServiceException {
        UserDetail userDetail = null;
        if (userDetailMapper.selectOne(new LambdaQueryWrapper<UserDetail>().eq(UserDetail::getEmail, email)) == null) {
            //未注册用户，直接调用UserService发送注册验证码
            userDetail = new UserDetail();
            userDetail.setEmail(email);
            userDetail.setAvatarUrl("/public/images/default_avatar.jpg");
            register(userDetail, code);
        }else{
            String vaildCode = emailLoginCodeMap.get(email);
            if (vaildCode == null || !vaildCode.equals(code)) {
                throw new ServiceException(AuthServiceErrorCode.EMAIL_CODE_INCORRECT);
            }
            emailLoginCodeMap.remove(email);
            userDetail = userDetailMapper.selectOne(new LambdaQueryWrapper<UserDetail>().eq(UserDetail::getEmail, email));
        }
        return login(userDetail.getUserId());
    }

    @Override
    public void sendLoginEmail(String email) throws ServiceException {
        //未注册用户，直接调用UserService发送注册验证码
        if (userDetailMapper.selectOne(new LambdaQueryWrapper<UserDetail>().eq(UserDetail::getEmail, email)) == null) {
            sendRegisterEmail(email);
            return;
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
    public Map.Entry<Integer, String>  login(Integer uid) throws ServiceException {
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
        return new AbstractMap.SimpleEntry<>(userAuth.getUserId(), sessionId);
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
