package com.inkneko.heimusic.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Data
@Component
@Configuration
public class MinIOConfig {
    @Value("${heimusic.minio.endpoint}")
    String endpoint;
    @Value("${heimusic.minio.region}")
    String region;
    @Value("${heimusic.minio.accessKey}")
    String accessKey;
    @Value("${heimusic.minio.secretKey}")
    String secretKey;
}
