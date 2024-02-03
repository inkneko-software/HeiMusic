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
    @Value("${heimusic.oss.endpoint}")
    String endpoint;
    @Value("${heimusic.oss.cdn:}")
    String cdn;
    @Value("${heimusic.oss.bucket}")
    String bucket;
    @Value("${heimusic.oss.accessKey}")
    String accessKey;
    @Value("${heimusic.oss.secretKey}")
    String secretKey;

}
