package com.inkneko.heimusic.controller;

import com.inkneko.heimusic.annotation.auth.UserAuth;
import com.inkneko.heimusic.config.HeiMusicConfig;
import com.inkneko.heimusic.config.MinIOConfig;
import com.inkneko.heimusic.errorcode.UserServiceErrorCode;
import com.inkneko.heimusic.model.entity.UserDetail;
import com.inkneko.heimusic.model.vo.IsExistsRootAccountVo;
import com.inkneko.heimusic.model.vo.Response;
import com.inkneko.heimusic.model.vo.UserDetailVo;
import com.inkneko.heimusic.service.AuthService;
import com.inkneko.heimusic.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@RestController
@RequestMapping(value = "/api/v1/user")
public class UserController {

    UserService userService;
    AuthService authService;
    HeiMusicConfig heiMusicConfig;
    MinIOConfig minIOConfig;

    public UserController(UserService userService, AuthService authService, HeiMusicConfig config, MinIOConfig minIOConfig) {
        this.userService = userService;
        this.authService = authService;
        this.heiMusicConfig = config;
        this.minIOConfig = minIOConfig;
    }



    @UserAuth
    @GetMapping(value = "/nav")
    @Operation(summary = "获取导航信息", description = "在已登录状态下返回当前用户信息")
    public Response<UserDetailVo> nav(HttpServletRequest request) {
        Integer uid = (Integer) request.getAttribute("userId");
        UserDetail userDetail = userService.findUser(uid);
        return new Response<>(0, "ok", new UserDetailVo(userDetail, minIOConfig));
    }



}
