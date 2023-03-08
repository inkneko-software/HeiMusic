HeiMusic 音乐数据库
---

数据库名称：`heimusic`

## 用户相关表

### 用户信息

```mysql
CREATE TABLE user_detail(
	user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(20) NOT NULL,
    email VARCHAR(30),
    avatar_url VARCHAR(255) NOT NULL,
    birth DATETIME NOT NULL,
    gender CHAR(1),
    sign VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE(email)
)Engine=InnoDB Default Charset=UTF8MB4;
```

### 认证信息

```mysql
CREATE TABLE user_auth(
	user_id INT PRIMARY KEY,
    auth_hash CHAR(64),
    auth_salt CHAR(32),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
)Engine=InnoDB default Charset=UTF8MB4;
```

## 音乐相关表

### 艺术家信息

```mysql
CREATE TABLE music_artist(
	artist_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    translate_name VARCHAR(50),
    avatar_url VARCHAR(100),
    birth DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
)Engine=InnoDB default charset=utf8mb4;
```

### 专辑信息

```mysql
CREATE TABLE music_album(
	album_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    translate_name VARCHAR(50),
    front_cover_url VARCHAR(255),
    back_cover_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP 
)Engine=InnoDB default charset=utf8mb4;
```

### 专辑-艺术家

```mysql
CREATE TABLE music_album_artist(
	album_id INT, 
    artist_id INT,
    PRIMARY KEY(album_id, artist_id)
)Engine=InnoDB default charset=utf8mb4;
```

### 音乐信息

```mysql
CREATE TABLE music_detail(
	music_id INT PRIMARY KEY AUTO_INCREMENT,
    album_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    resource_path VARCHAR(255),
    bitrate INT,
    codec VARCHAR(20),
    duration INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX(album_id)
)Engine=InnoDB default charset=utf8mb4;
```

### 音乐文件信息

```mysql
CREATE TABLE music_resource(
    music_resource_id INT PRIMARY KEY AUTO_INCREMENT,
	music_id INT NOT NULL,
    codec VARCHAR(20),
    bitrate INT,
    bitrate_str VARCHAR(20),
    url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX(music_id)
)Engine=InnoDB default charset=utf8mb4;
```
