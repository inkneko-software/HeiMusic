package com.inkneko.heimusic.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class HeiMusicConfig {
    @Value("${heimusic.domain}")
    private String domain;

    @Value("${heimusic.mail.from}")
    private String mailFrom;
}
