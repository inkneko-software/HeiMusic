package com.inkneko.heimusic.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * Redis 缓存配置：JSON 值序列化 + 全局 TTL 兜底
 * <p>
 * 不用 Boot 默认的 JDK 序列化，原因有二：
 * ① devtools 热部署下 jar 内序列化器解析实体类用的类加载器（app）与项目类
 * （RestartClassLoader）不一致，缓存命中即抛 "Music cannot be cast to Music"；
 * ② JDK 序列化绑定类结构，实体演进（改包名/字段类型）会使存量缓存全部反序列化失败。
 * JSON 序列化不依赖类身份，两者皆免。default typing 用 EVERYTHING 而非 NON_FINAL：
 * 缓存值含 final 包装类型（isFavorite 的 boolean、albumMusicNum 的数值），
 * NON_FINAL 不写 @class，Long 读回变 Integer 又是一颗转型雷。
 * 注意：TTL 在写入时生效，已存在的旧键不会被追溯赋 TTL
 */
@Configuration
public class CacheConfig {

    /**
     * 缓存条目默认存活时间
     */
    private static final Duration ENTRY_TTL = Duration.ofHours(24);

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheCustomizer() {
        return builder -> builder.cacheDefaults(
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(ENTRY_TTL)
                        //JSON 序列化器不识别 NullValue 哨兵，禁用 null 缓存（缺失 id 的查询不缓存，可接受）
                        .disableCachingNullValues()
                        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer())));
    }

    /**
     * 缓存值 JSON 序列化器（包内静态，供 CacheConfigTests 直接构造做往返验证）
     * <p>
     * findAndRegisterModules 注册 classpath 上的 Jackson SPI 模块（如 Java 时间），
     * 供将来实体引入时间字段时无需改此处
     */
    static GenericJackson2JsonRedisSerializer jsonSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(mapper);
    }
}
