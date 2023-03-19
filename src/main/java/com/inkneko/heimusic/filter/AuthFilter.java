package com.inkneko.heimusic.filter;

import com.inkneko.heimusic.service.AuthService;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

@Component
public class AuthFilter implements Filter {

    AuthService authService;

    public AuthFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest)servletRequest;
        String uid = null ;
        String sessionId = null;
        //获取客户端发送的uid与sessionId，Cookie优先级更高
        Cookie[] cookies = req.getCookies();
        if (cookies != null){
            Optional<Cookie> uidCookie = Arrays.stream(cookies).filter(t->"userId".equals(t.getName())).findFirst();
            Optional<Cookie> sessionIdCookie = Arrays.stream(cookies).filter(t->"sessionId".equals(t.getName())).findFirst();
            if (uidCookie.isPresent() && sessionIdCookie.isPresent()){
                uid = uidCookie.get().getValue();
                sessionId = sessionIdCookie.get().getValue();
            }
        }else{
            uid = ((HttpServletRequest) servletRequest).getHeader("x-heimusic-auth-uid");
            sessionId = ((HttpServletRequest) servletRequest).getHeader("x-heimusic-auth-sid");
        }

        if (uid != null && sessionId != null){
            Integer matchedUid = authService.findUserIdBySessionId(sessionId);
            if (matchedUid != null && uid.equals(matchedUid.toString())){
                servletRequest.setAttribute("uid", Integer.parseInt(uid));
                servletRequest.setAttribute("sessionId", sessionId);
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
