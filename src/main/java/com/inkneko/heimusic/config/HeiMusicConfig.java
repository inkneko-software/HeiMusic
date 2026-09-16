package com.inkneko.heimusic.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class HeiMusicConfig {
    @Value("${heimusic.domain:}")
    private String domain;

    @Value("${heimusic.mail.from}")
    private String mailFrom;

    @Value("${heimusic.storage-type}")
    private String storageType;

    @Value("${heimusic.local.data-directory}")
    private String localDataDirectory;

    @Value("${heimusic.local.application-data-directory}")
    private String localApplicationDataDirectory;

    /**
     * 启动时是否自动创建管理账户（root 不存在时创建，初始密码输出到日志）。
     * 测试环境关闭以避免污染测试库。
     */
    @Value("${heimusic.root-auto-init:true}")
    private boolean rootAutoInit;


}
