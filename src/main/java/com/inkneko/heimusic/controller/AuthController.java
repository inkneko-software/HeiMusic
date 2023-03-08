package com.inkneko.heimusic.controller;

import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    AuthService authService;

    @RequestMapping(value = "/helloworld")
    public String helloWorld(HttpServletRequest request){
        Integer uid =(Integer)request.getAttribute("uid");
        if (uid != null){
            return "hello user uid=" + uid;
        }

        return "hello  world";
    }

    @RequestMapping(value="/users/{id}", method = RequestMethod.GET)
    @ResponseBody
    public String get(@PathVariable Integer id){
        return "your user: " + id;
    }

    @RequestMapping("/sendRegisterEmail")
    public Map<String, Object> sendRegisterEmail(@RequestParam String email){
        Map<String, Object> resp = new HashMap<>();
        try {
            authService.sendRegisterEmail(email);

        } catch (ServiceException e) {
            resp.put("code", 500);
            resp.put("msg", e.getMessage());
            return resp ;
        }
        resp.put("code", 200);
        resp.put("msg", "验证码已发送到邮箱");
        return resp;
    }

}
