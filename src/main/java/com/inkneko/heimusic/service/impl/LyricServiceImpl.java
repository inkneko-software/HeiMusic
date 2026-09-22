package com.inkneko.heimusic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inkneko.heimusic.errorcode.LyricServiceErrorCode;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.mapper.LyricMapper;
import com.inkneko.heimusic.model.entity.Lyric;
import com.inkneko.heimusic.model.entity.Music;
import com.inkneko.heimusic.model.vo.LyricCoverageVo;
import com.inkneko.heimusic.service.LyricService;
import com.inkneko.heimusic.service.MusicService;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;

/**
 * lyric 表数据存取与缓存维护（拉取编排见 LyricFetchServiceImpl）
 */
@Service
public class LyricServiceImpl extends ServiceImpl<LyricMapper, Lyric> implements LyricService {

    MusicService musicService;
    CacheManager cacheManager;

    public LyricServiceImpl(MusicService musicService, CacheManager cacheManager) {
        this.musicService = musicService;
        this.cacheManager = cacheManager;
    }

    /**
     * 根据ID查询
     *
     * @param id 歌词id
     */
    @Override
    @Cacheable(cacheNames = "lyric", key = "#id")
    public Lyric getById(Serializable id) {
        return super.getById(id);
    }

    /**
     * 为音乐添加歌词
     * <p>
     * 唯一键(music_id, locale)拦截同语言重复添加，捕获DuplicateKeyException转为业务错误码
     *
     * @param musicId 音乐id
     * @param locale  语言标签
     * @param format  歌词格式
     * @param content 歌词全文
     * @return 新增的歌词（含自增id）
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "lyricList", key = "#musicId")
    public Lyric addLyric(Integer musicId, String locale, String format, String content) {
        if (musicService.getById(musicId) == null) {
            throw new ServiceException(LyricServiceErrorCode.LYRIC_MUSIC_NOT_FOUND);
        }
        Lyric lyric = new Lyric(null, musicId, content, normalizeLocale(locale), format, null, null);
        try {
            save(lyric);
        } catch (DuplicateKeyException e) {
            throw new ServiceException(LyricServiceErrorCode.LYRIC_ALREADY_EXISTS);
        }
        //提交后驱逐：注解驱逐在事务提交前执行，窗口内并发读会把旧列表回填缓存（经代理路径）；
        //同类内部调用（本类其他方法调 addLyric）不走代理时注解不生效，本驱逐是唯一保障
        TransactionAfterCommit.run(() -> evictLyricCaches(null, musicId));
        return lyric;
    }

    /**
     * 更新歌词的内容与格式
     *
     * @param lyricId 歌词id
     * @param format  歌词格式，null表示不变更
     * @param content 歌词内容，null表示不变更
     * @return 更新后的歌词
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "lyric", key = "#lyricId"),
            @CacheEvict(cacheNames = "lyricList", key = "#result.musicId")
    })
    public Lyric updateLyric(Integer lyricId, String format, String content) {
        if (format == null && content == null) {
            throw new ServiceException(LyricServiceErrorCode.LYRIC_UPDATE_EMPTY);
        }
        Lyric lyric = getById(lyricId);
        if (lyric == null) {
            throw new ServiceException(LyricServiceErrorCode.LYRIC_NOT_FOUND);
        }
        LambdaUpdateWrapper<Lyric> wrapper = new LambdaUpdateWrapper<Lyric>().eq(Lyric::getLyricId, lyricId);
        if (format != null) {
            wrapper.set(Lyric::getFormat, format);
        }
        if (content != null) {
            wrapper.set(Lyric::getContent, content);
        }
        update(wrapper);
        //回填内存对象，供缓存驱逐表达式与返回值使用
        lyric.setFormat(format != null ? format : lyric.getFormat());
        lyric.setContent(content != null ? content : lyric.getContent());
        return lyric;
    }

    /**
     * 删除歌词；若为默认歌词则同步清除music表引用
     *
     * @param lyricId 歌词id
     */
    @Override
    @Transactional
    public void removeLyric(Integer lyricId) {
        Lyric lyric = getById(lyricId);
        if (lyric == null) {
            throw new ServiceException(LyricServiceErrorCode.LYRIC_NOT_FOUND);
        }
        removeById(lyricId);
        clearDefaultReference(lyric.getMusicId(), lyricId);
        evictLyricCaches(lyric.getLyricId(), lyric.getMusicId());
    }

    /**
     * 删除某音乐下的全部歌词（删除音乐时级联调用）
     *
     * @param musicId 音乐id
     */
    @Override
    @Transactional
    public void removeByMusicId(Integer musicId) {
        List<Lyric> lyrics = list(new LambdaQueryWrapper<Lyric>().eq(Lyric::getMusicId, musicId));
        if (lyrics.isEmpty()) {
            return;
        }
        remove(new LambdaQueryWrapper<Lyric>().eq(Lyric::getMusicId, musicId));
        //被删除歌词中包含默认歌词时，清除music表引用
        Integer defaultLyricId = lyrics.stream()
                .filter(l -> l.getLyricId().equals(getDefaultLyricIdSafely(musicId)))
                .map(Lyric::getLyricId)
                .findFirst()
                .orElse(null);
        if (defaultLyricId != null) {
            musicService.updateDefaultLyric(musicId, null);
        }
        Cache lyricCache = cacheManager.getCache("lyric");
        if (lyricCache != null) {
            lyrics.forEach(l -> lyricCache.evict(l.getLyricId()));
        }
        evictLyricCaches(null, musicId);
    }

    /**
     * 查询某音乐的全部歌词
     *
     * @param musicId 音乐id
     * @return 歌词列表，按歌词id升序
     */
    @Override
    @Cacheable(cacheNames = "lyricList", key = "#musicId")
    public List<Lyric> getMusicLyrics(Integer musicId) {
        return list(new LambdaQueryWrapper<Lyric>()
                .eq(Lyric::getMusicId, musicId)
                .orderByAsc(Lyric::getLyricId));
    }

    /**
     * 查询某音乐指定语言的歌词
     *
     * @param musicId 音乐id
     * @param locale  语言标签
     * @return 歌词，不存在时返回null
     */
    @Override
    public Lyric getMusicLyricByLocale(Integer musicId, String locale) {
        return getOne(new LambdaQueryWrapper<Lyric>()
                .eq(Lyric::getMusicId, musicId)
                .eq(Lyric::getLocale, normalizeLocale(locale)));
    }

    /**
     * 设定/取消音乐的默认歌词
     *
     * @param musicId 音乐id
     * @param lyricId 默认歌词id，必须属于该音乐；null表示取消默认
     */
    @Override
    @Transactional
    public void setDefaultLyric(Integer musicId, Integer lyricId) {
        if (musicService.getById(musicId) == null) {
            throw new ServiceException(LyricServiceErrorCode.LYRIC_MUSIC_NOT_FOUND);
        }
        if (lyricId == null) {
            musicService.updateDefaultLyric(musicId, null);
            return;
        }
        Lyric lyric = getById(lyricId);
        if (lyric == null) {
            throw new ServiceException(LyricServiceErrorCode.LYRIC_NOT_FOUND);
        }
        if (!lyric.getMusicId().equals(musicId)) {
            throw new ServiceException(LyricServiceErrorCode.LYRIC_MUSIC_MISMATCH);
        }
        musicService.updateDefaultLyric(musicId, lyricId);
    }

    /**
     * 歌词覆盖率统计
     * <p>
     * 有歌词的音乐数按 DISTINCT music_id 计（同音乐多语言/翻译仅计一次），
     * 仅取 music_id 列做 selectObjs，避免把 content 大字段加载进内存
     */
    @Override
    public LyricCoverageVo getLyricCoverage() {
        long totalMusicCount = musicService.count();
        QueryWrapper<Lyric> wrapper = new QueryWrapper<Lyric>().select("DISTINCT music_id");
        long lyricMusicCount = listObjs(wrapper).size();
        return new LyricCoverageVo(totalMusicCount, lyricMusicCount);
    }

    /**
     * 若music的默认歌词指向lyricId则清除引用（删除歌词时调用）
     *
     * @param musicId 音乐id
     * @param lyricId 被删除的歌词id
     */
    private void clearDefaultReference(Integer musicId, Integer lyricId) {
        Music music = musicService.getById(musicId);
        if (music != null && lyricId.equals(music.getDefaultLyricId())) {
            musicService.updateDefaultLyric(musicId, null);
        }
    }

    /**
     * 读取music.default_lyric_id，音乐不存在时返回null（读侧对悬挂引用保持容忍，不抛错）
     *
     * @param musicId 音乐id
     * @return 默认歌词id或null
     */
    private Integer getDefaultLyricIdSafely(Integer musicId) {
        Music music = musicService.getById(musicId);
        return music == null ? null : music.getDefaultLyricId();
    }

    /**
     * 手动驱逐歌词相关缓存。
     * lyric缓存按lyricId为键、lyricList按musicId为键，删除类操作的musicId需查库才能获得，
     * 且removeByMusicId需逐条清理lyric缓存，注解无法表达，故在注解之外于此类方法内手动驱逐。
     *
     * @param lyricId 歌词id，null表示跳过
     * @param musicId 音乐id，null表示跳过
     */
    private void evictLyricCaches(Integer lyricId, Integer musicId) {
        if (lyricId != null) {
            Cache lyricCache = cacheManager.getCache("lyric");
            if (lyricCache != null) {
                lyricCache.evict(lyricId);
            }
        }
        if (musicId != null) {
            Cache lyricListCache = cacheManager.getCache("lyricList");
            if (lyricListCache != null) {
                lyricListCache.evict(musicId);
            }
        }
    }

    /**
     * locale归一化：去首尾空白、转小写、下划线转连字符，如 zh_CN -> zh-cn
     *
     * @param locale 原始语言标签
     * @return 归一化后的语言标签
     */
    private String normalizeLocale(String locale) {
        return locale == null ? null : locale.trim().toLowerCase().replace('_', '-');
    }
}
