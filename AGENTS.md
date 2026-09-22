# AGENTS.md

本文件为 AI 编码代理（以及新加入的开发者）提供 HeiMusic 项目的关键信息。修改代码前请先阅读本文件与 `document/design/` 下的设计文档。

## 项目概述

HeiMusic 是一个自托管音乐流媒体服务的**后端**（前端为独立仓库，dev server 端口 8888）。单体 Spring Boot 应用，提供音乐/专辑/艺术家/歌单/用户等 REST API。

- **技术栈**：Java 17、Spring Boot 3.5.16、MyBatis-Plus 3.5.12、MySQL（Druid 连接池）、Redis（spring-data-redis + Redisson）、RabbitMQ、MinIO SDK / 本地文件存储、Apache Tika、Thumbnailator、springdoc-openapi + knife4j
- **音频处理**：依赖外部 `ffmpeg` / `ffprobe` 命令行工具（生产 Docker 镜像基于 ffmpeg 镜像构建）
- **API 文档**：`/swagger-ui.html`（springdoc），`/v3/api-docs`
- **数据库 Schema**：`document/database/heimusic.md`（唯一基准，含建表语句与生成命令）；`heimusic.sql` 为其生成物
- **部署**：`document/deploy/`（docker-compose 全家桶）；GitHub Actions 在 release 时构建镜像 `leaflxh/heimusic-server`

## 常用命令

```bash
# 构建（跳过测试）
./mvnw clean package -Dmaven.test.skip=true

# 编译检查
./mvnw compile

# 运行全部测试（需要本地基础设施，见下节）
./mvnw test

# 运行单个测试类
./mvnw test -Dtest=MusicScannerTests

# 本地启动（dev profile，端口等见本地 application-dev.yml，该文件不入库）
./mvnw spring-boot:run
```

Windows 下使用 `mvnw.cmd`；本项目在 Windows + Git Bash 环境开发，基础设施（MySQL/Redis/RabbitMQ）跑在 WSL2，经 localhost 端口转发访问。

## 环境与测试基础设施

- **运行依赖**：MySQL、Redis、RabbitMQ 必须可用才能启动应用；编码节点还需要 `ffmpeg`/`ffprobe` 在 PATH 中。
- **profile**：`dev`（本地开发，连 localhost，本地文件存储）、`test`（集成测试）、生产配置见 `application-example.yaml`。
- **测试**（`src/test/java`，98 个用例，其中 2 个 `@Disabled`）：
  - test profile 使用独立的 `heimusic_test` 库（需先导入 `document/database/heimusic.sql`），DB 变更按事务回滚，Redis 键定向清理，不污染开发数据。
  - `CueParser` / `MusicScanner` 为纯单元测试（TempDir 生成样例文件）；`ffprobe` 缺失时优雅跳过。`LyricLanguageDetectorTests` / `LrclibClientTests` 亦为纯单元测试（后者经 `MockRestServiceServer` 模拟 HTTP）。
  - `ProbeConsumerTests` / `LyricFetchProducerTests` 标注 `@Disabled`，是手动运维脚本。
  - 部分测试（MinIO 回环测试等）依赖 localhost:9000 的 MinIO。
  - mock 外部 bean 时不要用 `@MockitoBean`（会 fork 新 Spring 上下文，多上下文并存时 Netty 建循环连接易超时）；用 `ReflectionTestUtils` 替换共享单例字段并在 `@AfterEach` 恢复（见 `LyricFetchServiceTests`）。
  - `REQUIRES_NEW` 的写入（如歌词任务日志）不受测试事务回滚保护：测试事务快照看不到它、事务内清理会被回滚，断言与清理须用独立 auto-commit 连接（见 `LyricFetchServiceTests` / `LyricFetchLogServiceTests`）。

提测前请运行 `./mvnw test` 并确保全部通过。

## 代码结构

```
src/main/java/com/inkneko/heimusic/
├── HeiMusicApplication.java      # 入口
├── annotation/auth/UserAuth.java # 接口鉴权注解（required / requireRootPrivilege）
├── config/                       # 各基础设施配置类（MinIO、RabbitMQ、Redisson、MyBatis-Plus、Web、Mail、HeiMusicConfig）
├── controller/                   # REST 控制器，路径统一 /api/v1/*
├── errorcode/                    # ErrorCode 接口 + 各服务错误码枚举
├── exception/                    # ServiceException + 全局异常处理（GlobalExceptionHanler）
├── interceptor/AuthInterceptor   # cookie 会话校验拦截器
├── job/MusicScannerJob           # 本地音乐目录扫描任务
├── mapper/                       # MyBatis-Plus BaseMapper；XML 位于 resources/com.inkneko.heimusic.mapper/
├── model/
│   ├── entity/                   # 数据库实体
│   ├── dto/                      # 请求体
│   └── vo/                       # 响应体（含统一包装 Response<T>）
├── rabbitmq/                     # Probe/Split/Encode 消费者（音频探测/拆分/转码流水线）
├── service/ + service/impl/      # 业务逻辑层（接口 + 实现）
└── util/
    ├── auth/                     # 会话工具
    ├── mail/                     # 异步邮件发送（注册验证码）
    └── music/                    # CueParser、MusicProber（ffprobe）、MusicScanner
```

## 开发约定

- **统一响应格式**：Controller 返回 `Response<T>`（`code`/`message`/`data`），成功用 `new Response<>(0, "ok", data)`；业务错误抛 `ServiceException(ErrorCode)`，由 `GlobalExceptionHanler` 统一转换为该格式。新增错误码放入对应服务的 errorcode 枚举。
- **鉴权**：基于 cookie（`userId` + `sessionId`），会话存 Redis（`"userId:epoch"` 格式，带会话版本号：改密/全端登出自增版本号使该用户全部旧会话失效）。需要登录的接口方法上加 `@UserAuth`，管理接口加 `@UserAuth(requireRootPrivilege = true)`；方法内通过 `request.getAttribute("userId")` 取当前用户。不要引入 Spring Security——本项目刻意用拦截器实现（例外：`spring-security-crypto` 仅作为 BCrypt 加密库引入，不含 Security 过滤器/认证框架，勿移除也勿扩大使用）。防爆破限制：邮箱验证码错 5 次作废、密码登录连续失败 10 次锁 15 分钟，均由 `AuthServiceImpl` 内的 Redis 计数实现。
- **分层**：Controller 只做参数校验与组装，业务在 Service；数据访问用 MyBatis-Plus（继承 `BaseMapper`），复杂查询写 XML（resources 下，namespace 注意包目录 `com.inkneko.heimusic.mapper`）。
- **缓存**：Service 方法上的 `@Cacheable`/`@CacheEvict` 需成对维护；`@CachePut` 只能用于返回实体本身的方法——历史上曾因 `@CachePut` 缓存了 boolean 返回值导致 `ClassCastException`（已修复，勿回退）。注意注解驱逐发生在事务提交前：外层事务较长时（含 REQUIRES_NEW 写入）并发读会在窗口内把旧值回填缓存，写入方法需经 `TransactionSynchronization.afterCommit` 二次驱逐兜底（先例：`LyricServiceImpl.evictAfterCommit`）。
- **存储**：`heimusic.storage-type` 区分 `local`（本地目录，`MusicScannerJob` 仅在该模式下扫描本地目录入库）与 OSS（MinIO）；本地存储的封面/音乐文件 URL 用相对路径 `/api/v1/...`，由前端按自身 API 地址拼接，勿改回绝对 URL（会破坏局域网 IP / Electron / Capacitor 访问）。
- **命名空间**：项目已迁移 Spring Boot 3，一律使用 `jakarta.*`，不要引入 `javax.servlet` 等旧命名空间。
- **依赖版本注意**：springdoc 必须 ≥2.8（旧版在 Boot 3.4+ 不工作，pom 中显式覆盖了 knife4j 传递的旧版）；MySQL 驱动版本交由 Spring Boot 托管。
- **代码风格**：Lombok（`@Data` 等）、构造器注入（无 `@Autowired` 注解）、注释与日志用中文。
- **提交信息**：Conventional Commits 风格 + 中文描述，如 `feat: 添加xx`、`fix: 修复xx`、`test:`、`chore:`，重要改动在正文列出要点。

## 已知坑

- `GlobalExceptionHanler` 类名拼写有误（Handler），是历史遗留，引用时注意。
- `application-dev.yml` 含开发环境真实配置，勿将其中的凭据复制到其他文件或提交新的密钥。
- 测试需要的 WSL2 基础设施未启动时，集成测试会失败（连接超时），这不是代码问题。
- 数据库表结构变更只需修改基准文档 `document/database/heimusic.md`，然后用文档头部的命令重新生成 `heimusic.sql` 并复制到 `document/deploy/mysql-initdb.d/`；两处 SQL 均为生成物，勿手改。
- 生成物 `heimusic.sql` 只适用于全新环境（initdb 或新库首导）。对**存量库**做增量同步时，直接导入全量文件会被文件头部的 `CREATE USER`/`CREATE DATABASE` 卡住（mysql 批处理遇错即停），应抽取对应表的 `CREATE TABLE` 段落单独执行；涉及已有表加列则需手写 `ALTER TABLE`。
- MQ 消息模型类统一放 `rabbitmq/model` 包：Jackson 转换器仅信任该包（spring-amqp 3.2.x 的信任匹配是**包名全等**，无前缀/通配），放别处的应用类会在消费端反序列化时被拒、消息被 `ConditionalRejectingErrorHandler` 直接丢弃（见 `RabbitMQConfigTests` 回归）。
