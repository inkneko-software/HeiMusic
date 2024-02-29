package com.inkneko.heimusic.util.music;

import com.inkneko.heimusic.config.HeiMusicConfig;
import com.inkneko.heimusic.util.music.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class MusicScanner {
    private static final Pattern ARTIST_PATTERN = Pattern.compile("(.+?)([,、&;]|feat.|Feat.)|(.+?)$");
    private static final Pattern TRACK_PATTERN = Pattern.compile("(\\d+)/(\\d+)|(\\d+)");
    private static final List<String> musicExtensions = Arrays.asList(".flac", ".mp3", ".ogg", ".tak", ".ape", ".wav");
    private static final List<String> imageExtensions = Arrays.asList(".jpg", ".png", ".bmp", ".webp");

    HeiMusicConfig heiMusicConfig;

    public MusicScanner(HeiMusicConfig heiMusicConfig) {
        this.heiMusicConfig = heiMusicConfig;
    }

    /**
     * 扫描音乐，从指定的root，自顶向下
     *
     * @param root               根目录
     * @param isMusicFileScanned 用于指定当前音乐文件是否已扫描过
     * @param albumConsumer      用于接收已扫描的专辑。
     * @return 专辑列表
     */
    public List<Album> scanDirectory(File root, Predicate<File> isMusicFileScanned, Consumer<Album> albumConsumer) {

        Map<String, Album> albumMap = new HashMap<>();
        List<Album> scannedAlbums = new ArrayList<>();
        Queue<File> pendingDirectories = new LinkedList<>();

        pendingDirectories.add(root);
        while (!pendingDirectories.isEmpty()) {
            File currentDir = pendingDirectories.poll();
            File[] subDirs = currentDir.listFiles(File::isDirectory);
            if (subDirs != null) {
                pendingDirectories.addAll(Arrays.asList(subDirs));
            }
            //扫描当前目录
            File[] files = currentDir.listFiles(File::isFile);
            if (files != null && files.length != 0) {
                //获取各个格式的文件列表
                List<File> cueFiles = new ArrayList<>();
                List<File> musicFiles = new ArrayList<>();
                List<File> imageFiles = new ArrayList<>();
                for (File file : files) {
                    String extension = getExtension(file);
                    if (extension.compareToIgnoreCase(".cue") == 0) {
                        cueFiles.add(file);
                    } else if (musicExtensions.contains(extension)) {
                        if (!isMusicFileScanned.test(file)) {
                            musicFiles.add(file);
                        }
                    } else if (imageExtensions.contains(extension)) {
                        imageFiles.add(file);
                    }
                }

                //如果当前目录的音乐文件都扫描过了（或没有音乐文件）则跳过
                if (musicFiles.isEmpty()){
                    continue;
                }

                List<Album> parsedAlbums = new ArrayList<>();
                //fuck CUE! 格式过于混乱，不知道是怎么生成的文件。
                //遇到了一个情况，音乐文件是提前分好的，CUE只是用来做索引。然后出现了一个FILE下有两个TRACK。
                //我只能认为生成CUE的软件多多少少有点毛病，尽量不依赖CUE
                if (!cueFiles.isEmpty() && musicFiles.size() <= cueFiles.size()) {
                    for (File cueFile : cueFiles) {
                        parsedAlbums.add(parseCueAlbum(musicFiles, imageFiles, cueFile));
                    }
                } else {
                    parsedAlbums.add(parseSplitedAlbum(musicFiles, imageFiles));
                }

                for (Album album : parsedAlbums) {
                    if (album != null) {
                        //处理一个专辑有多个碟片，并存储在不同文件夹的情况。通过专辑名称+艺术家名称来标识一个专辑
                        String key = album.getTitle() + album.getArtist();
                        Album tmp = albumMap.get(key);
                        if (tmp == null) {
                            tmp = album;
                            albumMap.put(key, album);
                            scannedAlbums.add(album);
                        } else {
                            //如果有同个专辑，则进行音乐列表的合并
                            tmp.getTrackList().addAll(album.getTrackList());
                        }
                        albumConsumer.accept(tmp);
                    }
                }

            }
        }
        return scannedAlbums;
    }

    /**
     * 通过当前目录中的音乐文件与CUE信息，提取为一个专辑
     *
     * @param musicFiles 音乐文件列表
     * @param imageFiles 图片文件列表
     * @param cueFile    CUE文件
     * @return 若分析成功，则返回专辑信息。否则返回null
     */
    private Album parseCueAlbum(List<File> musicFiles, List<File> imageFiles, File cueFile) {
        log.info("扫描到CUE，路径：{}", cueFile.getAbsolutePath());
        /*
         * 多个CUE + 多个音轨
         * 	对于每个CUE，作为一个专辑（实际上是一个专辑。但TITLE会将说明是第几个盘。所以留给用户处理合并）
         * 		每个CUE下会有一个或多个FILE。对于每个FILE，下面可能会有一个或多个TRACK。这些为实际的音乐
         * 			如果当前FILE下只有一个TRACK，则不进行转码
         * 			如果当前FILE下有多个TRACK，则需要转码
         *
         */
        try {
            String albumName = cueFile.getParentFile().getName();
            Album album = new Album(albumName, "", "", new ArrayList<>(), true);
            album.setTrackList(new ArrayList<>());

            CueParser cueParser = new CueParser();
            Cue cue = cueParser.parse(cueFile.getAbsolutePath());
            if (cue.getTitle() != null) {
                album.setTitle(cue.getTitle());
            }
            album.setArtist(cue.getPerformer());

            if (!imageFiles.isEmpty()) {
                List<File> filesNamedWithCover = imageFiles.stream()
                        .filter(filePath -> filePath.getName().toLowerCase().startsWith("cover"))
                        .toList();
                //优先寻找以cover开头的图片文件，如cover.jpg、cover01.jpg、Cover02.png
                if (!filesNamedWithCover.isEmpty()) {
                    album.setCoverFilePath(filesNamedWithCover.get(0).getAbsolutePath());
                } else {
                    //如果没有则从文件夹中选取一张
                    album.setCoverFilePath(imageFiles.get(0).getAbsolutePath());
                }
            }
            //对于每个FILE
            for (MusicFile musicFile : cue.getMusicFiles()) {
                boolean isMultiTracksFile = musicFile.getCueTracks().size() > 1;
                List<Track> trackList = new ArrayList<>();
                for (CueTrack cueTrack : musicFile.getCueTracks()) {
                    Track track = new Track();
                    track.setTrackNumber(cueTrack.getTrackNumber());
                    track.setArtist(cueTrack.getPerformer());
                    track.setTitle(cueTrack.getTitle());
                    File musicFilePath = new File(cueFile.getParentFile(), musicFile.getFilename());
                    track.setFilepath(musicFilePath.getAbsolutePath());
                    //如果是多个音乐在一个音乐文件内，则设置起始时间和结束时间
                    if (isMultiTracksFile) {
                        track.setDiskStartTime(track.getDiskStartTime());
                        track.setDiskEndTime(track.getDiskEndTime());
                    } else {
                        //如果是每个音乐为一个文件的情形，考虑使用文件中的tag信息
                        ProbeResult result = MusicProber.probe(musicFilePath);
                        Format format = result.getFormat();
                        Tags tags = format.getTags();
                        if (tags != null) {
                            track.setArtist(tags.getArtist());
                            if (tags.getTitle() != null) {
                                track.setTitle(tags.getTitle());
                            }
                            if (tags.getAlbumArtist() != null) {
                                album.setArtist(tags.getAlbumArtist());
                            }
                            track.setBitrate(format.getBitrate());
                            track.setDuration(format.getDuration());
                            track.setFormatName(format.getFormatName());
                            track.setSize(format.getSize());
                            try {
                                if (tags.getTrack() != null) {
                                    track.setTrackNumber(parseTrackNumber(tags.getTrack()));
                                }
                                if (tags.getTrackTotal() != null) {
                                    track.setTrackTotal(parseTrackNumber(tags.getTrackTotal()));
                                }
                                if (tags.getDisc() != null) {
                                    track.setDiscNumber(parseTrackNumber(tags.getDisc()));
                                }
                                if (tags.getDiscTotal() != null) {
                                    track.setDiscTotal(parseTrackNumber(tags.getDiscTotal()));
                                }
                            } catch (NumberFormatException e) {
                                log.error("解析Track/Disc编号时出现错误，原因：{}，文件路径: {}", e.getMessage(), musicFilePath.getAbsolutePath());
                            }
                        }
                    }
                    trackList.add(track);
                }
                album.getTrackList().addAll(trackList);
            }
            return album;
        } catch (IOException e) {
            log.error("处理CUE专辑时出现读写错误", e);
            return null;
        } catch (InterruptedException e) {
            log.error("处理CUE专辑时被中断", e);
            return null;
        }
    }

    /**
     * 通过当前目录中的音乐文件信息，提取为一个专辑
     *
     * @param musicFiles 音乐文件列表
     * @param imageFiles 图片文件列表
     * @return 若分析成功，则返回专辑信息。否则返回null
     */
    private Album parseSplitedAlbum(List<File> musicFiles, List<File> imageFiles) {
        if (musicFiles.isEmpty()) {
            return null;
        }
        File firstMusicFile = musicFiles.get(0);
        try {
            //通过ffprobe提取音乐标签信息
            ProbeResult result = MusicProber.probe(firstMusicFile);
            List<com.inkneko.heimusic.util.music.model.Stream> streams = result.getStreams();
            Format format = result.getFormat();
            Tags tags = format.getTags();
            //专辑默认名称为当前文件夹的名称
            String albumName = firstMusicFile.getParentFile().getName();
            if (tags != null && tags.getAlbum() != null && !tags.getAlbum().isEmpty()) {
                //若音乐信息中存在专辑信息，则进行提权
                albumName = tags.getAlbum();
            }
            Album album = new Album(albumName, "", "", new ArrayList<>(), false);
            //获取封面，先尝试从当前文件夹获取。
            if (!imageFiles.isEmpty()) {
                List<File> filesNamedWithCover = imageFiles.stream()
                        .filter(filePath -> filePath.getName().toLowerCase().startsWith("cover"))
                        .toList();
                //优先寻找以cover开头的图片文件，如cover.jpg、cover01.jpg、Cover02.png
                if (!filesNamedWithCover.isEmpty()) {
                    album.setCoverFilePath(filesNamedWithCover.get(0).getAbsolutePath());
                } else {
                    //如果没有则从文件夹中选取一张
                    album.setCoverFilePath(imageFiles.get(0).getAbsolutePath());
                }
            } else if (streams.size() > 1) {
                //若没有封面文件，则尝试从音乐中提取
                for (com.inkneko.heimusic.util.music.model.Stream musicStream : streams) {
                    if (musicStream.getCodecType().compareToIgnoreCase("audio") != 0) {
                        String coverExtension = null;
                        if (musicStream.getCodecName().compareTo("mjpeg") != 0) {
                            coverExtension = "jpg";
                        } else if (musicStream.getCodecName().compareTo("png") != 0) {
                            coverExtension = "png";
                        } else if (musicStream.getCodecName().compareTo("bmp") != 0) {
                            coverExtension = "bmp";
                        } else if (musicStream.getCodecName().compareTo("webp") != 0) {
                            coverExtension = "webp";
                        } else {
                            log.info("未知封面图片类型：{}", musicStream.getCodecName());
                        }
                        if (coverExtension != null) {
                            String coverBaseDir = heiMusicConfig.getLocalApplicationDataDirectory();
                            File file = new File(String.format("%s%s%s", coverBaseDir, File.separator, "cover"));
                            if (!file.exists() && !file.mkdirs()) {
                                log.error("创建封面文件夹失败，路径：{}", file.getAbsolutePath());
                            }
                            File coverPath = new File(String.format("%s%s%s", coverBaseDir, File.separator, "cover"), String.format("%s.%s", DigestUtils.sha1Hex(firstMusicFile.getAbsolutePath()), coverExtension));
                            if (!coverPath.exists()) {
                                try {
                                    extractCover(firstMusicFile, coverPath);
                                    log.info("提取到专辑封面，编码：{}，类型{}，下标：{}，专辑：{}，图片保存路径：{}", musicStream.getCodecName(), musicStream.getCodecType(), musicStream.getIndex(), album.getTitle(), coverPath.getAbsolutePath());
                                } catch (IOException e) {
                                    log.error("提取专辑封面时出现IOException，音乐文件路径：{}", firstMusicFile.getAbsolutePath(), e);
                                    break;
                                } catch (InterruptedException e) {
                                    log.error("提取专辑封面时被中断", e);
                                    break;
                                }
                            }
                            album.setCoverFilePath(coverPath.getAbsolutePath());
                            break;
                        }
                    }
                }
            }
            //向专辑添加音乐
            for (File musicFile : musicFiles) {
                result = MusicProber.probe(musicFile);
                format = result.getFormat();
                tags = format.getTags();

                Track track = new Track();
                track.setFilepath(musicFile.getAbsolutePath());
                track.setTitle(musicFile.getName());
                if (tags != null) {
                    track.setArtist(tags.getArtist());
                    if (tags.getTitle() != null) {
                        track.setTitle(tags.getTitle());
                    }
                    if (tags.getAlbumArtist() != null) {
                        album.setArtist(tags.getAlbumArtist());
                    }
                    track.setBitrate(format.getBitrate());
                    track.setDuration(format.getDuration());
                    track.setFormatName(format.getFormatName());
                    track.setSize(format.getSize());
                    try {
                        if (tags.getTrack() != null) {
                            track.setTrackNumber(parseTrackNumber(tags.getTrack()));
                        }
                        if (tags.getTrackTotal() != null) {
                            track.setTrackTotal(parseTrackNumber(tags.getTrackTotal()));
                        }
                        if (tags.getDisc() != null) {
                            track.setDiscNumber(parseTrackNumber(tags.getDisc()));
                        }
                        if (tags.getDiscTotal() != null) {
                            track.setDiscTotal(parseTrackNumber(tags.getDiscTotal()));
                        }
                    } catch (NumberFormatException e) {
                        log.error("解析Track/Disc编号时出现错误，原因：{}，文件路径: {}", e.getMessage(), musicFile.getAbsolutePath());
                    }
                }
                album.getTrackList().add(track);
            }
            return album;
        } catch (IOException e) {
            log.error("提取音乐信息时出现错误，路径 {}", firstMusicFile.getAbsolutePath(), e);
        } catch (InterruptedException e) {
            log.error("提取音乐信息时被中断，路径 {}", firstMusicFile.getAbsolutePath(), e);
        }
        return null;
    }

    private Integer parseTrackNumber(String trackNumber) {
        Matcher matcher = TRACK_PATTERN.matcher(trackNumber);
        if (matcher.matches()) {
            String matchedNumber = matcher.group(1) != null ? matcher.group(1) : matcher.group(3);
            return Integer.parseInt(matchedNumber);
        }
        throw new NumberFormatException("未知格式" + trackNumber);
    }

    /**
     * 获取文件后缀
     *
     * @param path 文件Path
     * @return 返回最后一个后缀。若无后缀返回空串
     */
    private String getExtension(File path) {
        String filename = path.getName();
        int extensionStop = filename.lastIndexOf(".");
        if (extensionStop == -1) {
            return "";
        }
        return filename.substring(extensionStop).toLowerCase();
    }

    private void extractCover(File musicFile, File dst) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg",
                "-i", musicFile.getAbsolutePath(),
                "-v", "quiet",
                "-an",
                "-vcodec", "copy",
                dst.getAbsolutePath()
        );
        //TODO: Error Handling
        processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);

        Process process = processBuilder.start();
        process.waitFor(60, TimeUnit.SECONDS);
    }

    public static List<String> parseArtists(String artist) {
        List<String> resolvedArtist = new ArrayList<>();
        if (artist != null) {
            Matcher matcher = ARTIST_PATTERN.matcher(artist);
            while (matcher.find()) {
                String start = matcher.group(1);
                String tail = matcher.group(3);
                resolvedArtist.add(start != null ? start.strip() : tail.strip());
            }
        }
        return resolvedArtist;
    }
}
