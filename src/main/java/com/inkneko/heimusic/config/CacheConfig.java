package com.inkneko.heimusic.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;

/**
 * Redis 缓存 TTL 配置
 * <p>
 * Boot 默认的 RedisCacheManager 条目永不过期，仅靠写操作驱逐：低频访问的缓存会长期驻留
 * Redis，服务端内存无界增长（缓存不在应用堆内，OOM 风险在 Redis 侧而非应用侧）。
 * 全局 TTL 作为兜底：数据一致性仍由写操作主动驱逐保证，TTL 仅限制
 * 漏驱逐场景下的脏数据存活窗口与内存上限。
 * 注意：TTL 在写入时生效，已存在的旧键不会被追溯赋 TTL
 */
@Configuration
public class CacheConfig {

    /**
     * 缓存条目默认存活时间
     */
    private static final Duration ENTRY_TTL = Duration.ofHours(24);

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheTtlCustomizer() {
        return builder -> builder.cacheDefaults(
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(ENTRY_TTL));
    }
}
