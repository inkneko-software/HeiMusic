CREATE DATABASE IF NOT EXISTS heimusic;
USE heimusic;
CREATE USER 'user'@'%' identified by 'password';
CREATE TABLE IF NOT EXISTS heimusic_application_info(
    version VARCHAR(255) NOT NULL DEFAULT 'current',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
)Engine=InnoDB Default Charset=UTF8MB4;
CREATE TABLE IF NOT EXISTS user_detail(
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255),
    email VARCHAR(255),
    avatar_bucket VARCHAR(255) NOT NULL DEFAULT '' COMMENT '头像所在的桶',
    avatar_object_key VARCHAR(255) NOT NULL DEFAULT '' COMMENT '头像的对象标识，为空表示未设置头像（客户端使用默认头像）',
    birth DATE,
    gender CHAR(1),
    sign VARCHAR(255) NOT NULL DEFAULT '' COMMENT '个性签名',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE(email)
)Engine=InnoDB Default Charset=UTF8MB4;
CREATE TABLE IF NOT EXISTS user_role(
    user_id INT PRIMARY KEY,
    user_role VARCHAR(20) COMMENT 'root/admin',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
)Engine=InnoDB Default Charset=UTF8MB4;
CREATE TABLE IF NOT EXISTS user_auth(
    user_id INT PRIMARY KEY,
    auth_hash VARCHAR(100) COMMENT 'bcrypt（60字符，内嵌盐）；历史数据为加盐SHA1（40字符）',
    auth_salt CHAR(32) COMMENT '已废弃，恒为 -，仅为兼容保留',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
)Engine=InnoDB default Charset=UTF8MB4;
CREATE TABLE IF NOT EXISTS artist(
    artist_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    translate_name VARCHAR(255),
    avatar_url VARCHAR(255) NOT NULL DEFAULT '',
    birth DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE(name) COMMENT '重名的歌手，应当进行备注，如李华（2013）、李华（2020），不过一般火的艺人基本没重名的，用艺名'
)Engine=InnoDB default charset=utf8mb4;
CREATE TABLE IF NOT EXISTS album(
    album_id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    translate_title VARCHAR(255),
    front_cover_bucket VARCHAR(255) NOT NULL DEFAULT '' COMMENT '封面所在的桶',
    front_cover_object_key VARCHAR(255) NOT NULL DEFAULT '' COMMENT '封面的对象标识',
    front_cover_file_path VARCHAR(255) NOT NULL DEFAULT '' COMMENT '封面文件路径',
    large_track_nums INT NOT NULL DEFAULT 0 COMMENT '专辑的整个抓取的音乐轨道数量(flac+cue)',
    album_artist VARCHAR(255) NOT NULL DEFAULT 'V.A.' COMMENT '专辑艺术家字符串',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP 
)Engine=InnoDB default charset=utf8mb4;
CREATE TABLE IF NOT EXISTS album_artist(
    album_id INT, 
    artist_id INT,
    PRIMARY KEY(album_id, artist_id)
)Engine=InnoDB default charset=utf8mb4;
CREATE TABLE IF NOT EXISTS album_music(
    album_id INT,
    music_id INT,
    PRIMARY KEY(album_id, music_id)
)Engine=InnoDB default charset=utf8mb4;
CREATE TABLE IF NOT EXISTS music(
    music_id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    translate_title VARCHAR(255),
    bucket VARCHAR(255) COMMENT '音乐文件的桶名称',
    object_key VARCHAR(255) COMMENT '音乐文件的对象名称，如"/路径/文件名.后缀"',
    bitrate VARCHAR(255) COMMENT '比特率，单位bs，如1904219',
    codec VARCHAR(255) COMMENT '音乐编码格式，如FLAC',
    duration VARCHAR(255) NOT NULL DEFAULT '0' COMMENT '时长，以秒为单位, 如275.453333',
    size VARCHAR(255) COMMENT '文件大小，以b为单位，如65565467',
    track_number INT NOT NULL DEFAULT 0 COMMENT '歌曲的编号',
    track_total INT NOT NULL DEFAULT 0 COMMENT '碟片的歌曲总数',
    disc_number INT NOT NULL DEFAULT 0 COMMENT '所在的碟片编号',
    disc_total INT NOT NULL DEFAULT 0 COMMENT '当前专辑的碟片总数',
    artist VARCHAR(255) NOT NULL DEFAULT '' COMMENT '该歌曲的艺术家（所有艺术家的名称）',
    file_path VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '该文件的路径',
    file_hash VARCHAR(255) NOT NULL DEFAULT '' COMMENT '该文件的哈希值',
    disc_start_time VARCHAR(255) NOT NULL DEFAULT '' COMMENT '音轨中音乐的起始时间，以秒为单位',
    disc_end_time VARCHAR(255) NOT NULL DEFAULT '' COMMENT '音轨中音乐的结束时间，以秒为单位',
    default_lyric_id INT DEFAULT NULL COMMENT '默认歌词id，指向lyric.lyric_id，应用层维护引用完整性（无外键），NULL表示未指定',
    is_instrumental TINYINT(1) DEFAULT NULL COMMENT '是否纯音乐：NULL=未知，1=纯音乐，0=有人声',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
)Engine=InnoDB default charset=utf8mb4;
CREATE TABLE IF NOT EXISTS music_artist(
    music_id INT,
    artist_id INT,
    PRIMARY KEY(music_id, artist_id)
)Engine=InnoDB default charset=utf8mb4;
CREATE TABLE IF NOT EXISTS music_resource(
    music_resource_id INT PRIMARY KEY AUTO_INCREMENT,
    music_id INT NOT NULL,
    codec VARCHAR(255),
    bitrate VARCHAR(255),
    bucket VARCHAR(255),
    object_key VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX(music_id)
)Engine=InnoDB default charset=utf8mb4;
CREATE TABLE IF NOT EXISTS lyric(
    lyric_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '歌词id',
    music_id INT NOT NULL COMMENT '所属音乐id',
    content MEDIUMTEXT NOT NULL COMMENT '歌词全文，格式由format字段解释',
    locale VARCHAR(32) NOT NULL COMMENT '语言标签，BCP 47风格，入库统一小写如zh-cn、ja',
    format VARCHAR(32) NOT NULL DEFAULT 'lrc' COMMENT '歌词格式标识：text/lrc/lrc_a2/qrc等，应用层解释，不设数据库枚举',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE(music_id, locale)
)Engine=InnoDB default charset=utf8mb4;
CREATE TABLE IF NOT EXISTS lyric_fetch_log(
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '日志id',
    music_id INT NULL COMMENT '音乐id，消息解析失败等场景可为空',
    source VARCHAR(16) NOT NULL COMMENT '来源：manual=手动接口 / mq=批量任务',
    outcome VARCHAR(16) NOT NULL COMMENT '结局：created=已创建歌词 / instrumental=纯音乐 / not_found=暂无曲目 / skipped=跳过（已有歌词等） / failed=失败',
    detail VARCHAR(512) NULL COMMENT '补充信息：locale、错误摘要等',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX(music_id)
)Engine=InnoDB default charset=utf8mb4;
CREATE TABLE IF NOT EXISTS music_favorite(
    music_id INT NOT NULL COMMENT '音乐id',
    user_id INT NOT NULL COMMENT '用户id',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY(music_id, user_id)
)Engine=InnoDB default charset=utf8mb4;
CREATE TABLE IF NOT EXISTS play_history(
    user_id INT NOT NULL COMMENT '用户id',
    music_id INT NOT NULL COMMENT '音乐id',
    play_count INT NOT NULL DEFAULT 1 COMMENT '累计播放次数',
    last_played_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近一次播放时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '首次播放时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY(user_id, music_id)
)Engine=InnoDB default charset=utf8mb4;
CREATE TABLE IF NOT EXISTS playlist(
    playlist_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT '歌单id',
    user_id INT NOT NULL COMMENT '创建者用户id',
    title VARCHAR(255) NOT NULL DEFAULT '' COMMENT '歌单名称',
    description VARCHAR(255) NOT NULL DEFAULT '' COMMENT '歌单简介',
    sequence_number INT NOT NULL DEFAULT 0 COMMENT '自定义排序编号',
    cover_url VARCHAR(255) COMMENT '歌单封面',
    play_count VARCHAR(255) NOT NULL DEFAULT 0 COMMENT '播放次数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
)Engine=InnoDB default charset=utf8mb4;
CREATE TABLE IF NOT EXISTS playlist_music(
    playlist_id INT NOT NULL COMMENT '歌单id',
    music_id INT NOT NULL COMMENT '音乐id',
    sequence_number INT NOT NULL DEFAULT 0 COMMENT '自定义排序编号',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY(playlist_id, music_id)
)Engine=InnoDB default charset=utf8mb4;
CREATE TABLE IF NOT EXISTS playlist_subscribe(
    playlist_id INT NOT NULL COMMENT '歌单id',
    user_id INT NOT NULL COMMENT '收藏者用户id',
    sequence_number INT NOT NULL DEFAULT 0 COMMENT '自定义排序编号',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY(playlist_id, user_id)
)Engine=InnoDB default charset=utf8mb4;
GRANT SELECT,DELETE,UPDATE,INSERT ON heimusic.* TO 'user'@'%';
