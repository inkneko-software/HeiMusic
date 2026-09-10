package com.inkneko.heimusic.service;

import com.inkneko.heimusic.mapper.UserDetailMapper;
import com.inkneko.heimusic.model.entity.UserDetail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@Transactional
class UserServiceTests {

    @Autowired
    UserService userService;

    @Autowired
    UserDetailMapper userDetailMapper;

    @Test
    void findUserByEmailAndByUid() {
        String email = UUID.randomUUID() + "@test.example.com";
        UserDetail detail = new UserDetail();
        detail.setEmail(email);
        detail.setUsername("测试用户");
        userDetailMapper.insert(detail);

        UserDetail byEmail = userService.findUser(email);
        assertEquals(detail.getUserId(), byEmail.getUserId());
        assertEquals("测试用户", byEmail.getUsername());

        UserDetail byUid = userService.findUser(detail.getUserId());
        assertEquals(email, byUid.getEmail());
    }

    @Test
    void findUserReturnsNullWhenMissing() {
        assertNull(userService.findUser("nobody-" + UUID.randomUUID() + "@test.example.com"));
        assertNull(userService.findUser(-1));
    }
}
