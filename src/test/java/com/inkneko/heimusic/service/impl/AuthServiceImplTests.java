package com.inkneko.heimusic.service.impl;

import com.inkneko.heimusic.config.HeiMusicConfig;
import com.inkneko.heimusic.mapper.UserDetailMapper;
import com.inkneko.heimusic.model.entity.UserDetail;
import org.junit.jupiter.api.Test;
import org.redisson.api.RKeys;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Date;
import java.util.concurrent.TimeUnit;

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

    @Autowired
    RedissonClient redissonClient;

    @Test
    void testTTL(){
        RMapCache<String,String> emailCode = redissonClient.getMapCache("email_ttl_test");
        emailCode.put("meleaf@qq.com", "123456", 5, TimeUnit.MINUTES);

        long a = emailCode.remainTimeToLive("meleaf@qq.com");
        RKeys keys= redissonClient.getKeys();
        long b = keys.remainTimeToLive("meleaf@qq.com");
        System.out.println(a);
        System.out.println(b);

    }

    @Autowired
    HeiMusicConfig config;
    @Test
    void configTest(){
        System.out.println(config.getDomain());
    }

}
