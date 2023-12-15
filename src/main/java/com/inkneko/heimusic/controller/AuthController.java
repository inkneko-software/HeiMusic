package com.inkneko.heimusic.controller;

import com.inkneko.heimusic.annotation.auth.UserAuth;
import com.inkneko.heimusic.config.HeiMusicConfig;
import com.inkneko.heimusic.errorcode.AuthServiceErrorCode;
import com.inkneko.heimusic.model.dto.CreateRootAccountDto;
import com.inkneko.heimusic.model.dto.LoginDto;
import com.inkneko.heimusic.model.entity.UserDetail;
import com.inkneko.heimusic.model.vo.IsExistsRootAccountVo;
import com.inkneko.heimusic.model.vo.Response;
import com.inkneko.heimusic.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {

    AuthService authService;
    HeiMusicConfig heiMusicConfig;

    public AuthController(AuthService authService, HeiMusicConfig heiMusicConfig) {
        this.authService = authService;
        this.heiMusicConfig = heiMusicConfig;
    }

    @PostMapping(value = "/sendRegisterEmail")
    public Response sendRegisterEmail(@RequestParam String email) {
        authService.sendRegisterEmail(email);
        return new Response(0, "发送成功");
    }

    @PostMapping(value = "/isEmailRegistered")
    public Response isEmailRegistered(@RequestParam String email) {
        if (authService.isEmailRegistered(email)) {
            return new Response(AuthServiceErrorCode.EMAIL_REGISTERED);
        }
        return new Response(0, "邮箱可用");
    }

    @PostMapping(value = "/register")
    public Response<?> register(@RequestParam String email, @RequestParam String code, HttpServletResponse response) {
        UserDetail userDetail = new UserDetail();
        userDetail.setEmail(email);
        userDetail.setAvatarUrl("/public/images/default_avatar.jpg");
        authService.register(userDetail, code);

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
        return new Response<>(0, "注册成功");
    }

    @PostMapping(value = "/sendLoginEmailCode")
    public Response<?> sendLoginEmailCode(@RequestParam @Email(message = "邮箱格式不正确") String email) {
        authService.sendLoginEmail(email);
        return new Response<>(0, "邮件发送成功");
    }

    @UserAuth(required = false)
    @PostMapping(value = "/sendPasswordResetEmailCode")
    public Response sendPasswordResetEmailCode(@RequestParam(required = false) String email, HttpServletRequest request) {
        if (email == null) {
            Integer userId = (Integer) request.getAttribute("userId");
            authService.sendPasswordResetEmail(userId);
        } else {
            authService.sendPasswordResetEmail(email);
        }

        return new Response(0, "发送成功");
    }

    @UserAuth
    @PostMapping(value = "/resetPassword")
    @Operation(summary = "重置密码", description = "在已登录状态下重置密码。需提供邮箱验证码")
    public Response resetPassword(@Parameter(description = "密码") @RequestParam String password, @Parameter(description = "邮箱验证码") @RequestParam String emailCode, HttpServletRequest request, HttpServletResponse response) {
        Integer userId = (Integer) request.getAttribute("userId");
        String newSessionId = authService.updatePasswordWithEmailCode(userId, emailCode, password);

        Cookie cookieSessionId = new Cookie("sessionId", newSessionId);
        cookieSessionId.setDomain(heiMusicConfig.getDomain());
        cookieSessionId.setMaxAge(60 * 60 * 24 * 180);
        cookieSessionId.setHttpOnly(true);
        cookieSessionId.setPath("/");
        Cookie cookieUserId = new Cookie("userId", String.valueOf(userId));
        cookieUserId.setDomain(heiMusicConfig.getDomain());
        cookieUserId.setMaxAge(60 * 60 * 24 * 180);
        cookieUserId.setHttpOnly(true);
        cookieUserId.setPath("/");

        response.addCookie(cookieSessionId);
        response.addCookie(cookieUserId);
        return new Response(0, "密码已更新");
    }

    @PostMapping(value = "/login")
    @Operation(summary = "登录", description = "使用验证码或密码登录，登录成功会setCookie，包括userId与sessionId两个字段")
    public Response<?> login(@Valid @RequestBody LoginDto loginDTO, HttpServletResponse response) {
        Map.Entry<Integer, String> pair;

        if (loginDTO.getCode() != null) {
            pair = authService.loginByEmailCode(loginDTO.getEmail(), loginDTO.getCode());
        } else if (loginDTO.getPassword() != null) {
            pair = authService.login(loginDTO.getEmail(), loginDTO.getPassword());
        } else {
            return new Response<>(AuthServiceErrorCode.PASSWORD_CODE_NOT_PROVIDED);
        }


        Cookie cookieSessionId = new Cookie("sessionId", pair.getValue());
        cookieSessionId.setDomain(heiMusicConfig.getDomain());
        cookieSessionId.setMaxAge(60 * 60 * 24 * 180);
        cookieSessionId.setHttpOnly(true);
        cookieSessionId.setPath("/");
        Cookie cookieUserId = new Cookie("userId", String.valueOf(pair.getKey()));
        cookieUserId.setDomain(heiMusicConfig.getDomain());
        cookieUserId.setMaxAge(60 * 60 * 24 * 180);
        cookieUserId.setHttpOnly(true);
        cookieUserId.setPath("/");

        response.addCookie(cookieSessionId);
        response.addCookie(cookieUserId);

        return new Response<>(0, "登录成功");
    }

    @PostMapping("/logout")
    @UserAuth
    @Operation(summary = "退出当前登录")
    public Response<?> logout(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        String sessionId = (String) request.getAttribute("sessionId");
        authService.logout(userId, sessionId);
        return new Response<>(0, "ok");
    }

    @GetMapping("/isExistsRootAccount")
    @Operation(summary = "检查是否存在管理账户")
    public Response<IsExistsRootAccountVo> isExistsRootAccount() {
        return new Response<>(0, "ok", new IsExistsRootAccountVo( authService.isRootAccountExists()));
    }

    @PostMapping("/createRootAccount")
    @Operation(summary = "创建唯一管理账户")
    public Response<?> createRootAccount(@RequestBody CreateRootAccountDto createRootAccountDto){
        authService.createRootAccount(createRootAccountDto.getEmail(), createRootAccountDto.getPassword());
        return new Response<>(0, "创建成功");
    }

}
