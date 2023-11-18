package com.inkneko.heimusic.controller;

import com.inkneko.heimusic.annotation.auth.UserAuth;
import com.inkneko.heimusic.config.HeiMusicConfig;
import com.inkneko.heimusic.errorcode.UserServiceErrorCode;
import com.inkneko.heimusic.model.entity.UserDetail;
import com.inkneko.heimusic.model.vo.IsExistsRootAccountVo;
import com.inkneko.heimusic.model.vo.Response;
import com.inkneko.heimusic.service.AuthService;
import com.inkneko.heimusic.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

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



    @UserAuth
    @GetMapping(value = "/nav")
    @Operation(summary = "获取导航信息", description = "在已登录状态下返回当前用户信息")
    public Response<UserDetail> nav(HttpServletRequest request) {
        Integer uid = (Integer) request.getAttribute("userId");
        UserDetail userDetail = userService.findUser(uid);
        return new Response<>(0, "ok", userDetail);
    }



}
