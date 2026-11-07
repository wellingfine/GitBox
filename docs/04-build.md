# 构建与发布

完全脱离 Android Studio，用命令行 + 脚本完成编译、装到模拟器、出包。

## 1. 环境准备

| 依赖 | 说明 |
| --- | --- |
| JDK 17 | `java -version` 可验证；AGP 9 要求 JDK 17+ |
| Android SDK | 需要 `platforms;android-37`、`build-tools;36+`、`platform-tools`、`emulator` |
| 环境变量 | 设置 `ANDROID_HOME`（例如 `D:\software\android\sdk`），脚本会读取它；未设置时回退到 `%LOCALAPPDATA%\Android\Sdk` |

Gradle 不需要单独安装，用仓库自带的 `gradlew`（Gradle 9.6.0 已随 wrapper 提供）。

## 2. 脚本一览

所有脚本在 `scripts/` 下，Windows PowerShell 直接运行：

```powershell
# 拉起模拟器 + 编译 debug + 安装 + 启动 App
powershell -ExecutionPolicy Bypass -File .\scripts\run.ps1

# 指定 AVD
powershell -ExecutionPolicy Bypass -File .\scripts\run.ps1 -Avd Medium_Phone_API_36.1

# 模拟器已经开着（或插着真机），跳过启动
powershell -ExecutionPolicy Bypass -File .\scripts\run.ps1 -SkipEmulator

# 装 release 包而不是 debug
powershell -ExecutionPolicy Bypass -File .\scripts\run.ps1 -Release

# 版本号 +1 并出 release 包
powershell -ExecutionPolicy Bypass -File .\scripts\release.ps1

# 出包后顺手装到当前设备
powershell -ExecutionPolicy Bypass -File .\scripts\release.ps1 -Install

# 关闭正在运行的模拟器
powershell -ExecutionPolicy Bypass -File .\scripts\stop.ps1
```

如果提示“禁止运行脚本”，加 `-ExecutionPolicy Bypass` 即可（上面示例都已带上）。

### run.ps1 做了什么

1. 定位 SDK（`$ANDROID_HOME` → `%LOCALAPPDATA%\Android\Sdk`）
2. 若没有在线的 `emulator-*` 设备，就用 `emulator -avd <name>` 启动（不指定 `-Avd` 时取列表里的第一个），并轮询 `sys.boot_completed` 等开机完成
3. `gradlew :app:assembleDebug`（或 `assembleRelease`）
4. `adb install -r` 安装、`am start` 启动 `com.v0tx.gitbox/.MainActivity`

> AVD 需要提前创建一次：`avdmanager create avd -n GitBox -k "system-images;android-36;google_apis;x86_64" -d pixel`

## 3. 版本号自动 +1

版本写在 **gradle.properties**：

```properties
gitboxVersionCode=1
gitboxVersionName=0.0.1
```

- `app/build.gradle.kts` 通过 `providers.gradleProperty(...)` 读取，作为 `versionCode` / `versionName`
- 根工程提供 `bumpVersion` 任务：`patch` 位 +1、`versionCode` +1，并写回 `gradle.properties`
  ```bash
  ./gradlew bumpVersion      # 0.0.1 (1) -> 0.0.2 (2)
  ```
- `scripts/release.ps1` 会**先 bump 再构建**，所以每次出包版本号都会自动前进一位

版本号同时注入 `BuildConfig.VERSION_NAME`，设置页最后一行展示为：

```
make by v0tx [v0.0.1]
```

想手动指定版本，直接改 `gradle.properties` 即可（`release.ps1 -NoBump` 可以跳过自增）。

## 4. 签名

`app/build.gradle.kts` 会在根目录存在 `keystore.properties` 时使用正式签名，否则**回退到 debug 签名**，保证任何时刻都能出包。

```properties
# keystore.properties（不要提交到 git）
storeFile=gitbox.jks
storePassword=******
keyAlias=gitbox
keyPassword=******
```

生成 keystore：

```bash
keytool -genkey -v -keystore gitbox.jks -keyalg RSA -keysize 2048 -validity 10000 -alias gitbox
```

建议把 `keystore.properties`、`*.jks`、`*.keystore` 加进 `.gitignore`（已包含）。

## 5. 产物路径

每次 assemble 完成后，APK 会自动复制到仓库最外层的 `build/`（已在 `.gitignore` 中忽略）。

| 构建 | 路径 |
| --- | --- |
| debug | `build/GitBox.<版本>.debug.apk` |
| release | `build/GitBox.<版本>.release.apk` |

Gradle 原始输出仍在 `app/build/outputs/apk/<debug|release>/`，`scripts/run.ps1` 从那里取包安装。文件名里的版本号来自 `gradle.properties`。

## 6. 常用 Gradle 命令

```bash
./gradlew debug                   # 编译 debug 包
./gradlew release                 # 编译 release 包（不会自增版本号）
./gradlew bumpVersion             # 版本号 +1
./gradlew :app:installDebug       # 编译并安装（需要已连接设备）
./gradlew :app:lint               # 静态检查
```

## 7. 注意事项 / 已知坑

- **AGP 9 已内置 Kotlin Android 插件**：不要再在 `plugins {}` 里声明 `org.jetbrains.kotlin.android`，否则会报 `Cannot add extension with name 'kotlin'`；Compose 编译器插件仍需显式声明，版本要跟 AGP 内置的 Kotlin 一致（当前 2.2.10）
- **配置缓存**：`org.gradle.configuration-cache=true` 已开启。自定义任务里不要在 `doLast` 引用脚本级函数/属性，否则会报 “cannot serialize Gradle script object references”
- **minSdk 35**：只能在 Android 15+ 设备上安装；模拟器镜像请选择 API 35 及以上
- **仓库依赖**：首次构建需要从 Google / Maven Central 下载 Compose、JGit 等依赖，请保持网络可用
- **不要配置 `optimization.packageScope`**：这是给「尚未使用 R8 的应用」做渐进迁移的实验功能，只优化指定包、其余代码原样保留。配了它要么因第三方库被重打包而启动闪退（`androidx.startup` 抛 `IllegalAccessError`），要么因依赖库完全不裁剪让 APK 从约 3MB 涨到 43MB。直接用全量 `optimization { enable = true }`，反射相关的问题靠 keep 规则解决
- **Gson 与混淆**：`app/proguard-rules.pro` keep 了 `data.model.**` 和 `data.repo.**`。Gson 按字段名反射读写 JSON，字段名被混淆后 release 读不出已有数据，写进仓库的 JSON 也会变成 `a` / `b` 这种键名；`data.repo` 里的 `ItemsFile` / `RecordsFile` 包装类尤其容易漏掉
- **JGit 的缺失类**：全量 R8 会对 JGit 5.13 引用的 `java.lang.management` / `javax.management` / `org.ietf.jgss` / slf4j 类报 Missing classes（Android 平台没有），已在 `proguard-rules.pro` 里 `-dontwarn` 掉；本项目只用 HTTP(S)+Token，不走这些路径
