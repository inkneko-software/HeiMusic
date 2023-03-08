package com.inkneko.heimusic.service.impl;

import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.model.entity.UserDetail;
import com.inkneko.heimusic.service.UserService;
import javafx.util.Pair;

public class UserServiceImpl implements UserService {
    @Override
    public String register(UserDetail userDetail) throws ServiceException {
        return null;
    }

    @Override
    public Boolean isEmailRegistered(String email) {
        return null;
    }

    @Override
    public void sendRegisterEmail(String email) throws ServiceException {

    }

    @Override
    public Pair<UserDetail, String> loginByEmail(String email, String password) {
        return null;
    }
}
