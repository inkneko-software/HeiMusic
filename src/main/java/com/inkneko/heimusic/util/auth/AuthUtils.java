package com.inkneko.heimusic.util.auth;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

public class AuthUtils {
    public static Integer auth() {
        ServletRequestAttributes requestAttributes = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes());
        HttpServletRequest request;
        if (requestAttributes != null) {
            request = requestAttributes.getRequest();
            return (Integer) request.getAttribute("userId");
        }
        return null;

    }
}
