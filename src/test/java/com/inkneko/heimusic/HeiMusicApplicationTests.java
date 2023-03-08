package com.inkneko.heimusic;

import com.inkneko.heimusic.mapper.UserDetailMapper;
import com.inkneko.heimusic.model.entity.UserDetail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.security.SecureRandom;
import java.sql.Date;
import java.time.Duration;

@SpringBootTest
class HeiMusicApplicationTests {

    @Autowired
    UserDetailMapper userDetailMapper;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void emailCode(){
        SecureRandom secureRandom = new SecureRandom();
        for(int i = 0;i< 100; ++i){
            System.out.printf("%06d%n", secureRandom.nextInt(1000000));
        }
    }



}
