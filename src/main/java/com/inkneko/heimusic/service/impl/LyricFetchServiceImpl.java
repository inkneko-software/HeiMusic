package com.inkneko.heimusic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.inkneko.heimusic.config.RabbitMQConfig;
import com.inkneko.heimusic.errorcode.LyricServiceErrorCode;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.mapper.AlbumMapper;
import com.inkneko.heimusic.mapper.AlbumMusicMapper;
import com.inkneko.heimusic.mapper.MusicMapper;
import com.inkneko.heimusic.model.entity.Album;
import com.inkneko.heimusic.model.entity.AlbumMusic;
import com.inkneko.heimusic.model.entity.Lyric;
import com.inkneko.heimusic.model.entity.LyricFetchLog;
import com.inkneko.heimusic.model.entity.Music;
import com.inkneko.heimusic.model.vo.LyricFetchVo;
import com.inkneko.heimusic.model.vo.LyricVo;
import com.inkneko.heimusic.rabbitmq.model.LyricFetchRequest;
import com.inkneko.heimusic.service.LyricFetchLogService;
import com.inkneko.heimusic.service.LyricFetchService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * LRCLIB 歌词拉取编排：只负责外部获取与流程控制，数据存取经 LyricService 完成
 * （跨 bean 调用走代理，addLyric 上的缓存/事务注解正常生效，
 * 避免编排与存取同类时内部调用旁路代理的坑）
 */
@Service
public class LyricFetchServiceImpl implements LyricFetchService {

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

    LyricService lyricService;
    MusicService musicService;
    LyricFetchLogService lyricFetchLogService;
    MusicMapper musicMapper;
    AlbumMapper albumMapper;
    AlbumMusicMapper albumMusicMapper;
    LrclibClient lrclibClient;
    LyricLanguageDetector lyricLanguageDetector;
    AmqpTemplate amqpTemplate;
    RedissonClient redissonClient;
    CacheManager cacheManager;

    public LyricFetchServiceImpl(LyricService lyricService, MusicService musicService,
                                 LyricFetchLogService lyricFetchLogService,
                                 MusicMapper musicMapper, AlbumMapper albumMapper, AlbumMusicMapper albumMusicMapper,
                                 LrclibClient lrclibClient, LyricLanguageDetector lyricLanguageDetector,
                                 AmqpTemplate amqpTemplate, RedissonClient redissonClient,
                                 CacheManager cacheManager) {
        this.lyricService = lyricService;
        this.musicService = musicService;
        this.lyricFetchLogService = lyricFetchLogService;
        this.musicMapper = musicMapper;
        this.albumMapper = albumMapper;
        this.albumMusicMapper = albumMusicMapper;
        this.lrclibClient = lrclibClient;
        this.lyricLanguageDetector = lyricLanguageDetector;
        this.amqpTemplate = amqpTemplate;
        this.redissonClient = redissonClient;
        this.cacheManager = cacheManager;
    }

    /**
     * 从 LRCLIB 拉取歌词，并记录任务日志（含跳过与失败结局）。
     * 日志经独立事务写入，主流程回滚不影响日志留存
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
        long lyricCount = lyricService.count(new LambdaQueryWrapper<Lyric>().eq(Lyric::getMusicId, musicId));
        if (lyricCount > 0) {
            throw new ServiceException(LyricServiceErrorCode.LYRIC_FETCH_ALREADY_HAS_LYRIC);
        }
        //纯音乐联动：仅当前为未知(NULL)时采信 LRCLIB，不覆盖人工标注；无数据(404)路径不触碰该列
        if (lockedMusic.getIsInstrumental() == null && track.getInstrumental() != null) {
            musicService.updateInstrumental(musicId, track.getInstrumental());
            //提交后驱逐 music 缓存（updateInstrumental 的注解驱逐在事务提交前执行）
            TransactionAfterCommit.run(() -> {
                Cache musicCache = cacheManager.getCache("music");
                if (musicCache != null) {
                    musicCache.evict(musicId);
                }
            });
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
        //显式 locale 优先（归一化由 addLyric 统一处理），否则逐行投票自动判定（仅作初始值，无法判定存 und）
        String finalLocale = locale != null && !locale.isBlank()
                ? locale
                : lyricLanguageDetector.detectLocale(content);
        Lyric lyric = lyricService.addLyric(musicId, finalLocale, format, content);
        lyricFetchLogService.record(source, musicId, LyricFetchLog.OUTCOME_CREATED, "locale=" + lyric.getLocale() + ", format=" + format);
        return new LyricFetchVo(LyricFetchLog.OUTCOME_CREATED, new LyricVo(lyric, getDefaultLyricIdSafely(musicId)));
    }

    /**
     * 一键扫描：查询全部"无歌词且非纯音乐"的音乐并投放拉取队列
     * <p>
     * 幂等性由两层保证：入队前排除已创建歌词的音乐（人工数据永不覆盖）；
     * 即使并发场景下重复入队，消费端"无歌词才拉取"校验也会自然跳过（5005）。
     * 60 秒节流窗口用 Redis CAS 原子实现，仅首个调用通过，防止 MQ 洪峰
     */
    @Override
    public int scanMissingLyric() {
        RBucket<String> throttle = redissonClient.getBucket(SCAN_THROTTLE_KEY);
        //trySet = SETNX + TTL：仅窗口内首个调用成功，原子防并发
        if (!throttle.trySet("1", SCAN_THROTTLE_SECONDS, TimeUnit.SECONDS)) {
            throw new ServiceException(LyricServiceErrorCode.LYRIC_SCAN_THROTTLED);
        }
        //仅取 music_id 列（DISTINCT），避免把 content 大字段整表加载进内存
        Set<Integer> hasLyricMusicIds = lyricService.listObjs(new QueryWrapper<Lyric>().select("DISTINCT music_id"))
                .stream().map(o -> (Integer) o).collect(Collectors.toSet());
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
     * 读取music.default_lyric_id，音乐不存在时返回null（读侧对悬挂引用保持容忍，不抛错）
     *
     * @param musicId 音乐id
     * @return 默认歌词id或null
     */
    private Integer getDefaultLyricIdSafely(Integer musicId) {
        Music music = musicService.getById(musicId);
        return music == null ? null : music.getDefaultLyricId();
    }
}
