HeiMusic 服务端部署
--

使用docker compose进行项目部署

## 目录结构

```
.
├── README.md             本说明文件
├── conf                  springboot配置文件
├── docker-compose.yaml   
├── mysql                 mysql数据库
└── stand-along-install   各个组件的单个docker-compose文件
```

## 安装

### 1. MySQL配置

若mysql目录为空，容器会默认执行mysql-initdb.d/heimusic.sql文件，创建库表和用户。

在初始化完成后请删除该初始化文件，否则若mysql目录不为空而存在init文件，容器会拒绝上线

请确保该文件中的用户密码，与服务端配置文件相匹配

如需设置root密码请在docker-compose.yaml文件中指定，默认无法使用密码登录


### 2. MinIO配置

请指定MinIO的accessKey与secretKey，即管理账户与密码，确保与服务端配置文件相匹配

同时请将服务端配置`heimusic.minio.endpoint`指定为外部访问minio的路径url

### 3. 启动项目

使用命令`docker compose up -d`启动项目

使用命令`docker compose ps -a`查看各个组件的运行状态

使用命令`docker compose logs [组件名称] [-f] [-n num]`查看输出日志

默认 minio 暴露于`9000`端口，minio console暴露于`9090`端口，api服务端暴露于`9001`端口

### 4. 设置MinIO权限与目录

目前需要手动设置，后续将添加自动设置

通过访问`localhost:9000`访问minio console

首先创建桶 `heimusic`，权限控制参考如下，指定了该桶默认公开只读

```
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "AWS": [
                    "*"
                ]
            },
            "Action": [
                "s3:GetBucketLocation",
                "s3:ListBucket",
                "s3:ListBucketMultipartUploads"
            ],
            "Resource": [
                "arn:aws:s3:::heimusic"
            ]
        },
        {
            "Effect": "Allow",
            "Principal": {
                "AWS": [
                    "*"
                ]
            },
            "Action": [
                "s3:GetObject",
                "s3:ListMultipartUploadParts",
                "s3:AbortMultipartUpload"
            ],
            "Resource": [
                "arn:aws:s3:::heimusic/*"
            ]
        }
    ]
}
```

再创建项目所需的路径`music`，`cover`路径

### 5.Nginx反代

docker-compose.yaml文件默认将服务暴露于127.0.0.1地址上

通过域名访问，请参考nginx配置文件`nginx/sites-enabled/heimusic`