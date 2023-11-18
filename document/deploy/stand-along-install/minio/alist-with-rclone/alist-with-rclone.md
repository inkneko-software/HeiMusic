使用AList与Rclone挂载网盘至本地
---


### 1. 启动Alist并配置存储

进入到alist目录，使用`docker compose up -d`启动服务

服务开启后使用端口5244访问webui，设置好账号密码与存储

最后更新compose文件将监听地址改为127.0.0.1 并通过`docker compose restart`重启服务

alist配置存储在`/etc/alist`

### 2.创建rclone配置

配置将存储于`/etc/rclone`

```bash
docker run --rm \
  -v /etc/rclone:/config/rclone \
  --device /dev/fuse --cap-add SYS_ADMIN --security-opt apparmor:unconfined \
  rclone/rclone \
  config create alist webdav\
  url=http://127.0.0.1:5244/dav/\
  vendor=other\
  user=用户\
  pass=密码\
  --obscure
```

### 3.挂载rclone
创建挂载目录

```bash
mkdir -p /mnt/heimusic/minio
```

进入到rclone目录，使用`docker compose up -d`启动服务

