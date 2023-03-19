package com.inkneko.heimusic.config;

import com.inkneko.heimusic.filter.AuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@Component
public class AuthFilterConfig {

    AuthFilter authFilter;

    public AuthFilterConfig(AuthFilter authFilter) {
        this.authFilter = authFilter;
    }

    @Bean
    public FilterRegistrationBean<AuthFilter> registerFilter() {
        FilterRegistrationBean<AuthFilter> registration = new FilterRegistrationBean<AuthFilter>();
        registration.setFilter(authFilter);
        registration.addUrlPatterns("/*");
        registration.setName("AuthFilter");
        registration.setOrder(1);
        return registration;
    }
}
