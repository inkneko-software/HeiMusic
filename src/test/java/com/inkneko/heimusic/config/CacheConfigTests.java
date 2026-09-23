package com.inkneko.heimusic.config;

import com.inkneko.heimusic.model.entity.Lyric;
import com.inkneko.heimusic.model.entity.Music;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CacheConfig JSON 序列化器单元测试：验证各类缓存值（实体、List、final 包装类型）
 * 经 Redis 序列化往返后类型与内容不丢失。背景：JDK 序列化在 devtools 下会产生
 * 跨类加载器的 ClassCastException，且 final 包装类型在非 EVERYTHING typing 下
 * 会丢失精确类型（Long 读回 Integer）
 */
class CacheConfigTests {

    GenericJackson2JsonRedisSerializer serializer = CacheConfig.jsonSerializer();

    private Object roundTrip(Object value) {
        return serializer.deserialize(serializer.serialize(value));
    }

    @Test
    void entityRoundTripKeepsTypeAndContent() {
        Music music = new Music();
        music.setMusicId(42);
        music.setTitle("类型往返测试");
        music.setArtist("艺术家");
        music.setDuration("233.500000");

        Object result = roundTrip(music);

        assertInstanceOf(Music.class, result);
        Music restored = (Music) result;
        assertEquals(42, restored.getMusicId());
        assertEquals("类型往返测试", restored.getTitle());
        assertEquals("233.500000", restored.getDuration());
    }

    @Test
    void listRoundTripKeepsElementType() {
        Lyric lyric = new Lyric(null, 42, "[00:17.12] 歌词内容", "zh-cn", "lrc", null, null);

        Object result = roundTrip(List.of(lyric));

        assertInstanceOf(List.class, result);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
        assertInstanceOf(Lyric.class, list.get(0));
        assertEquals("zh-cn", ((Lyric) list.get(0)).getLocale());
    }

    @Test
    void finalBoxedTypeRoundTripKeepsExactType() {
        //albumMusicNum 缓存数值、isFavorite 缓存 boolean：EVERYTHING typing 写入 @class，
        //否则 Long 会被 Jackson 还原成 Integer，代理返回处 checkcast Long 即 CCE
        Object longResult = roundTrip(12345678901L);
        assertInstanceOf(Long.class, longResult);
        assertEquals(12345678901L, longResult);

        assertEquals(Boolean.TRUE, roundTrip(Boolean.TRUE));
    }
}
