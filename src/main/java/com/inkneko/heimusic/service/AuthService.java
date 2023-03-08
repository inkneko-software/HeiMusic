package com.inkneko.heimusic.service;

import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.model.entity.UserDetail;
import javafx.util.Pair;

public interface AuthService{
    /**
     * 更新密码
     * @param userId 用户id
     * @param newPassword 新密码
     * @return 新token
     */
    String updatePassword(Integer userId, String newPassword);

    /**
     * 登出
     * @param userId 用户id
     * @param token 与之对应的token
     */
    void logout(Integer userId, String token);
}
