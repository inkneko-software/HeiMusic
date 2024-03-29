docker exec -i heimusic-mysql -uroot -ppassword < heimusic.sql
CREATE DATABASE IF NOT EXISTS heimusic;
USE heimusic;
CREATE USER 'user'@'%' identified by 'password';
CREATE TABLE IF NOT EXISTS user_detail(
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(20),
    email VARCHAR(30),
    avatar_url VARCHAR(255) NOT NULL DEFAULT "/images/default_avatar.jpg",
    birth DATETIME,
    gender CHAR(1),
    sign VARCHAR(100),
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
    auth_hash CHAR(64),
    auth_salt CHAR(32),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
)Engine=InnoDB default Charset=UTF8MB4;
CREATE TABLE IF NOT EXISTS artist(
    artist_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    translate_name VARCHAR(50),
    avatar_url VARCHAR(100),
    birth DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE(name) COMMENT '重名的歌手，应当进行备注，如李华（2013）、李华（2020），不过一般火的艺人基本没重名的，用艺名'
)Engine=InnoDB default charset=utf8mb4;
CREATE TABLE IF NOT EXISTS album(
    album_id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    translate_title VARCHAR(255),
    front_cover_file VARCHAR(255),
    back_cover_file VARCHAR(255),
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
    codec VARCHAR(20) COMMENT '音乐编码格式，如FLAC',
    duration VARCHAR(255) COMMENT '时长，以秒为单位, 如275.453333',
    size VARCHAR(255) COMMENT '文件大小，以b为单位，如65565467' ,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
)Engine=InnoDB default charset=utf8mb4;
CREATE TABLE IF NOT EXISTS music_artist(
    music_id INT,
    artist_id INT,
    PRIMARY KEY(music_id, artist_id)
)Engine=InnoDB default charset=utf8mb4;
CREATE TABLE IF NOT EXISTS music_resource(
    music_resource_id INT PRIMARY KEY AUTO_INCREMENT,
    music_id INT NOT NULL,
    codec VARCHAR(20),
    bitrate VARCHAR(255),
    bucket VARCHAR(255),
    object_key VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX(music_id)
)Engine=InnoDB default charset=utf8mb4;
CREATE TABLE IF NOT EXISTS music_favorite(
    music_id INT NOT NULL COMMENT '音乐id',
    user_id INT NOT NULL COMMENT '用户id',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY(music_id, user_id)
)Engine=InnoDB default charset=utf8mb4;
CREATE TABLE IF NOT EXISTS playlist(
    playlist_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT '歌单id',
    user_id INT NOT NULL COMMENT '创建者用户id',
    description VARCHAR(255) COMMENT '歌单简介',
    cover_url VARCHAR(255) COMMENT '歌单封面',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
)Engine=InnoDB default charset=utf8mb4;
CREATE TABLE IF NOT EXISTS playlist_music(
    playlist_id INT NOT NULL COMMENT '歌单id',
    music_id INT NOT NULL COMMENT '音乐id',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY(playlist_id, music_id)
)Engine=InnoDB default charset=utf8mb4;
CREATE TABLE IF NOT EXISTS playlist_subscribe(
    playlist_id INT NOT NULL COMMENT '歌单id',
    user_id INT NOT NULL COMMENT '收藏者用户id',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY(playlist_id, music_id)
)Engine=InnoDB default charset=utf8mb4;
GRANT SELECT,DELETE,UPDATE,INSERT ON heimusic.* TO 'user'@'%';
