package com.inkneko.heimusic;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class HeiMusicApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeiMusicApplication.class, args);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NotNull CorsRegistry registry) {
//                registry.addMapping("/api/**")
//                        .allowedHeaders("x-heimusic-auth-userid", "x-heimusic-auth-sessionid")
//                        .allowedOrigins("http://localhost:8888","app://.");
            }
        };
    }

}
