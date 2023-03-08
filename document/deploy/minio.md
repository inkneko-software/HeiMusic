MinIO部署文档
---

使用MinIO进行存储

数据保存至：/mnt/heimusic

```
mkdir /mnt/heimusic/
   
docker run \
   -d \
   -p 127.0.0.1:9000:9000 \
   -p 127.0.0.1:9090:9090 \
   --name minio \
   -v /mnt/heimusic:/data \
   -e "MINIO_ROOT_USER=user" \
   -e "MINIO_ROOT_PASSWORD=password" \
   -e "TZ='Asia/Shanghai'" \
   -e "MINIO_SERVER_URL=https://example.com" \
   -e "MINIO_BROWSER_REDIRECT_URL=https://c.example.com" \
   quay.io/minio/minio server /data --console-address ":9090"
```

nginx转发

```

server {
  listen 443 ssl default_server;

  server_name example.com;
  ssl_certificate /etc/nginx/cert/example.com/cert.crt;
  ssl_certificate_key /etc/nginx/cert/example.com/cert.key;
  
  # To allow special characters in headers
  ignore_invalid_headers off;
  # Allow any size file to be uploaded.
  # Set to a value such as 1000m; to restrict file size to a specific value
  client_max_body_size 0;
  # To disable buffering
  proxy_buffering off;

  location / {
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header Host $http_host;

    proxy_connect_timeout 300;
    # Default is HTTP/1, keepalive is only enabled in HTTP/1.1
    proxy_http_version 1.1;
    proxy_set_header Connection "";
    chunked_transfer_encoding off;

    proxy_pass http://127.0.0.1:9000; # If you are using docker-compose this would be the hostname i.e. minio
    # Health Check endpoint might go here. See https://www.nginx.com/resources/wiki/modules/healthcheck/
    # /minio/health/live;
  }
    
}

map $http_upgrade $connection_upgrade {
  default     keep-alive;
  'websocket' upgrade;
}

server {
  listen 443 ssl;
  server_name c.example.com;
  ssl_certificate /etc/nginx/cert/c.example.com/cert.crt;
  ssl_certificate_key /etc/nginx/cert/c.example.com/cert.key;
  location / {
    proxy_pass http://127.0.0.1:9090/;
    proxy_set_header X-Real-IP $remote_addr;
    keepalive_timeout 5;
    client_max_body_size 1G;
    client_body_buffer_size 16M;
    proxy_set_header Upgrade    $http_upgrade;
    proxy_set_header Connection $connection_upgrade;
    proxy_set_header Host       $host;
  }
}

```

