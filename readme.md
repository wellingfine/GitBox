# GitBox

用 **git 仓库当云存储**的小 App 集合。**Box 就是一个内置的小 App 功能**（当前有「打卡」，新功能由代码添加，用户不能增删）；所有 Box 共用一份存储：一个 git 仓库（保存即推送）或本机目录，每个 Box 在仓库里拥有自己的目录。

- 存储：全局一份 git 地址 + Token（私有仓库填写）；保存时自动 commit + push；不填地址则存本机
- 界面：3 个 Tab（仅图标）—— 当前 Box 功能页、Box 列表（切换/改外观）、配置
- 内置 48 个图标、12 种颜色，可供 Box 外观与打卡事项选择
- 配置页最后一行显示版本：`make by v0tx [v0.0.1]`

## 快速开始

环境：JDK 17、Android SDK（设置 `ANDROID_HOME`），**不需要 Android Studio**。

```powershell
# 拉起模拟器 + 编译 + 安装 + 启动
powershell -ExecutionPolicy Bypass -File .\scripts\run.ps1

# 版本号 +1 并出 release 包
powershell -ExecutionPolicy Bypass -File .\scripts\release.ps1

# 关闭模拟器
powershell -ExecutionPolicy Bypass -File .\scripts\stop.ps1
```

常用 Gradle 命令：

```bash
./gradlew :app:assembleDebug     # 编译 debug 包
./gradlew :app:assembleRelease   # 编译 release 包（不自增版本）
./gradlew bumpVersion            # 版本号 patch +1、versionCode +1
```

产物：`app/build/outputs/apk/debug/app-debug.apk`、`app/build/outputs/apk/release/app-release.apk`

## 使用方式

1. 首次打开第一个 Tab 就是「打卡」功能页
2. 到配置页填一次存储：git 地址（如 `https://github.com/name/repo.git`）与 Token
   - 不填 git 地址 → 数据保存在 `filesDir/gitbox/local`
   - 私有仓库 → 必须填 Token（GitHub 用 PAT，用户名填 `git` 或自己的用户名）
3. 保存后自动 clone/commit/push；之后每次改动都会直接推送
4. 打卡：新建事项 → 每天点圆形按钮打卡（再点撤销）→ 主页看最近 5 天 → 右上角看记录墙
5. Box 列表里可以切换 Box、修改它的显示名/图标/颜色；Box 本身由代码添加

## 数据怎么存

仓库里：

```
.gitbox/config.json              # 应用配置：主题、各 Box 的显示名/图标
checkin/items.json               # 每个 Box 在仓库根下有自己的目录
checkin/records/2026-10.json     # 打卡记录按月拆分
```

**git 地址与 Token 只保存在本机**（SharedPreferences），永远不进仓库。详细见 [docs/02-storage.md](./docs/02-storage.md)。

## 版本号

版本写在根目录 `gradle.properties`：

```properties
gitboxVersionCode=1
gitboxVersionName=0.0.1
```

`scripts/release.ps1` 会先执行 `./gradlew bumpVersion`（patch +1、versionCode +1，写回 `gradle.properties`）再构建，并注入 `BuildConfig.VERSION_NAME` 显示在设置页。

## 签名

根目录放 `keystore.properties` 即使用正式签名，否则回退 debug 签名（保证随时能出包）：

```properties
storeFile=gitbox.jks
storePassword=******
keyAlias=gitbox
keyPassword=******
```

## 目录结构

```
app/src/main/java/com/v0tx/gitbox
├─ data/        模型、存储后端（git/local）、JGit 封装、仓储
├─ ui/          界面（3 个 Tab、导航、打卡、组件、图标库、主题）
└─ util/        时间工具
scripts/        run.ps1 / release.ps1 / stop.ps1
docs/           详细文档
```

## 文档

- [01 · 产品与设计](./docs/01-overview.md)
- [02 · 存储与 Git 同步](./docs/02-storage.md)
- [03 · 打卡模块](./docs/03-checkin.md)
- [04 · 构建与发布](./docs/04-build.md)
- [05 · 扩展新的小 App](./docs/05-extend.md)

## 技术栈

Kotlin · Jetpack Compose (Material3) · JGit 5.13 · Gson · minSdk 35
