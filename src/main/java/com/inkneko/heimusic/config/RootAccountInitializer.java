package com.inkneko.heimusic.config;

import com.inkneko.heimusic.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 应用启动时检查管理账户（root）是否存在，不存在则自动创建，初始密码仅在日志中输出一次。
 * 替代原先 POST /api/v1/auth/createRootAccount 的手动创建流程，消除部署后到创建前
 * 任何访问者可抢先创建 root 的窗口。
 * <p>
 * 自动创建的账户使用占位邮箱（.local 为保留 TLD，不会产生真实投递），root 首次登录后
 * 应通过修改邮箱接口换成真实邮箱，并通过旧密码修改初始密码。
 */
@Component
@ConditionalOnProperty(prefix = "heimusic", name = "root-auto-init", havingValue = "true", matchIfMissing = true)
public class RootAccountInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(RootAccountInitializer.class);

    /**
     * 初始密码字符集，剔除易混淆字符（0/O、1/l/I）
     */
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int PASSWORD_LENGTH = 16;

    private final AuthService authService;

    public RootAccountInitializer(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (authService.isRootAccountExists()) {
            return;
        }
        String placeholderEmail = "admin@heimusic.local";
        String initialPassword = genInitialPassword();
        authService.createRootAccount(placeholderEmail, initialPassword);
        logger.warn("================================================================");
        logger.warn("已自动创建管理账户（root），初始凭证如下，仅显示此一次：");
        logger.warn("  邮箱: {}", placeholderEmail);
        logger.warn("  密码: {}", initialPassword);
        logger.warn("请立即使用上述凭证登录，将邮箱修改为真实邮箱并修改密码。");
        logger.warn("================================================================");
    }

    private String genInitialPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            password.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        }
        return password.toString();
    }
}
