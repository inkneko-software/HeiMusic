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

    /**
     * 登录 cookie 是否标记 Secure（仅 HTTPS 部署时开启，HTTP 环境开启会导致浏览器拒绝种 cookie）
     */
    @Value("${heimusic.secure-cookie:false}")
    private boolean secureCookie;

    /**
     * LRCLIB 歌词数据源 API 地址
     */
    @Value("${heimusic.lrclib.base-url:https://lrclib.net}")
    private String lrclibBaseUrl;

    /**
     * LRCLIB 要求客户端以 User-Agent 标识应用（应用名 + 项目地址），缺失可能被拒绝服务
     */
    @Value("${heimusic.lrclib.user-agent:HeiMusic (https://github.com/leaf-lxh/heimusic)}")
    private String lrclibUserAgent;

    @Value("${heimusic.lrclib.connect-timeout-millis:5000}")
    private int lrclibConnectTimeoutMillis;

    @Value("${heimusic.lrclib.read-timeout-millis:10000}")
    private int lrclibReadTimeoutMillis;


}
