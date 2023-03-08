package com.inkneko.heimusic.filter;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;

public class AuthFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest)servletRequest;
        Cookie[] cookies = req.getCookies();
        if (cookies != null){
            Integer uid = null;
            for (Cookie cookie : cookies){
                if (cookie.getName().equals("uid")){
                    uid = Integer.parseInt(cookie.getValue());
                    servletRequest.setAttribute("uid", uid);
                    break;
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
