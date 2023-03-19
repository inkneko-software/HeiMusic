package com.inkneko.heimusic.controller;

import com.inkneko.heimusic.config.HeiMusicConfig;
import com.inkneko.heimusic.errorcode.AuthServiceErrorCode;
import com.inkneko.heimusic.model.dto.ResponseDto;
import com.inkneko.heimusic.service.AuthService;
import javafx.util.Pair;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.util.HashMap;
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

    @PostMapping(value = "/sendLoginEmailCode")
    public ResponseDto sendLoginEmailCode(@RequestParam @Email(message = "邮箱格式不正确") String email){
        authService.sendLoginEmail(email);
        return new ResponseDto(0, "邮件发送成功");
    }

    @RequestMapping(value = "/login")
    public ResponseDto login(@RequestParam @NotBlank(message = "邮箱不能为空") String email,
                             @RequestParam(required = false) String code,
                             @RequestParam(required = false) String password,
                             @RequestParam(required = false) Boolean client,
                             HttpServletResponse response) {
        Pair<Integer, String> pair;

        if (code != null){
            pair = authService.loginByEmailCode(email,code);
        }else if (password != null){
            pair = authService.login(email, password);
        }else {
            return new ResponseDto(AuthServiceErrorCode.PASSWORD_CODE_NOT_PROVIDED);
        }

        if (client == null || !client){
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

            return new ResponseDto(0, "登录成功");
        }
        Map<String, String> auth = new HashMap<>();
        auth.put("userId", pair.getKey().toString());
        auth.put("sessionId", pair.getValue());
        return new ResponseDto(0, "登录成功", auth);
    }
}
