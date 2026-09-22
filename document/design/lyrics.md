HeiMusic 歌词功能设计
---------------------

歌词与音乐为 1:N 关系：一首音乐可有多种语言（locale）的歌词，每种语言仅一份；翻译即另一条 locale 记录（如原文 ja + 译文 zh-cn），后端不做翻译关联字段，前端按 musicId 拉全量后自行配对展示。

数据源规划：LRCLIB 为主数据源（合规；API 事实与 locale 判定实测见第 6、7 节），其他在线平台后续以第三方插件方式接入。录入通过独立接口，在音乐创建后追加（兼容自动化爬取与手动录入），本期做存储与维护接口及 LRCLIB 拉取（设计见第 8 节）。

## 1. 数据模型

### 1.1 歌词表（lyric）

- `lyric_id`：自增主键
- `music_id`：所属音乐
- `content`：歌词全文，`MEDIUMTEXT`（16MB）。不用 VARCHAR：utf8mb4 下 VARCHAR 上限约 16383 字符（65535 是字节行上限），QRC 逐字格式体积约为 LRC 的 3~5 倍，双语对照有溢出风险；TEXT 的 64KB 上限是字节而非字符，同样可能触顶。content 不参与 WHERE/JOIN，off-page 存储无代价
- `locale`：BCP 47 风格语言标签，入库统一小写（Service 层归一化：trim + 小写 + 下划线转连字符，如 `zh_CN` → `zh-cn`）
- `format`：歌词格式标识，自由字符串不设数据库枚举：`text` / `lrc` / `lrc_a2`（增强 LRC）/ `qrc` 等，应用层解释
- 唯一约束：`UNIQUE(music_id, locale)`，同音乐同语言仅一份，重复添加由应用层转为业务错误码

### 1.2 music 表扩展列

- `default_lyric_id INT NULL`：默认歌词指向 `lyric.lyric_id`，无外键，应用层维护引用完整性。"至多一个默认"由构造保证（一列一值）；删除默认歌词时在 Service 层事务中清除引用；读侧对悬挂引用容忍（指向不存在的歌词时视为无默认，不抛错）。不用 `lyric.is_default` + `unique(music_id, is_default)` 的 NULL 技巧方案：该方案依赖"非默认必须存 NULL 不能存 0"的隐式约定，易踩雷
- `is_instrumental TINYINT(1) NULL`：三态——NULL=未知，1=纯音乐，0=有人声（参考 LRCLIB instrumental 语义）

## 2. 歌词维护接口（管理账户，`@UserAuth(requireRootPrivilege = true)`）

### 2.1 添加歌词

`POST /api/v1/lyric/add`，JSON body：`musicId`（必填）、`locale`（BCP 47，正则校验）、`format`（小写字母/数字/下划线）、`content`（最长 200000 字符）。音乐不存在报 5001，同语言重复报 5002（捕获 DuplicateKeyException）。

### 2.2 更新歌词

`POST /api/v1/lyric/updateLyric`，JSON body：`lyricId`（必填）、`content`/`format`（可空，至少其一）。**不支持修改 locale 与所属音乐**：改语言 = 删旧增新，避免唯一键冲突面扩大与"翻译=独立记录"语义混乱。

### 2.3 删除歌词

`POST /api/v1/lyric/removeLyric?lyricId=`。事务内：删除歌词 → 若为默认歌词则清除 `music.default_lyric_id`。

### 2.4 设定/取消默认歌词

`POST /api/v1/lyric/setDefaultLyric?musicId=&lyricId=`，`lyricId` 不传表示取消默认。校验歌词存在（5000）且属于该音乐（5003）。音乐不存在报 5001。

### 2.5 纯音乐标记

`POST /api/v1/music/setInstrumental?musicId=&isInstrumental=`，`isInstrumental` 不传表示回到未知态。

## 3. 歌词查询接口（登录用户，`@UserAuth`）

- `GET /api/v1/lyric/getList?musicId=`：该音乐全部歌词的完整列表（含 content），`isDefault` 字段标识默认歌词。单曲 locale 数通常 1~3，一次拉全避免播放器 N+1
- `GET /api/v1/lyric/get?lyricId=`：按 id 查单条
- `GET /api/v1/lyric/getByLocale?musicId=&locale=`：按语言查单条，locale 查询前归一化

## 4. 边界语义与约定

- **唯一键冲突**：添加时同音乐同语言重复（含并发）捕获 `DuplicateKeyException` 转 5002
- **locale 归一化**：入库与查询统一 `trim().toLowerCase().replace('_', '-')`；数据库排序规则大小写不敏感兜底
- **纯音乐与歌词共存**：`is_instrumental` 不阻止歌词增删。理由：标注与歌词数据来源、生命周期不同（人工标注 vs LRCLIB 拉取），强制互斥会把误标记的修复成本变成"先删数据才能改标记"；该字段是展示层提示而非完整性约束
- **default_lyric_id 悬挂防御**：读侧容忍——指向的歌词不存在时 `isDefault` 全为 false，不抛错、不做读时自愈写回；写入口（setDefaultLyric/removeLyric）保证引用有效
- **删除音乐级联**：`removeAlbum` 逐曲删除时调用 `lyricService.removeByMusicId(musicId)`，不留孤儿歌词

## 5. 缓存策略

| 缓存名 | 键 | 读 | 驱逐 |
|---|---|---|---|
| `lyric` | lyricId | getById | updateLyric（注解）/ removeLyric、removeByMusicId（手动逐条） |
| `lyricList` | musicId | getMusicLyrics | addLyric（注解）/ updateLyric（注解 #result.musicId）/ removeLyric、removeByMusicId（手动） |
| `music` | musicId | 既有 | updateDefaultLyric / updateInstrumental（注解） |

删除类操作的 musicId 需查库获得，且 removeByMusicId 需逐条清理 lyric 缓存，注解无法表达，故在 `LyricServiceImpl.evictLyricCaches` 手动驱逐（CacheManager），其余走注解。

## 6. LRCLIB 数据源事实（2026-09-21 实测核实）

拉取功能的设计输入。LRCLIB 公开访问，无需 API Key。

### 6.1 读取端点

| 端点 | 参数 | 说明 |
|---|---|---|
| `GET /api/get` | `track_name`、`artist_name` 必填；`album_name`、`duration` 推荐传 | 曲目签名最优匹配，返回单条；`duration` 单位秒，须在 1~3600，服务端仅返回时长差 **±2s** 内的记录 |
| `GET /api/get/{id}` | 路径参数 `id` | 按 LRCLIB 绝对 ID 查单条，响应同上 |
| `GET /api/search` | `q`（全字段，优先级最高）或 `track_name`（+可选 `artist_name`/`album_name`） | 返回数组，**最多 20 条，无分页** |

### 6.2 响应与空返回

成功：`/api/get` 返回单条记录，`/api/search` 返回数组，字段为 `id`、`name`（`trackName` 的别名）、`trackName`、`artistName`、`albumName`、`duration`、`instrumental`、`plainLyrics`、`syncedLyrics`（LRC 格式）、`lyricsfile`（YAML，必有但暂不使用）。

空数据两种形态，解析需分开处理：

- `/api/get`：**HTTP 404** + `{"code":404,"name":"TrackNotFound","message":"..."}`
- `/api/search`：HTTP **200** + 空数组 `[]`

404 ≠ 永久缺失：LRCLIB 后台服务会补录缺失曲目，后续重试可能命中——业务错误码应表达"可重试"语义。

### 6.3 频率限制

文档只给机制不给数值：超限返回 **429 + `Retry-After` 头（秒）**，客户端必须遵守，无视可能临时封禁。官方节流建议：**串行请求**（上一请求完成再发下一个），批量场景每请求间隔 **200~500ms**。

### 6.4 请求标识

必须设置 `User-Agent`，格式 `应用名 版本 (主页或邮箱)`，如 `HeiMusic v1.0 (https://github.com/leaf-lxh/heimusic)`；无法设置 UA 时用 `X-User-Agent` 或 `Lrclib-Client` 头替代。

### 6.5 内容映射与既有字段的对接

- `syncedLyrics` → `format=lrc`；`plainLyrics` → `format=text`。同一条记录可能两者都有：**只取其一入库**（有 synced 存 lrc，无 synced 才存 text）——`UNIQUE(music_id, locale)` 下一首音乐同语言仅一份，不拆两条
- `instrumental=true`：纯音乐标记，走 `music.is_instrumental`（见 1.2），**不产生歌词记录**
- LRCLIB 无语言维度，locale 需自行判定（见第 7 节）
- 本项目 `music.duration` 为 String（ffprobe 原样输出，形如 `233.500000`），调 `/api/get` 前需解析为整数秒并校验 1~3600 边界
- 专辑名不在 music 表（在 album + album_music 关联），artist 为逗号分隔的多艺术家字符串——多艺术家可能降低 LRCLIB 匹配率，降级策略见拉取功能设计

## 7. locale 自动判定策略（Tika 2026-09-21 实测）

### 7.1 依赖

新增 `org.apache.tika:tika-langdetect-optimaize:2.9.0`（与现有 tika-core 版本对齐）。注意：`tika-core` 只有 `LanguageDetector` 接口无实现，现有依赖下 `getDefaultLanguageDetector()` 抛 `No language detectors available`。传递依赖：optimaize `language-detector:0.6`、guava 18.0、jsonic 1.2.11。使用 `new OptimaizeLangDetector().loadModels()` 初始化，实例可复用（逐次 `reset()`）；直接实例化而非依赖 SPI 自动发现（实验中类加载器差异曾导致 ServiceLoader 发现失败，直接实例化行为确定）。

### 7.2 代码格式兼容

返回 `en` / `ja` / `zh-CN` / `zh-TW`（语言小写 + 区域大写），经现有 `normalizeLocale`（toLowerCase）即为我们约定形态；简繁可区分，同音乐可并存 `zh-cn` 与 `zh-tw` 两份。

### 7.3 实测结论

| 文本形态 | 整体检测 | 结论 |
|---|---|---|
| 单语言长文本（en / 简中 / 繁中） | HIGH | 准确 |
| 日文长段 | MEDIUM 0.68（第二名 zh-CN 0.31） | 汉字干扰，可用 |
| 中英 / 日英混唱 | **en HIGH** | **整体检测必错判英文** |
| 超短文本（"la la la"） | oc HIGH | 完全不可靠 |
| 罗马音日文 | sw | 预期误判（拉丁字母） |

逐行投票实测：中英混合整体误判 en → 逐行投票 zh-CN 6 票 : en 1 票，修正成功；日英混合投票 zh-CN（ja 仅 1 票），ja/zh 混淆仍在但至少不再错成英文。

### 7.4 策略

1. **逐行投票**：`syncedLyrics` 剥离 `[mm:ss.xx]` 时间戳后逐行检测计票（`plainLyrics` 按行同理），取票数第一者；LRC 时间戳实测不干扰检测，剥离只为稳妥且零成本
2. **结果仅作初始值**：低置信度 / 无票 / 纯拉丁文本存 `und`（3 字母码，现有 locale 正则 `[A-Za-z]{2,8}` 兼容）
3. **手动覆盖**：拉取接口允许显式传 locale，跳过自动检测
4. **纯音乐不检测**：LRCLIB `instrumental=true` 走 `updateInstrumental`，不产生歌词记录、不进检测流程
5. 已知残留（ja/zh 混淆、罗马音误判）接受不完美：数据模型"每语言一份"下混唱本无唯一正确答案，人工修正走删旧增新

## 8. LRCLIB 拉取功能设计（本期）

核心原则：**拉取只服务无歌词的音乐，人工数据无条件优先**。不提供覆盖能力，并发覆盖由事务防护（见 8.2）。

### 8.1 手动接口（管理）

`POST /api/v1/lyric/fetchFromLrclib?musicId=&locale=`，`@UserAuth(requireRootPrivilege = true)`。`locale` 可选，显式传时跳过自动判定（应对罗马音等已知误判场景）。

返回 `LyricFetchVo`：`outcome`（`created` / `instrumental` / `not_found`）+ `lyric`（created 时为 LyricVo）。三种 outcome 均以 code=0 返回——not_found 非操作错误，LRCLIB 会后台补录（见 6.2），表达可重试语义。

### 8.2 Service 流程

单方法 `@Transactional`，锁只覆盖写段：

1. `musicService.getById` 校验存在（5001）
2. 组装签名参数：title、artist（music.artist 原样）、album 名（album + album_music 关联，缺失则不携带）、duration 解析整数秒（解析成功且在 1~3600 才携带，见 6.1/6.5）
3. **HTTP 调用在锁外**：`LrclibClient.getBySignature(...)`；404 → outcome=not_found，事务内不做任何写
4. `SELECT music ... FOR UPDATE` 锁行 → 校验该音乐歌词数：**已有任何歌词 → 抛新错误码 5005**（手动调用即操作失误；消费侧捕获后跳过）
5. instrumental 联动：**仅当 `music.is_instrumental` 为 NULL 时**写入 LRCLIB 的值（true→1、false→0）；已有人工标注不覆盖。LRCLIB `instrumental=true` → outcome=instrumental，不建歌词记录
6. 内容取舍：有 `syncedLyrics` → `format=lrc`，否则 `plainLyrics` → `format=text`（见 6.5，不拆两条）
7. locale：显式传参 > 逐行投票 > `und`（见 7.4）
8. 复用 `addLyric`（继承归一化与缓存驱逐；`DuplicateKeyException` → 5002 作为并发漏网兜底）→ outcome=created

并发正确性说明：InnoDB 的 FOR UPDATE 在语句执行时才取锁，置于 HTTP 调用之后，使持锁时长仅覆盖校验 + 插入（毫秒级）；"锁行 → 查歌词 → 插入"同事务串行化，杜绝"校验后、插入前"手工添加被覆盖。事务全程占用一个连接（外部调用期间为秒级超时），配合 MQ 串行消费可接受。

### 8.3 MQ 批量补全

- 新增持久化队列 `lyric-queue`，routingKey `lyric.musicId.%s`，绑定既有 topic exchange（RabbitMQConfig 常量内嵌类先例），消息体 `{musicId}`
- `LyricFetchConsumer`：`@RabbitListener(queues = "lyric-queue", ackMode = "MANUAL")` + `@ConditionalOnProperty(heimusic.is-lyric-fetch-node, havingValue = "true", matchIfMissing = true)`（对齐 Encode 系开关风格）
- 消费语义（全部 ack，不留死信）：
  - created / instrumental → ack + log.info
  - not_found → ack + log.info（LRCLIB 后台补录后可重投）
  - 5005 已有歌词 → ack + log.info（不覆盖人工数据）
  - 429 → 读 `Retry-After`，`sleep(min(retryAfter, 60s))` 后重试一次；再 429 则 ack 丢弃，待下轮批量投放
  - 其余异常 → log.error + ack 丢弃（与 ProbeConsumer 丢弃语义一致），靠投放脚本重投
- **批量触发两条路径**：
  - HTTP：`POST /api/v1/lyric/scanMissingLyric`（管理权限，无参数）——查询"无歌词且非纯音乐"的音乐逐条投递，返回投放数；**60 秒节流窗口**（Redis `SETNX + TTL` 原子实现，键 `heimusic:lyric:scan_throttle`），窗口内重复调用抛 `LYRIC_SCAN_THROTTLED(5006)`（HTTP 200 + 业务码，前端 request 封装友好）。幂等性两层：入队前排除已有歌词的音乐 + 消费端"无歌词才拉取"校验兜底
  - 手动脚本：`@Disabled` 的 `LyricFetchProducerTests`（ProbeConsumerTests 先例）保留，用于绕过 HTTP 直接投放
- **串行与单飞**：listener 并发固定 1——任意时刻只有一条消息在处理，"同时只有一个拉取任务"由消费端内建（对比 MusicScannerJob 的 `AtomicBoolean isRunning` 任务级防重入，此处是更细的消息级串行）；每条处理完 `sleep(2000ms)`，以远低于官方节流建议（200~500ms）的速率访问 LRCLIB（见 6.3）；即使重复投放，重复消息自然得到 5005 → ack 丢弃，幂等自愈
- **多实例注意**：若部署多节点，`heimusic.is-lyric-fetch-node` 只在一个节点开启（竞争消费者会并行消费、放大 LRCLIB 请求速率）；本项目单体部署默认单节点，无此问题

### 8.4 新增组件与依赖

- pom：`org.apache.tika:tika-langdetect-optimaize:2.9.0`（见 7.1）
- `util/lrclib/LrclibClient`：Spring 自带 `RestClient`（项目首个 HTTP 客户端，不引新依赖），连接/读取超时配置化；`User-Agent` 按 6.4 设置；响应映射 `LrclibTrack`（`@JsonProperty` + `@JsonAnySetter` 容错，ProbeConsumer 解析 ffprobe JSON 的先例）
- `util/lyric/LyricLanguageDetector`：Optimaize 封装 + 逐行投票（见 7.4）；实例复用 + `synchronized`（消费串行 + 手动单曲，低并发足够）
- 配置：`heimusic.lrclib.base-url`（默认 `https://lrclib.net`）、`heimusic.lrclib.user-agent`（默认 `HeiMusic (https://github.com/leaf-lxh/heimusic)`）、`heimusic.is-lyric-fetch-node`；application-example.yaml 同步补齐
- 错误码（LyricServiceErrorCode 续）：`LYRIC_FETCH_ALREADY_HAS_LYRIC(5005)`

### 8.5 任务日志与查询接口

每次拉取尝试（手动与 MQ 批量均覆盖）写入一条 `lyric_fetch_log` 记录，供管理端前端查看任务历史：

- 表结构（已入 `heimusic.md` 基准）：`id`、`music_id`（可空，消息解析失败场景）、`source`（manual / mq）、`outcome`（created / instrumental / not_found / skipped / failed）、`detail`（VARCHAR(512)，locale、错误摘要，超长截断）、`created_at`；`INDEX(music_id)`
- **写入点在服务层统一埋点**：`fetchFromLrclib` 包装 try/catch，各结局 return 前与异常抛出前记录；Controller 传 `SOURCE_MANUAL`、消费者传 `SOURCE_MQ`，一条代码路径覆盖两种入口
- `record()` 用 `REQUIRES_NEW` 独立事务：主流程事务回滚（如并发校验失败）不影响日志留存；写入失败仅打错误日志、不上抛，绝不影响拉取主流程
- 查询接口：`GET /api/v1/lyric/fetchLog/list?page=&pageSize=&musicId=&outcome=`，管理权限，MyBatis-Plus `Page` 按 id 倒序，musicId/outcome 可选过滤
- 保留策略：暂不做定时清理——单曲通常仅一条记录、增长缓慢；需要治理时再补定时任务

### 8.6 测试

- `LyricLanguageDetectorTests`：纯单测（混唱投票、时间戳剥离、und 兜底），无基础设施依赖（CueParser 先例）
- `LrclibClientTests`：`MockRestServiceServer.bindTo(RestClient.Builder)`（spring-test 自带，无新依赖）——正常响应 / 404 / 429 + Retry-After / search 空数组
- 拉取流程：Mockito mock `LrclibClient`（经 `ReflectionTestUtils` 替换共享单例字段并在 `@AfterEach` 恢复，勿用 `@MockitoBean`——会 fork 新上下文，多上下文并存时 Netty 建循环连接易超时），`@SpringBootTest` + `@Transactional` + 缓存清理（LyricServiceTests 先例）——outcome 三态、5005 拒绝、instrumental 仅 NULL 时联动、404 路径零写入
- 任务日志：`REQUIRES_NEW` 提交的日志行在测试事务（REPEATABLE READ 快照）内不可见、事务内清理会被回滚——断言与清理须用独立 auto-commit 连接（见 `LyricFetchServiceTests.countLogs`）；分页与过滤行为由 `LyricFetchLogServiceTests` 覆盖
- 批量投放脚本 `@Disabled` 手动执行

## 9. 后续规划（非本期）

- 第三方平台插件化接入
- 逐字歌词（qrc/lrc_a2）客户端渲染约定
