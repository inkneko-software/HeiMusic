package com.inkneko.heimusic.config;

import com.inkneko.heimusic.interceptor.AuthInterceptor;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {
    AuthInterceptor authInterceptor;
    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/**");
    }

    @Override
    public void addCorsMappings(@NotNull CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowCredentials(true)
//                        .allowedHeaders("x-heimusic-auth-userid", "x-heimusic-auth-sessionid")
                        // 前端 dev server 常通过局域网 IP 访问（如真机调试 http://192.168.x.x:8888），
                        // 代理转发会保留浏览器原始 Origin，因此白名单需覆盖私网地址段
                        .allowedOriginPatterns(
                                "http://localhost:8888",
                                "http://localhost:3000",
                                "app://.",
                                "http://localhost",
                                "https://localhost",
                                "capacitor://localhost",
                                "http://127.0.0.1:8888",
                                "http://192.168.*.*:8888",
                                "http://10.*.*.*:8888",
                                "http://172.16.*.*:8888"
                        );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //knife4j
        registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }


}
