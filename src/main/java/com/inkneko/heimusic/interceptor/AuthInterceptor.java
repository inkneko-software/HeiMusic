package com.inkneko.heimusic.interceptor;

import com.inkneko.heimusic.annotation.auth.UserAuth;
import com.inkneko.heimusic.service.AuthService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            UserAuth userAuthAnnotation = handlerMethod.getMethodAnnotation(UserAuth.class);
            if (userAuthAnnotation != null) {
                //如果UserAuth注解存在，则检查当前请求的Cookie，若登录状态有效，则setAttribute() userId与sessionId
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    Cookie uidCookie = null;
                    Cookie sessionIdCookie = null;
                    for (Cookie cookie : cookies) {
                        if (cookie.getName().equals("userId")) {
                            uidCookie = cookie;
                        } else if (cookie.getName().equals("sessionId")) {
                            sessionIdCookie = cookie;
                        }
                    }
                    if (uidCookie != null && sessionIdCookie != null) {
                        String uid = uidCookie.getValue();
                        String sessionId = sessionIdCookie.getValue();
                        if (uid != null && sessionId != null) {
                            //检查userId与sessionId是否匹配且有效
                            Integer matchedUid = authService.findUserIdBySessionId(sessionId);
                            if (matchedUid != null && uid.equals(matchedUid.toString())) {
                                //若需要管理权限，则再进行权限校验
                                if (userAuthAnnotation.requireRootPrivilege()) {
                                    if (authService.isRootAccount(Integer.parseInt(uid))){
                                        request.setAttribute("root", true);
                                    }else{
                                        response.setStatus(403);
                                        response.setContentType("application/json;charset=utf-8");
                                        response.getWriter().print("{code:403, msg:\"权限不足\"}");
                                        response.flushBuffer();
                                        return false;
                                    }
                                }
                                try {
                                    request.setAttribute("userId", Integer.parseInt(uid));
                                    request.setAttribute("sessionId", sessionId);
                                    return true;
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                }
                //若不要求必须登录
                if (!userAuthAnnotation.required()) {
                    //不要求登录但却要求管理权限，返回权限不足
                    if (userAuthAnnotation.requireRootPrivilege()) {
                        response.setStatus(403);
                        response.setContentType("application/json;charset=utf-8");
                        response.getWriter().print("{code:403, msg:\"权限不足\"}");
                        response.flushBuffer();
                        return false;
                    }
                    //否则通过
                    return true;
                }
                //否则拦截，返回403
                response.setStatus(403);
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().print("{code:403, msg:\"请先登录\"}");
                response.flushBuffer();
                return false;
            }
        }

        return true;
    }

//    @Override
//    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
//        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
//    }
//
//    @Override
//    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
//        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
//    }
}
