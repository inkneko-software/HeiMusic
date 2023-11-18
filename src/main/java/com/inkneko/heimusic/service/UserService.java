package com.inkneko.heimusic.service;

import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.model.entity.UserDetail;

public interface UserService {


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
     *
     * @param uid
     * @return
     * @throws ServiceException
     */
    UserDetail findUser(Integer uid) throws ServiceException;
}
