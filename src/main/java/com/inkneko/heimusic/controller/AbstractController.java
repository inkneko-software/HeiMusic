package com.inkneko.heimusic.controller;

import com.inkneko.heimusic.annotation.auth.UserAuth;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AbstractController implements HandlerInterceptor {
}
