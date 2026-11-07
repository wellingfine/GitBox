# 01 · 产品与设计

## 核心概念

- **Box = 一个内置的小 app 功能**（当前有「打卡」「打点」，之后按需在代码里加新的）。用户**不能增删 Box**，只能在 Box 列表里切换，或改它的显示名 / 图标 / 颜色。
- **存储全局只有一份**：一个 git 仓库（或本机目录），所有 Box 共用；每个 Box 在仓库根下有自己的目录（`checkin/`、将来的 `hobbit/`…）。
- **git 地址与 Token 只存本机**，其余配置（主题、各 Box 的显示设置）保存在仓库的 `.gitbox/config.json`。

## 三个 Tab（只显示图标，无文字）

| Tab | 作用 |
| --- | --- |
| Home | 直接进入当前 Box 的功能页（如打卡主页） |
| Apps | 内置 Box 列表：切换当前 Box、修改外观 |
| 齿轮 | 配置：存储设置、拉取/推送/重置、主题、版本 |

在 Apps Tab 里选中某个 Box 后会自动跳回第一个 Tab 进入它的功能。

## Box 注册（代码）

```kotlin
object BuiltInBoxes {
    val CHECK_IN = BoxDef("checkin", "打卡", "check", "#4CAF50", "每天点一下，记录习惯与坚持")
    val ALL = listOf(CHECK_IN)      // 新增功能时在这里加
}
```

- `BoxDef`：id、默认名称、图标、颜色、描述
- 用户可以在「外观设置」里覆盖名称 / 图标 / 颜色，覆盖值存在 `.gitbox/config.json` 的 `boxes` 列表里
- 第一个 Tab 按 `currentBoxId` 分发到对应功能页（`ui/screens/BoxHomeScreen.kt`），新 Box 在这里加分支

## 导航实现

`ui/GitBoxApp.kt` 用简单的返回栈（不引入 Navigation 组件）：

- 切 Tab：清空栈放入该 Tab 的根页面（`TAB_ROOTS`）
- 子页面（打卡详情、记录墙、外观设置）：push 到栈里
- `NavigationBarItem` 不传 `label`，只有图标

## 状态管理

- 单 Activity + Compose，`MainViewModel` 持有 `UiState`：
  - `settings`（本机：存储配置 + 当前 Box）
  - `config`（git 上的应用配置）
  - `currentBoxId`、`sync`（全局唯一的同步状态）、`checkIn`（打卡数据）
- 事件通过 `SharedFlow<String>` 送到 Snackbar

## 模块结构

```
com.v0tx.gitbox
├─ data/
│  ├─ model/      BoxDef / BoxSetting / AppConfig / StorageConfig / CheckIn*
│  ├─ storage/    StorageBackend 接口 + GitBackend + LocalBackend
│  ├─ git/        GitEngine（JGit 封装）
│  └─ repo/       StorageRepository（本机设置 + .gitbox/config.json）、CheckInRepository
├─ ui/            GitBoxApp / MainViewModel / nav / screens / checkin / components / icons / theme
└─ util/          时间工具
```
