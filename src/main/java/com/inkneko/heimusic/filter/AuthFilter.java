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
        HttpServletRequest req = (HttpServletRequest) servletRequest;

        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            //fixme: 改成一次循环
            Optional<Cookie> uidCookie = Arrays.stream(cookies).filter(t -> "userId".equals(t.getName())).findFirst();
            Optional<Cookie> sessionIdCookie = Arrays.stream(cookies).filter(t -> "sessionId".equals(t.getName())).findFirst();
            if (uidCookie.isPresent() && sessionIdCookie.isPresent()) {
                String uid = uidCookie.get().getValue();
                String sessionId = sessionIdCookie.get().getValue();
                if (uid != null && sessionId != null) {
                    Integer matchedUid = authService.findUserIdBySessionId(sessionId);
                    if (matchedUid != null && uid.equals(matchedUid.toString())) {
                        try {
                            servletRequest.setAttribute("userId", Integer.parseInt(uid));
                            servletRequest.setAttribute("sessionId", sessionId);
                        } catch (Exception ignored) {

                        }
                    }
                }
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
