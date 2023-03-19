package com.inkneko.heimusic.controller;

import com.inkneko.heimusic.config.HeiMusicConfig;
import com.inkneko.heimusic.errorcode.UserServiceErrorCode;
import com.inkneko.heimusic.model.entity.UserDetail;
import com.inkneko.heimusic.model.dto.ResponseDto;
import com.inkneko.heimusic.service.AuthService;
import com.inkneko.heimusic.service.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@RestController
@RequestMapping(value = "/api/v1/user")
public class UserController {

    UserService userService;
    AuthService authService;
    HeiMusicConfig heiMusicConfig;

    public UserController(UserService userService, AuthService authService, HeiMusicConfig config) {
        this.userService = userService;
        this.authService = authService;
        this.heiMusicConfig = config;
    }

    @PostMapping(value = "/sendRegisterEmail")
    public ResponseDto sendRegisterEmail(@RequestParam String email) {
        userService.sendRegisterEmail(email);
        return new ResponseDto(0, "发送成功");
    }

    @PostMapping(value = "/isEmailRegistered")
    public ResponseDto isEmailRegistered(@RequestParam String email) {
        if (userService.isEmailRegistered(email)) {
            return new ResponseDto(UserServiceErrorCode.EMAIL_REGISTERED);
        }
        return new ResponseDto(0, "邮箱可用");
    }

    @PostMapping(value = "/register")
    public ResponseDto register(@RequestParam String email, @RequestParam String code, HttpServletResponse response) {
        UserDetail userDetail = new UserDetail();
        userDetail.setEmail(email);
        userDetail.setAvatarUrl("/public/images/default_avatar.jpg");
        userService.register(userDetail, code);

        String sessionId = authService.login(userDetail.getUserId()).getValue();
        Cookie cookieSessionId = new Cookie("sessionId", sessionId);
        cookieSessionId.setDomain(heiMusicConfig.getDomain());
        cookieSessionId.setMaxAge(60 * 60 * 24 * 180);
        cookieSessionId.setHttpOnly(true);
        cookieSessionId.setPath("/");
        Cookie cookieUserId = new Cookie("userId", String.valueOf(userDetail.getUserId()));
        cookieUserId.setDomain(heiMusicConfig.getDomain());
        cookieUserId.setMaxAge(60 * 60 * 24 * 180);
        cookieUserId.setHttpOnly(true);
        cookieUserId.setPath("/");

        response.addCookie(cookieSessionId);
        response.addCookie(cookieUserId);
        return new ResponseDto(0, "注册成功");
    }

    @GetMapping(value = "/nav")
    public ResponseDto nav(HttpServletRequest request){
        Integer uid =  (Integer) request.getAttribute("uid");
        if (uid != null){
            UserDetail userDetail = userService.findUser(uid);
            return new ResponseDto(0, "ok", userDetail);
        }

        return new ResponseDto(403, "请先登录");
    }

}
