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
//                        .allowedHeaders("x-heimusic-auth-userid", "x-heimusic-auth-sessionid")
                        .allowedOrigins("http://localhost:8888", "app://.");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //knife4j
        registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }


}
