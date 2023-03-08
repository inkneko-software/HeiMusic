package com.inkneko.heimusic.service.impl;

import com.inkneko.heimusic.mapper.UserDetailMapper;
import com.inkneko.heimusic.model.entity.UserDetail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Date;

@SpringBootTest
public class AuthServiceImplTests {
    @Autowired
    UserDetailMapper userDetailMapper;
    @Test
    void registeredUserTestCase(){
        UserDetail detail = new UserDetail();
        detail.setUsername("inkneko");
        detail.setEmail("meleaf@qq.com");
        detail.setBirth(new Date(1999,6,29));
        detail.setSign("");
        detail.setGender("男");
        detail.setAvatarUrl("4355a46b19d348dc2f57c046f8ef63d4538ebb936000f3c9ee954a27460dd865.jpg");
        userDetailMapper.insert(detail);
    }
}
