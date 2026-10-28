# 03 · 打卡模块

打卡是内置的第一个 Box（id = `checkin`），数据存放在仓库根下的 `checkin/` 目录。

## 数据模型

```kotlin
data class CheckInItem(
    val id: String, val name: String, val icon: String, val colorHex: String,
    val createdAt: Long, val archived: Boolean = false, val order: Int = 0
)

data class CheckInRecord(
    val id: String, val itemId: String, val timestamp: Long, val note: String = ""
)
```

记录用**毫秒时间戳**，详情页能看到「几点几分几秒」。

## 文件

| 文件 | 内容 |
| --- | --- |
| `checkin/items.json` | `{ "items": [ ... ] }` |
| `checkin/records/<yyyy-MM>.json` | `{ "month": "2026-10", "records": [ ... ] }` |

按月拆分，避免单文件无限膨胀。加载时读取「最近 13 个月 + 仓库里已存在的月份」。

## 页面

### 打卡主页（第一个 Tab 的内容）

- 顶栏：Box 名称 + 同步按钮 + 记录墙入口
- 今日概览：`已完成 x / y 项`
- 事项卡片：图标、连续天数、总次数、今日打卡时间；右侧圆形按钮打卡，**再点撤销当天打卡**（一天一次）
- 新建事项：名称 + 图标（内置图标库）+ 颜色
- 最近 5 天：按天列出打卡次数与事项图标，点击进详情

### 某天详情

当天每条记录：事项、精确到秒的时间、删除按钮。

### 记录墙

GitHub 贡献墙风格：53 周 × 7 天，周日开头，横向滚动；颜色 5 级（0 / 1 / 2–3 / 4–5 / 6+）；顶部统计总次数与连续天数；点格子看当天详情。

## 关键算法

连续天数（今天没打卡不打断，从昨天算起）：

```kotlin
var cursor = LocalDate.now()
if (cursor !in days) cursor = cursor.minusDays(1)
while (cursor in days) { count++; cursor = cursor.minusDays(1) }
```

## 保存时机

每次新建/编辑/删除事项、打卡、撤销、删除记录都会写入对应文件，然后由 `StorageBackend` 立即 commit + push。失败时 Snackbar 提示，数据留在本地仓库，可用配置页「推送」补交。
