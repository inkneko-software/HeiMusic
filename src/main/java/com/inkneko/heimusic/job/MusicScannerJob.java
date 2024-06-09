package com.inkneko.heimusic.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inkneko.heimusic.config.HeiMusicConfig;
import com.inkneko.heimusic.model.entity.Music;
import com.inkneko.heimusic.service.AlbumService;
import com.inkneko.heimusic.service.ArtistService;
import com.inkneko.heimusic.service.MusicService;
import com.inkneko.heimusic.util.music.MusicScanner;
import com.inkneko.heimusic.util.music.model.Album;
import com.inkneko.heimusic.util.music.model.Track;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Component
@EnableScheduling
@Slf4j
public class MusicScannerJob implements SchedulingConfigurer {
    private static final Pattern ARTIST_PATTERN = Pattern.compile("(.+?)[,、&;]|feat.|(.+?)$");

    private final HeiMusicConfig heiMusicConfig;
    private final AlbumService albumService;
    private final MusicService musicService;
    private final ArtistService artistService;

    public MusicScannerJob(HeiMusicConfig heiMusicConfig, AlbumService albumService, MusicService musicService, ArtistService artistService) {
        this.heiMusicConfig = heiMusicConfig;
        this.albumService = albumService;
        this.musicService = musicService;
        this.artistService = artistService;
    }

    @Override
    public void configureTasks(@NotNull ScheduledTaskRegistrar taskRegistrar) {
        //taskRegistrar.addTriggerTask(this::process, triggerContext -> new CronTrigger("* * */14 * * *").nextExecutionTime(triggerContext));
    }

    public static AtomicBoolean isRunning = new AtomicBoolean(false);

    public void process() {
        log.info("音乐扫描任务开始执行");
        if (heiMusicConfig.getStorageType().compareTo("local") != 0) {
            return;
        }
        //判断音乐文件是否已扫描过。如果数据库中存在该音乐的路径，则认为该音乐已扫描过
        Predicate<File> isMusicScanedPredicate = file -> !musicService.getBaseMapper().selectList(new LambdaQueryWrapper<Music>().eq(Music::getFilePath, file.getAbsolutePath())).isEmpty();

        //对于扫描到的专辑的处理逻辑
        Consumer<Album> albumConsumer = (com.inkneko.heimusic.util.music.model.Album album) -> {
            //如果存在与数据库中，专辑标题与艺术家信息相同的专辑，则认为该专辑已扫描过
            com.inkneko.heimusic.model.entity.Album savedAlbum = albumService.getOne(
                    new LambdaQueryWrapper<com.inkneko.heimusic.model.entity.Album>()
                            .eq(com.inkneko.heimusic.model.entity.Album::getTitle, album.getTitle())
                            .eq(com.inkneko.heimusic.model.entity.Album::getAlbumArtist, album.getArtist())
            );
            if (savedAlbum == null) {
                //如果是新专辑，则创建专辑
                savedAlbum = new com.inkneko.heimusic.model.entity.Album(album.getTitle());
                savedAlbum.setAlbumArtist(album.getArtist());
                savedAlbum.setFrontCoverFilePath(album.getCoverFilePath());
                albumService.save(savedAlbum);
                if (album.getArtist() != null) {
                    List<String> artists = MusicScanner.parseArtists(album.getArtist());
                    albumService.addAlbumArtistWithNames(savedAlbum.getAlbumId(), artists);
                }
            }

            //将新扫描到的音乐，存储至专辑音乐列表。会根据碟片序号和音轨序号进行排序。递增顺序
            List<Integer> savedMusicList = new ArrayList<>();
            album.getTrackList().sort((a, b) -> {
                if (a.getDiscNumber() != null && b.getDiscNumber() != null && a.getDiscNumber().equals(b.getDiscNumber())) {
                    return a.getTrackNumber() - b.getTrackNumber();
                }
                if (a.getDiscNumber() != null && b.getDiscNumber() != null) {
                    return a.getDiscNumber() - b.getDiscNumber();
                }
                return 0;
            });
            //存储音乐对象
            for (Track track : album.getTrackList()) {
                Music music = new Music();
                music.setTitle(track.getTitle());
                music.setFilePath(track.getFilepath());
                music.setCodec(track.getFormatName());
                music.setDuration(track.getDuration());
                music.setSize(track.getSize());
                music.setTrackNumber(track.getTrackNumber());
                music.setTrackTotal(track.getTrackTotal());
                music.setDiscNumber(track.getDiscNumber());
                music.setDiscTotal(track.getDiscTotal());
                music.setArtist(track.getArtist());
                //截断过长的艺术家字符串
                if (track.getArtist() != null && track.getArtist().length() > 255){
                    music.setArtist(track.getArtist().substring(0, 255));
                }
                music.setDiscStartTime(track.getDiskStartTime());
                music.setDiscEndTime(track.getDiskEndTime());
                musicService.save(music);
                savedMusicList.add(music.getMusicId());
                log.info("专辑：{}，扫描到音乐{}", album.getTitle(), music);
                List<String> artists = MusicScanner.parseArtists(music.getArtist());
                musicService.addMusicArtistsWithName(music.getMusicId(), artists);
            }
            //添加到专辑
            albumService.addAlbumMusic(savedAlbum.getAlbumId(), savedMusicList);

        };

        //创建MusicScanner，并执行上面的逻辑
        MusicScanner musicScanner = new MusicScanner(heiMusicConfig);
        List<com.inkneko.heimusic.util.music.model.Album> albums = musicScanner.scanDirectory(
                new File(heiMusicConfig.getLocalDataDirectory()),
                isMusicScanedPredicate,
                albumConsumer
        );

        log.info("音乐扫描任务执行完毕");
        MusicScannerJob.isRunning.set(false);
    }
}
