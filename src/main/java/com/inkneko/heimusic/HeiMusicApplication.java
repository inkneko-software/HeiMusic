package com.inkneko.heimusic;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(description = "HeiMusic! 服务端API说明", version = "0.2.1"))
public class HeiMusicApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeiMusicApplication.class, args);
    }
}
