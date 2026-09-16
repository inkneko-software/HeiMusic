package com.inkneko.heimusic.service;

import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.mapper.UserAuthMapper;
import com.inkneko.heimusic.mapper.UserDetailMapper;
import com.inkneko.heimusic.model.entity.UserAuth;
import com.inkneko.heimusic.model.entity.UserDetail;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AuthService 集成测试（MySQL/Redis 为 WSL2 中的真实实例）。
 * 数据库写入由 @Transactional 在测试结束后回滚；
 * Redis 中的验证码与会话键在 @AfterEach 中按本测试用到的键定向清理。
 */
@SpringBootTest
@Transactional
class AuthServiceTests {

    @Autowired
    AuthService authService;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    UserDetailMapper userDetailMapper;

    @Autowired
    UserAuthMapper userAuthMapper;

    private String email;
    private final List<Integer> usedUids = new java.util.ArrayList<>();
    private final List<String> usedSessionIds = new java.util.ArrayList<>();
    private final List<String> usedEmails = new java.util.ArrayList<>();

    @BeforeEach
    void setUp() {
        //每个测试独立邮箱，避免 Redis 键冲突
        email = UUID.randomUUID() + "@test.example.com";
    }

    @AfterEach
    void cleanRedis() {
        RMapCache<String, String> registerCodeMap = redissonClient.getMapCache("email_register_code");
        RMapCache<String, String> loginCodeMap = redissonClient.getMapCache("auth_email_login_code");
        registerCodeMap.remove(email);
        loginCodeMap.remove(email);
        usedEmails.forEach(e -> {
            registerCodeMap.remove(e);
            loginCodeMap.remove(e);
        });
        usedEmails.clear();

        RMapCache<String, Integer> sessionMap = redissonClient.getMapCache("auth_session_uid");
        RMapCache<Integer, List<String>> uidSessionMap = redissonClient.getMapCache("auth_uid_sessionIds");
        for (String sessionId : usedSessionIds) {
            sessionMap.remove(sessionId);
        }
        //服务内部（如 createRootAccount -> updatePassword）创建的会话记录在 uid 索引里，一并清掉
        for (Integer uid : usedUids) {
            List<String> sessionIds = uidSessionMap.get(uid);
            if (sessionIds != null) {
                sessionIds.forEach(sessionMap::remove);
            }
            uidSessionMap.remove(uid);
        }
        usedSessionIds.clear();
        usedUids.clear();
    }

    /** 直接向 Redis 种入已知注册验证码，绕过邮件发送 */
    private void seedRegisterCode(String code) {
        redissonClient.<String, String>getMapCache("email_register_code").put(email, code, 5, TimeUnit.MINUTES);
    }

    private Integer registerUser() {
        seedRegisterCode("123456");
        UserDetail detail = new UserDetail();
        detail.setEmail(email);
        detail.setUsername("测试用户");
        authService.register(detail, "123456");
        usedUids.add(detail.getUserId());
        return detail.getUserId();
    }

    private String track(String sessionId) {
        usedSessionIds.add(sessionId);
        return sessionId;
    }

    @Test
    void registerAndLoginByPassword() {
        Integer uid = registerUser();

        assertTrue(authService.isEmailRegistered(email));
        assertNotNull(userDetailMapper.selectById(uid));

        //注册后 auth 记录为占位符，需先设置密码
        track(authService.updatePassword(uid, "Passw0rd"));

        assertThrowsServiceException(com.inkneko.heimusic.errorcode.AuthServiceErrorCode.PASSWORD_INCORRECT.getCode(),
                () -> authService.login(email, "wrong-password"));

        Map.Entry<Integer, String> loginResult = authService.login(email, "Passw0rd");
        assertEquals(uid, loginResult.getKey());
        track(loginResult.getValue());
        assertEquals(uid, authService.findUserIdBySessionId(loginResult.getValue()));

        authService.logout(uid, loginResult.getValue());
        assertNull(authService.findUserIdBySessionId(loginResult.getValue()));
    }

    @Test
    void logoutWithoutExistingSessionDoesNotThrow() {
        Integer uid = registerUser();

        //会话不存在（重复登出、会话已过期）时不应抛 NPE
        assertDoesNotThrow(() -> authService.logout(uid, "nonexistent-session"));
        //uid 没有任何会话索引（从未登录/已全部登出）时不应抛 NPE
        assertDoesNotThrow(() -> authService.logout(uid));

        //正常登出路径不受影响：单会话登出
        Map.Entry<Integer, String> loginResult = authService.login(uid);
        track(loginResult.getValue());
        authService.logout(uid, loginResult.getValue());
        assertNull(authService.findUserIdBySessionId(loginResult.getValue()));

        //全端登出
        Map.Entry<Integer, String> secondLogin = authService.login(uid);
        track(secondLogin.getValue());
        authService.logout(uid);
        assertNull(authService.findUserIdBySessionId(secondLogin.getValue()));
    }

    @Test
    void registerRejectsIncorrectCode() {
        seedRegisterCode("123456");
        UserDetail detail = new UserDetail();
        detail.setEmail(email);

        assertThrowsServiceException(com.inkneko.heimusic.errorcode.AuthServiceErrorCode.EMAIL_CODE_INCORRECT.getCode(),
                () -> authService.register(detail, "999999"));
        assertFalse(authService.isEmailRegistered(email));

        authService.register(detail, "123456");
        assertTrue(authService.isEmailRegistered(email));
    }

    @Test
    void registerRejectsMissingCode() {
        //从未发送验证码（或验证码已过期）时，应返回业务错误码而非 NPE
        UserDetail detail = new UserDetail();
        detail.setEmail(email);

        assertThrowsServiceException(com.inkneko.heimusic.errorcode.AuthServiceErrorCode.EMAIL_CODE_INCORRECT.getCode(),
                () -> authService.register(detail, "123456"));
        assertFalse(authService.isEmailRegistered(email));
    }

    @Test
    void registerEmailCodeRateLimit() {
        //首次发送：验证码写入 Redis，6 位数字
        authService.sendRegisterEmail(email);
        String code = redissonClient.<String, String>getMapCache("email_register_code").get(email);
        assertNotNull(code);
        assertTrue(code.matches("\\d{6}"), "验证码应为 6 位数字，实际: " + code);

        //60 秒内重复发送被拒绝
        assertThrowsServiceException(com.inkneko.heimusic.errorcode.AuthServiceErrorCode.EMAIL_CODE_OVER_LIMIT.getCode(),
                () -> authService.sendRegisterEmail(email));

        //已注册邮箱不能发送注册验证码
        registerUser();
        assertThrowsServiceException(com.inkneko.heimusic.errorcode.AuthServiceErrorCode.EMAIL_REGISTERED.getCode(),
                () -> authService.sendRegisterEmail(email));
    }

    @Test
    void loginByEmailCodeConsumesCode() {
        Integer uid = registerUser();
        redissonClient.<String, String>getMapCache("auth_email_login_code").put(email, "654321", 5, TimeUnit.MINUTES);

        assertThrowsServiceException(com.inkneko.heimusic.errorcode.AuthServiceErrorCode.EMAIL_CODE_INCORRECT.getCode(),
                () -> authService.loginByEmailCode(email, "111111"));

        Map.Entry<Integer, String> result = authService.loginByEmailCode(email, "654321");
        assertEquals(uid, result.getKey());
        track(result.getValue());

        //验证码一次性使用
        assertThrowsServiceException(com.inkneko.heimusic.errorcode.AuthServiceErrorCode.EMAIL_CODE_INCORRECT.getCode(),
                () -> authService.loginByEmailCode(email, "654321"));
    }

    @Test
    void rootAccountCanBeCreatedOnlyOnce() {
        assertFalse(authService.isRootAccountExists());

        authService.createRootAccount(email, "RootPassw0rd");
        assertTrue(authService.isRootAccountExists());

        UserDetail root = userDetailMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserDetail>()
                        .eq(UserDetail::getEmail, email));
        assertEquals("neptune", root.getUsername());
        assertTrue(authService.isRootAccount(root.getUserId()));
        usedUids.add(root.getUserId());

        //第二个 root 账户被拒绝，即使使用不同邮箱
        assertThrowsServiceException(com.inkneko.heimusic.errorcode.AuthServiceErrorCode.ROOT_ACCOUNT_EXISTS.getCode(),
                () -> authService.createRootAccount("another-" + email, "x"));
    }

    @Test
    void updatePasswordWithOldPasswordVerification() {
        Integer uid = registerUser();
        track(authService.updatePassword(uid, "OldPassw0rd"));

        assertThrowsServiceException(com.inkneko.heimusic.errorcode.AuthServiceErrorCode.PASSWORD_INCORRECT.getCode(),
                () -> authService.updatePasswordWithOldPassword(uid, "wrong", "NewPassw0rd"));

        track(authService.updatePasswordWithOldPassword(uid, "OldPassw0rd", "NewPassw0rd"));

        assertEquals(uid, authService.login(email, "NewPassw0rd").getKey());
        assertThrowsServiceException(com.inkneko.heimusic.errorcode.AuthServiceErrorCode.PASSWORD_INCORRECT.getCode(),
                () -> authService.login(email, "OldPassw0rd"));
    }

    @Test
    void legacySha1PasswordIsUpgradedToBcryptOnLogin() {
        Integer uid = registerUser();

        //手动构造历史格式凭证（加盐 SHA1），模拟生产环境存量数据
        String salt = UUID.randomUUID().toString().substring(0, 32);
        UserAuth auth = userAuthMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserAuth>()
                        .eq(UserAuth::getUserId, uid));
        auth.setAuthSalt(salt);
        auth.setAuthHash(DigestUtils.sha1Hex(String.format("9527-%s-%s", "LegacyPassw0rd", salt)));
        userAuthMapper.updateById(auth);

        //旧格式密码可正常登录，且登录后哈希透明升级为 bcrypt
        track(authService.login(uid, "LegacyPassw0rd").getValue());
        UserAuth upgraded = userAuthMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserAuth>()
                        .eq(UserAuth::getUserId, uid));
        assertTrue(upgraded.getAuthHash().startsWith("$2"), "登录后应重写为 bcrypt 哈希");
        assertEquals("-", upgraded.getAuthSalt());

        //升级后同一密码仍可登录，错误密码被拒绝
        track(authService.login(uid, "LegacyPassw0rd").getValue());
        assertThrowsServiceException(com.inkneko.heimusic.errorcode.AuthServiceErrorCode.PASSWORD_INCORRECT.getCode(),
                () -> authService.login(uid, "wrong-password"));
    }

    @Test
    void newPasswordIsStoredAsBcrypt() {
        Integer uid = registerUser();
        track(authService.updatePassword(uid, "Passw0rd"));

        UserAuth auth = userAuthMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserAuth>()
                        .eq(UserAuth::getUserId, uid));
        assertTrue(auth.getAuthHash().startsWith("$2"), "新设置密码应存储为 bcrypt 哈希");
        assertEquals("-", auth.getAuthSalt());
        //密文与明文不同，长度为 bcrypt 标准 60 字符
        assertNotEquals("Passw0rd", auth.getAuthHash());
        assertEquals(60, auth.getAuthHash().length());
    }

    @Test
    void updateEmailWithPasswordVerification() {
        Integer uid = registerUser();
        track(authService.updatePassword(uid, "Passw0rd"));

        //密码错误时拒绝
        assertThrowsServiceException(com.inkneko.heimusic.errorcode.AuthServiceErrorCode.PASSWORD_INCORRECT.getCode(),
                () -> authService.updateEmail(uid, "new-" + email, "wrong"));

        //目标邮箱已被其他用户占用时拒绝
        String otherEmail = UUID.randomUUID() + "@test.example.com";
        usedEmails.add(otherEmail);
        redissonClient.<String, String>getMapCache("email_register_code").put(otherEmail, "123456", 5, TimeUnit.MINUTES);
        UserDetail other = new UserDetail();
        other.setEmail(otherEmail);
        authService.register(other, "123456");
        usedUids.add(other.getUserId());
        assertThrowsServiceException(com.inkneko.heimusic.errorcode.AuthServiceErrorCode.EMAIL_REGISTERED.getCode(),
                () -> authService.updateEmail(uid, otherEmail, "Passw0rd"));

        //改成自己当前邮箱幂等成功
        assertDoesNotThrow(() -> authService.updateEmail(uid, email, "Passw0rd"));

        //密码验证通过后改邮箱成功，登录标识随之变更
        String newEmail = "new-" + email;
        authService.updateEmail(uid, newEmail, "Passw0rd");
        assertEquals(uid, userDetailMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserDetail>()
                        .eq(UserDetail::getEmail, newEmail)).getUserId());
        assertNull(userDetailMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserDetail>()
                        .eq(UserDetail::getEmail, email)));
        assertEquals(uid, authService.login(newEmail, "Passw0rd").getKey());
    }

    /** 断言抛出指定错误码的 ServiceException */
    private static void assertThrowsServiceException(int expectedCode, Runnable action) {
        ServiceException exception = assertThrows(ServiceException.class, action::run);
        assertEquals(expectedCode, exception.getCode(), "错误信息: " + exception.getMessage());
    }
}
