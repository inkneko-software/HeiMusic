package com.inkneko.heimusic.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class HeiMusicConfig {
    @Value("${heimusic.domain}")
    private String domain;

    @Value("${heimusic.mail.from}")
    private String mailFrom;

    @Value("${heimusic.storage-type}")
    private String storageType;

    @Value("${heimusic.local.data-directory}")
    private String localDataDirectory;

    @Value("${heimusic.local.application-data-directory}")
    private String localApplicationDataDirectory;

    @Value("${heimusic.local.url-prefix}")
    private String localUrlPrefix;


}
