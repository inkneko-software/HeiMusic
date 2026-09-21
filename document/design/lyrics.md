HeiMusic 歌词功能设计
---------------------

歌词与音乐为 1:N 关系：一首音乐可有多种语言（locale）的歌词，每种语言仅一份；翻译即另一条 locale 记录（如原文 ja + 译文 zh-cn），后端不做翻译关联字段，前端按 musicId 拉全量后自行配对展示。

数据源规划：LRCLIB 为主数据源（合规），其他在线平台后续以第三方插件方式接入。录入通过独立接口，在音乐创建后追加（兼容自动化爬取与手动录入），本期只做存储与维护接口，拉取功能见"后续规划"。

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

## 6. 后续规划（非本期）

- LRCLIB 拉取：管理用户手动触发 + 系统定时任务批量补全缺歌词的音乐
- 第三方平台插件化接入
- 逐字歌词（qrc/lrc_a2）客户端渲染约定
