package com.inkneko.heimusic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.model.entity.UserDetail;
import javafx.util.Pair;

public interface UserService {
    /**
     * 根据参数进行注册，注册成功则userDetail.userId为用户id
     * @param userDetail 用户信息
     * @param code 邮箱验证码
     * @throws ServiceException 业务异常
     */
    void register(UserDetail userDetail, String code) throws ServiceException;

    /**
     * 检查邮箱是否注册
     * @param email 邮箱
     * @return 邮箱是否已注册
     */
    boolean isEmailRegistered(String email) throws ServiceException;

    /**
     * 发送注册邮件至指定邮箱
     * @param email 邮箱
     * @throws ServiceException 业务异常
     */
    void sendRegisterEmail(String email) throws ServiceException;

    /**
     * 通过邮箱查询用户
     *
     * @param email
     * @return
     * @throws ServiceException
     */
    UserDetail findUser(String email) throws ServiceException;

    /**
     * 通过用户id查询用户
     * @param uid
     * @return
     * @throws ServiceException
     */
    UserDetail findUser(Integer uid) throws ServiceException;
}
