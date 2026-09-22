package com.inkneko.heimusic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inkneko.heimusic.errorcode.LyricServiceErrorCode;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.mapper.AlbumMapper;
import com.inkneko.heimusic.mapper.AlbumMusicMapper;
import com.inkneko.heimusic.mapper.LyricMapper;
import com.inkneko.heimusic.mapper.MusicMapper;
import com.inkneko.heimusic.model.entity.Album;
import com.inkneko.heimusic.model.entity.AlbumMusic;
import com.inkneko.heimusic.model.entity.Lyric;
import com.inkneko.heimusic.model.entity.LyricFetchLog;
import com.inkneko.heimusic.model.entity.Music;
import com.inkneko.heimusic.model.vo.LyricCoverageVo;
import com.inkneko.heimusic.model.vo.LyricFetchVo;
import com.inkneko.heimusic.model.vo.LyricVo;
import com.inkneko.heimusic.config.RabbitMQConfig;
import com.inkneko.heimusic.rabbitmq.model.LyricFetchRequest;
import com.inkneko.heimusic.service.LyricFetchLogService;
import com.inkneko.heimusic.service.LyricService;
import com.inkneko.heimusic.service.MusicService;
import com.inkneko.heimusic.util.lrclib.LrclibClient;
import com.inkneko.heimusic.util.lrclib.LrclibTrack;
import com.inkneko.heimusic.util.lyric.LyricLanguageDetector;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.AmqpTemplate;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class LyricServiceImpl extends ServiceImpl<LyricMapper, Lyric> implements LyricService {

    /**
     * LRCLIB 签名匹配允许的最大时长（秒），超出范围时签名不携带该参数
     */
    private static final int LRCLIB_MAX_DURATION_SECONDS = 3600;

    /**
     * 一键扫描的节流窗口（秒）：窗口内重复调用直接拒绝，防止重复触发造成 MQ 洪峰
     */
    private static final long SCAN_THROTTLE_SECONDS = 60;

    /**
     * 一键扫描节流窗口的 Redis 键
     */
    private static final String SCAN_THROTTLE_KEY = "heimusic:lyric:scan_throttle";

    MusicService musicService;
    CacheManager cacheManager;
    LrclibClient lrclibClient;
    LyricLanguageDetector lyricLanguageDetector;
    LyricFetchLogService lyricFetchLogService;
    MusicMapper musicMapper;
    AlbumMapper albumMapper;
    AlbumMusicMapper albumMusicMapper;
    AmqpTemplate amqpTemplate;
    RedissonClient redissonClient;

    public LyricServiceImpl(MusicService musicService, CacheManager cacheManager,
                            LrclibClient lrclibClient, LyricLanguageDetector lyricLanguageDetector,
                            LyricFetchLogService lyricFetchLogService,
                            MusicMapper musicMapper, AlbumMapper albumMapper, AlbumMusicMapper albumMusicMapper,
                            AmqpTemplate amqpTemplate, RedissonClient redissonClient) {
        this.musicService = musicService;
        this.cacheManager = cacheManager;
        this.lrclibClient = lrclibClient;
        this.lyricLanguageDetector = lyricLanguageDetector;
        this.lyricFetchLogService = lyricFetchLogService;
        this.musicMapper = musicMapper;
        this.albumMapper = albumMapper;
        this.albumMusicMapper = albumMusicMapper;
        this.amqpTemplate = amqpTemplate;
        this.redissonClient = redissonClient;
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
     * 从 LRCLIB 拉取歌词，并记录任务日志（含跳过与失败结局）。
     * 日志经独立事务写入，主流程回滚不影响日志留存
     *
     * @param musicId 音乐id
     * @param locale  显式语言标签，null 时自动判定
     * @param source  来源标识：LyricFetchLog.SOURCE_MANUAL / SOURCE_MQ
     * @return 拉取结果
     */
    @Override
    @Transactional
    public LyricFetchVo fetchFromLrclib(Integer musicId, String locale, String source) {
        try {
            return doFetchFromLrclib(musicId, locale, source);
        } catch (ServiceException e) {
            //业务性跳过：音乐不存在/已有歌词（人工数据无条件优先）
            lyricFetchLogService.record(source, musicId, LyricFetchLog.OUTCOME_SKIPPED, e.getMessage());
            throw e;
        } catch (Exception e) {
            lyricFetchLogService.record(source, musicId, LyricFetchLog.OUTCOME_FAILED, String.valueOf(e.getMessage()));
            throw e;
        }
    }

    /**
     * 拉取核心流程
     * <p>
     * 仅服务无歌词的音乐，人工数据无条件优先。外部 HTTP 调用在锁外执行；
     * 随后 SELECT music ... FOR UPDATE 锁行，同事务内校验"无任何歌词"后写入，
     * 使拉取与手工添加在同音乐的写路径上串行化，杜绝覆盖。
     * InnoDB 行锁在语句执行时才获取，置于 HTTP 调用之后使持锁时长仅覆盖校验与插入
     */
    private LyricFetchVo doFetchFromLrclib(Integer musicId, String locale, String source) {
        Music music = musicService.getById(musicId);
        if (music == null) {
            throw new ServiceException(LyricServiceErrorCode.LYRIC_MUSIC_NOT_FOUND);
        }
        //锁外的外部调用：404 为正常结局（LRCLIB 会后台补录），事务内不产生任何写
        LrclibTrack track = lrclibClient.getBySignature(
                music.getTitle(), music.getArtist(), getAlbumTitle(musicId), parseDurationSeconds(music.getDuration()));
        if (track == null) {
            lyricFetchLogService.record(source, musicId, LyricFetchLog.OUTCOME_NOT_FOUND, "LRCLIB暂无该曲目");
            return new LyricFetchVo(LyricFetchLog.OUTCOME_NOT_FOUND, null);
        }
        //锁行重读：锁前的缓存读仅用于存在性与元数据，写路径校验与联动基于锁定行
        Music lockedMusic = musicMapper.selectOne(new LambdaQueryWrapper<Music>()
                .eq(Music::getMusicId, musicId)
                .last("FOR UPDATE"));
        long lyricCount = count(new LambdaQueryWrapper<Lyric>().eq(Lyric::getMusicId, musicId));
        if (lyricCount > 0) {
            throw new ServiceException(LyricServiceErrorCode.LYRIC_FETCH_ALREADY_HAS_LYRIC);
        }
        //纯音乐联动：仅当前为未知(NULL)时采信 LRCLIB，不覆盖人工标注；无数据(404)路径不触碰该列
        if (lockedMusic.getIsInstrumental() == null && track.getInstrumental() != null) {
            musicService.updateInstrumental(musicId, track.getInstrumental());
        }
        if (Boolean.TRUE.equals(track.getInstrumental())) {
            lyricFetchLogService.record(source, musicId, LyricFetchLog.OUTCOME_INSTRUMENTAL, "LRCLIB标记为纯音乐");
            return new LyricFetchVo(LyricFetchLog.OUTCOME_INSTRUMENTAL, null);
        }
        //内容取舍：优先带时间戳的 syncedLyrics(lrc)，无则纯文本(text)；同音乐同语言仅一份，不拆两条
        String content;
        String format;
        if (track.getSyncedLyrics() != null && !track.getSyncedLyrics().isBlank()) {
            content = track.getSyncedLyrics();
            format = "lrc";
        } else if (track.getPlainLyrics() != null && !track.getPlainLyrics().isBlank()) {
            content = track.getPlainLyrics();
            format = "text";
        } else {
            //LRCLIB 约定三字段全空即纯音乐，此处为脏数据兜底，按暂无歌词处理
            lyricFetchLogService.record(source, musicId, LyricFetchLog.OUTCOME_NOT_FOUND, "LRCLIB记录内容为空");
            return new LyricFetchVo(LyricFetchLog.OUTCOME_NOT_FOUND, null);
        }
        //显式 locale 优先，否则逐行投票自动判定（仅作初始值，无法判定存 und）
        String finalLocale = locale != null && !locale.isBlank()
                ? normalizeLocale(locale)
                : lyricLanguageDetector.detectLocale(content);
        Lyric lyric = addLyric(musicId, finalLocale, format, content);
        lyricFetchLogService.record(source, musicId, LyricFetchLog.OUTCOME_CREATED, "locale=" + finalLocale + ", format=" + format);
        return new LyricFetchVo(LyricFetchLog.OUTCOME_CREATED, new LyricVo(lyric, getDefaultLyricIdSafely(musicId)));
    }

    /**
     * 一键扫描：查询全部"无歌词且非纯音乐"的音乐并投放拉取队列
     * <p>
     * 幂等性由两层保证：入队前排除已创建歌词的音乐（人工数据永不覆盖）；
     * 即使并发场景下重复入队，消费端"无歌词才拉取"校验也会自然跳过（5005）。
     * 60 秒节流窗口用 Redis CAS 原子实现，仅首个调用通过，防止 MQ 洪峰
     *
     * @return 本次投放的音乐数
     */
    @Override
    public int scanMissingLyric() {
        RBucket<String> throttle = redissonClient.getBucket(SCAN_THROTTLE_KEY);
        //trySet = SETNX + TTL：仅窗口内首个调用成功，原子防并发
        if (!throttle.trySet("1", SCAN_THROTTLE_SECONDS, TimeUnit.SECONDS)) {
            throw new ServiceException(LyricServiceErrorCode.LYRIC_SCAN_THROTTLED);
        }
        Set<Integer> hasLyricMusicIds = list().stream()
                .map(Lyric::getMusicId)
                .collect(Collectors.toSet());
        //is_instrumental 为 NULL（未知）或 false 的音乐均参与拉取
        List<Music> targets = musicMapper.selectList(new LambdaQueryWrapper<Music>()
                        .and(w -> w.ne(Music::getIsInstrumental, true).or().isNull(Music::getIsInstrumental)))
                .stream()
                .filter(m -> !hasLyricMusicIds.contains(m.getMusicId()))
                .toList();
        for (Music music : targets) {
            LyricFetchRequest request = new LyricFetchRequest();
            request.setMusicId(music.getMusicId());
            amqpTemplate.convertAndSend(RabbitMQConfig.topicExchangeName, RabbitMQConfig.LyricFetch.routingKey, request);
        }
        return targets.size();
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
     * 查询音乐所属专辑名（LRCLIB 签名的可选参数，缺失只影响匹配精度不影响可用性）
     *
     * @param musicId 音乐id
     * @return 专辑名，无专辑关联时返回 null
     */
    private String getAlbumTitle(Integer musicId) {
        AlbumMusic albumMusic = albumMusicMapper.selectOne(new LambdaQueryWrapper<AlbumMusic>()
                .eq(AlbumMusic::getMusicId, musicId)
                .last("LIMIT 1"));
        if (albumMusic == null) {
            return null;
        }
        Album album = albumMapper.selectById(albumMusic.getAlbumId());
        return album == null ? null : album.getTitle();
    }

    /**
     * 解析 music.duration（ffprobe 原样输出，如 233.500000）为整数秒
     *
     * @param duration 原始时长字符串
     * @return 整数秒；缺失、非数值或超出 LRCLIB 允许范围(1~3600)时返回 null（签名中不携带该参数）
     */
    private Integer parseDurationSeconds(String duration) {
        if (duration == null || duration.isBlank()) {
            return null;
        }
        try {
            long seconds = Math.round(Double.parseDouble(duration));
            return seconds >= 1 && seconds <= LRCLIB_MAX_DURATION_SECONDS ? (int) seconds : null;
        } catch (NumberFormatException e) {
            return null;
        }
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
