package com.inkneko.heimusic.controller;

import com.inkneko.heimusic.annotation.auth.UserAuth;
import com.inkneko.heimusic.config.HeiMusicConfig;
import com.inkneko.heimusic.config.MinIOConfig;
import com.inkneko.heimusic.errorcode.UserServiceErrorCode;
import com.inkneko.heimusic.model.dto.UpdateUserInfoDto;
import com.inkneko.heimusic.model.entity.UserDetail;
import com.inkneko.heimusic.model.vo.IsExistsRootAccountVo;
import com.inkneko.heimusic.model.vo.Response;
import com.inkneko.heimusic.model.vo.UserDetailVo;
import com.inkneko.heimusic.service.AuthService;
import com.inkneko.heimusic.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;


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

    @UserAuth
    @PostMapping(value = "/updateUserInfo")
    @Operation(summary = "更新用户资料", description = "用户名/生日/性别/个性签名为全量更新，字段传null视为清空（个性签名清空为空串）")
    public Response<UserDetailVo> updateUserInfo(@Valid @RequestBody UpdateUserInfoDto updateUserInfoDto, HttpServletRequest request) {
        Integer uid = (Integer) request.getAttribute("userId");
        userService.updateUserInfo(uid, updateUserInfoDto.getUsername(), updateUserInfoDto.getBirth(), updateUserInfoDto.getGender(), updateUserInfoDto.getSign());
        return new Response<>(0, "ok", new UserDetailVo(userService.findUser(uid), minIOConfig));
    }

    @UserAuth
    @PostMapping(value = "/updateAvatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "更新用户头像", description = "按文件实际内容校验类型，仅支持 JPEG/PNG/WebP/GIF，最大10MB")
    public Response<UserDetailVo> updateAvatar(@RequestParam("avatar") MultipartFile avatar, HttpServletRequest request) {
        Integer uid = (Integer) request.getAttribute("userId");
        userService.updateAvatar(uid, avatar);
        return new Response<>(0, "ok", new UserDetailVo(userService.findUser(uid), minIOConfig));
    }



}
