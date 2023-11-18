package com.inkneko.heimusic.annotation.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指定某个被注解的Controller方法在被SpringMvc调用前需要进行登录状态检查
 * <p>
 * 在登录状态下会通过HttpServletRequest.setAttribute()方法，将userId与sessionId保存到当前请求中。
 * <p>
 * 在方法内部，通过getAttribute()方法进行相应值的获取
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UserAuth {
    /**
     * 指定是否必须在登录状态下访问该方法
     * 在未登录状态下，若required为true，则AuthInterceptor会拦截该请求，向请求方发送403状态码
     *               若required为false，则放行该请求，不会执行setAttribute操作
     */
    boolean required() default true;

    boolean requireRootPrivilege() default false;
}
