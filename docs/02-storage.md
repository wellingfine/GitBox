# 02 · 存储与 Git 同步

## 一份存储，所有 Box 共用

整个应用只有一个存储后端（`StorageRepository.backendFor()`）：

- 配置了 git 地址 → `GitBackend`（JGit 5.13，工作副本在 `files/gitbox/repo`）
- 未配置 → `LocalBackend`（`files/gitbox/local`）

所有 Box 的数据都写在同一个仓库里，**每个 Box 拥有自己的顶层目录**：

```
.gitbox/config.json              # 应用自身配置（主题、各 Box 的显示名/图标）
checkin/items.json               # 打卡 Box 的数据目录
checkin/records/2026-10.json
```

以后新增 hobbit Box，就是仓库里的 `hobbit/…`。路径由 `StoragePaths` 统一定义：

```kotlin
object StoragePaths {
    const val CONFIG = ".gitbox/config.json"
    fun boxDir(boxId: String) = boxId
    fun boxFile(boxId: String, name: String) = "$boxId/$name"
}
```

## 本机只存什么

SharedPreferences（`gitbox_local`）里的 `LocalSettings`：

```json
{
  "storage": { "gitUrl": "https://github.com/x/y.git", "token": "ghp_xxx",
               "branch": "main", "userName": "git",
               "authorName": "GitBox", "authorEmail": "gitbox@gitbox.local" },
  "currentBoxId": "checkin"
}
```

**git 地址与 Token 永远只写在这里，不会进入仓库。** `.gitbox/config.json` 里是主题与各 Box 的显示设置：

```json
{ "version": 1, "themeMode": "system",
  "boxes": [ { "id": "checkin", "name": "", "icon": "", "colorHex": "" } ] }
```

`BoxSetting` 的字段为空时使用代码里 `BoxDef` 的默认值。

## 保存即推送

`GitBackend.writeFiles()` 的流程：

1. 写文件到工作副本
2. `git add -A` → `commit`
3. `push`
4. push 失败时先 `pull` 再 `pushPending()` 重试一次；仍失败返回 `SyncResult(ok=false)`，数据留在本地仓库不丢

同步状态是**全局唯一**的（`UiState.sync`），在配置页和第一个 Tab 的顶栏都能看到。

## 拉取与冲突

- 启动时、保存存储设置后、手动点同步都会 `pull`
- 本地是空仓库（unborn HEAD）时先 `fetch`，再 checkout `origin/main|master|<branch>`
- 冲突时报「同步失败」，可用配置页「重置本地」删掉本机副本重新拉取

## 认证

- 仅支持 http(s) + Token：`UsernamePasswordCredentialsProvider(userName, token)`
- Token 为空则不带凭据（公开仓库）
- SSH 暂不支持

## Android 上的 JGit 注意点

- `GitBoxApplication` 设置了 `System.setProperty("user.home", filesDir)`，避免 JGit 读全局配置失败
- 使用 Java 8 兼容的 JGit 5.13（避开 Android 缺失的 `java.net.http`）
- 所有 JGit 调用都在 `Dispatchers.IO` 并包 try/catch，错误转成可读提示
