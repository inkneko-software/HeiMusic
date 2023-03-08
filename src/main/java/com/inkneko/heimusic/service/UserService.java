package com.inkneko.heimusic.service;

import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.model.entity.UserDetail;
import javafx.util.Pair;

public interface UserService {
    /**
     * 根据参数进行注册，注册成功则userDetail.userId为用户id，并返回token
     * @param userDetail 用户信息
     * @return 用户认证令牌（token）
     */
    String register(UserDetail userDetail) throws ServiceException;

    /**
     * 检查邮箱是否注册
     * @param email
     * @return
     */
    Boolean isEmailRegistered(String email) throws ServiceException;

    /**
     * 发送注册邮件至指定邮箱
     * @param email
     * @throws ServiceException
     */
    void sendRegisterEmail(String email) throws ServiceException;

    /**
     * 邮箱登录
     * @param email 邮箱
     * @param password 密码
     * @return 登录成功返回用户信息和用户认证令牌（token）
     */
    Pair<UserDetail, String> loginByEmail(String email, String password);
}
