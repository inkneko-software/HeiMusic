HeiMusic 播放历史功能设计
---------------------

记录用户播放行为，支撑"最近播放"与"最常播放"两个列表场景。

数据粒度采用**每用户每歌一条**（方案B）：`play_history(user_id, music_id, play_count, last_played_at)`，复合主键。不存 append 式播放明细（完整时间线）——若将来有该需求，应引入其他存储机制（如时序库）而非膨胀本表。断点续播不做：音乐默认从头播放，上一首进度由客户端自行保存。

## 1. 数据模型

```sql
play_history(
    user_id, music_id,            -- 复合主键，与 music_favorite 同形态
    play_count INT DEFAULT 1,     -- 累计播放次数
    last_played_at TIMESTAMP,     -- 最近一次播放时间
    created_at,                   -- 首次播放时间
    updated_at
)
```

排序查询走主键前缀（user_id）+ filesort，单用户行数有限，不额外建索引。表体积上界 = 用户数 × 听过的歌数，无需清理策略。

## 2. 接口（`/api/v1/playHistory`，全部登录用户、仅本人数据）

- `POST /report?musicId=`：打点。**实际开始播放时调用**（不做播放时长阈值，个人自托管服务不为不存在的刷量设计）。首次插入 play_count=1，之后计数+1 并刷新 last_played_at
- `GET /getRecentList?limit=`：最近播放，按 last_played_at 倒序（秒级并列时 music_id 兜底稳定排序）
- `GET /getMostPlayedList?limit=`：最常播放，按 play_count 倒序，并列时按 last_played_at 倒序
- `POST /removeHistory?musicId=`：删除单条本人历史

limit 默认 100、上限 500，controller 内钳制。

`PlayHistoryVo` = `{ music: MusicVo, playCount, lastPlayedAt }`——包装而非污染 MusicVo；MusicVo 组装链与 `PlaylistController.getMyFavoriteMusicList` 一致（listByIds + HashMap 保序 + 专辑/艺术家/资源/收藏标记）。

## 3. 边界语义

- **打点永不静默**：并发首播竞态（DuplicateKeyException）必须落入"计数递增"分支补偿执行，与收藏的 catch-ignored 不同
- **权限**：userId 一律取自 session（AuthUtils），不提供任何跨用户查询
- **无缓存**：打点是写热路径（每次播放一次），列表缓存会反复失效；查询走主键前缀成本可接受
- **悬挂容忍**：列表组装时 music 缺失（理论上级联已覆盖）跳过不抛错
- **级联**：`removeAlbum` 删音乐时调用 `removeByMusicId` 清理全部用户的历史

## 4. 错误码（PlayHistoryServiceErrorCode，6000 段）

- 6000 指定音乐不存在（打点时）
- 6001 未查询到播放历史（删除时）

## 5. 已知限制

- `removeAlbum` 级联覆盖 play_history 与 lyric，但**不清理 music_favorite**（孤儿收藏，历史遗留缺口，待另行决定）
- last_played_at 秒级精度，同一秒内的多次播放排序不稳定（已用 music_id/play_count 兜底，业务无感知）
