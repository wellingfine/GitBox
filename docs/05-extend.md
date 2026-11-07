# 05 · 新增一个 Box（小功能）

Box 是代码内置的功能，用户不能增删。新增一个 Box（例如 `hobbit`）只需 5 步，全部在代码里完成。

## 1. 注册 BoxDef

`data/model/Models.kt` 的 `BuiltInBoxes`：

```kotlin
val HOBBIT = BoxDef(
    id = "hobbit",
    name = "习惯",
    icon = "yoga",
    colorHex = "#8BC34A",
    description = "养成好习惯"
)

val ALL = listOf(CHECK_IN, HOBBIT)
```

注册后它会自动出现在「Box 列表」Tab 里。

## 2. 定义数据与仓储

- 数据类放 `data/model/`
- 仓储放 `data/repo/XxxRepository(backend, boxId)`，路径用 `StoragePaths.boxFile(boxId, name)`，即仓库根下的 `hobbit/…`
- 参照 `CheckInRepository`：小文件一个 JSON；按时间累积的数据怎么拆由该 Box 自己决定（打卡、打点目前都按月拆文件，框架不强制）

## 3. 接入 ViewModel

在 `MainViewModel`：

- `loadBoxDataInternal()` 的 `when (boxId)` 里加分支，加载数据到 `UiState`
- 增加对应的操作方法（参照打卡的 `toggleCheckIn()`），写入后由 `StorageBackend` 自动 commit + push

## 4. 写界面并挂到第一个 Tab

- 界面放 `ui/<box>/`，子页面类型加到 `ui/nav/Screen.kt`，并在 `ui/GitBoxApp.kt` 的 `when` 里加分支
- 在 `ui/screens/BoxHomeScreen.kt` 的 `when (state.currentBoxId)` 里加分支，让第一个 Tab 直接显示这个 Box 的主页

## 5. 完成

不需要数据库迁移、不需要用户操作；老用户升级后，Box 列表里自动多出「习惯」，数据目录 `hobbit/` 会在第一次保存时创建并推送。

## 约定

- 数据只写自己的目录（`hobbit/…`），不要碰别的 Box
- 时间戳用毫秒 `Long`，展示用 `util/TimeUtil.kt`
- 提交信息写清楚，如 `"打卡：晨跑"`，方便在 git 历史里检索
- 不要读写 git 地址与 Token；`.gitbox/config.json` 的读写统一走 `StorageRepository`
