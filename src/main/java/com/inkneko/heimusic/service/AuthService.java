package com.inkneko.heimusic.service;

import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.model.entity.UserAuth;
import com.inkneko.heimusic.model.entity.UserDetail;
import javafx.util.Pair;

/**
 * 认证服务
 */
public interface AuthService {
    /**
     * 更新密码
     *
     * @param userId      用户id
     * @param newPassword 新密码
     * @return 新sessionId
     */
    String updatePassword(Integer userId, String newPassword) throws ServiceException;

    /**
     * 更新密码
     * @param userId 用户id
     * @param emailCode 验证码
     * @param newPassword 新密码
     * @return 新sessionId
     */
    String updatePasswordWithEmailCode(Integer userId, String emailCode, String newPassword) throws ServiceException;

    /**
     * 发送密码重置验证码
     * @param email 邮箱
     */
    void sendPasswordResetEmail(String email) throws ServiceException;

    /**
     * 向用户绑定邮箱发送密码重置验证码
     * @param userId 用户id
     */
    void sendPasswordResetEmail(Integer userId) throws ServiceException;

    /**
     * 更新密码，需提供当前使用的密码
     * @param userId 用户id
     * @param oldPassword 当前使用的密码
     * @param newPassword 新密码
     * @return 新sessionId
     * @throws ServiceException 业务异常
     */
    String updatePasswordWithOldPassword(Integer userId, String oldPassword, String newPassword) throws ServiceException;

    /**
     * 登出
     *
     * @param userId 用户id
     * @param sessionId  与之对应的sessionId
     */
    void logout(Integer userId, String sessionId) throws ServiceException;

    /**
     * 退出当前uid的所有session
     * @param uid 用户id
     */
    void logout(Integer uid);

    /**
     * 登录
     *
     * @param email    邮箱
     * @param password 密码
     * @return 若登录成功，返回(uid, sessionId)
     * @exception ServiceException 业务异常
     */
    Pair<Integer, String>  login(String email, String password) throws ServiceException;

    /**
     * 登录
     *
     * @param uid      uid
     * @param password 密码
     * @return 若登录成功，返回(uid, sessionId)
     */
    Pair<Integer, String> login(Integer uid, String password) throws ServiceException;

    /**
     * 通过邮箱验证码登录
     *
     * @param email 邮箱
     * @param code  验证码
     * @return 若登录成功，返回(uid, sessionId)
     */
    Pair<Integer, String>  loginByEmailCode(String email, String code) throws ServiceException;

    /**
     * 发送登录验证码
     *
     * @param email 邮箱
     * @throws ServiceException 业务异常
     */
    void sendLoginEmail(String email) throws ServiceException;

    /**
     * 登录
     *
     * @param uid 用户id
     * @return 若登录成功，返回(uid, sessionId)
     */
    Pair<Integer, String>  login(Integer uid) throws ServiceException;

    /**
     * 新建用户认证
     *
     * @param uid 用户id
     * @throws ServiceException 业务异常
     */
    void addUserAuth(Integer uid) throws ServiceException;

    /**
     * 根据SessionId查询UserId
     *
     * @param sessionId sessionId
     * @return 其所属userId
     */
    Integer findUserIdBySessionId(String sessionId);

}
