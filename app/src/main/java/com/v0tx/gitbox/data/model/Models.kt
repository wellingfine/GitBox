package com.v0tx.gitbox.data.model

/**
 * Box = 一个内置的小 app 功能，由代码注册，用户不能增删。
 * 想加新功能时在这里加一条 BoxDef（见 docs/05-extend.md）。
 */
data class BoxDef(
    val id: String,
    val name: String,
    val icon: String,
    val colorHex: String,
    val description: String
)

object BuiltInBoxes {
    val CHECK_IN = BoxDef(
        id = "checkin",
        name = "打卡",
        icon = "check",
        colorHex = "#4CAF50",
        description = "每天点一下，记录习惯与坚持"
    )

    /** 所有内置 Box，新增功能时往这里加 */
    val ALL: List<BoxDef> = listOf(CHECK_IN)

    val DEFAULT_ID: String = CHECK_IN.id

    fun byId(id: String?): BoxDef? = ALL.firstOrNull { it.id == id }
}

/** 用户对某个 Box 的显示设置（可缺省，缺省时用 BoxDef 的默认值） */
data class BoxSetting(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val colorHex: String = ""
)

/** Box 定义 + 用户设置 合并后的显示模型 */
data class BoxUi(
    val id: String,
    val name: String,
    val icon: String,
    val colorHex: String,
    val description: String
)

fun BoxDef.with(setting: BoxSetting?): BoxUi = BoxUi(
    id = id,
    name = setting?.name?.takeIf { it.isNotBlank() } ?: name,
    icon = setting?.icon?.takeIf { it.isNotBlank() } ?: icon,
    colorHex = setting?.colorHex?.takeIf { it.isNotBlank() } ?: colorHex,
    description = description
)

/** 应用自身的配置：除 git 地址与 token 外都保存在 git 上 */
data class AppConfig(
    val version: Int = 1,
    val themeMode: String = THEME_MODE_SYSTEM,
    val boxes: List<BoxSetting> = emptyList()
) {
    companion object {
        const val THEME_MODE_SYSTEM = "system"
        const val THEME_MODE_LIGHT = "light"
        const val THEME_MODE_DARK = "dark"
    }

    fun settingFor(boxId: String): BoxSetting? = boxes.firstOrNull { it.id == boxId }
}

/**
 * 全局唯一的存储设置：仅保存在本机，绝不写入 git。
 * gitUrl 为空时表示使用本机目录存储。
 */
data class StorageConfig(
    val gitUrl: String = "",
    val token: String = "",
    val branch: String = "main",
    val userName: String = "git",
    val authorName: String = "GitBox",
    val authorEmail: String = "gitbox@gitbox.local"
) {
    val isGit: Boolean
        get() = gitUrl.isNotBlank()
}

/** 本机保存的内容：存储设置 + 当前 Box */
data class LocalSettings(
    val storage: StorageConfig = StorageConfig(),
    val currentBoxId: String = BuiltInBoxes.DEFAULT_ID
)

/** 打卡事项 */
data class CheckInItem(
    val id: String = "",
    val name: String = "",
    val icon: String = "check",
    val colorHex: String = "#4CAF50",
    val createdAt: Long = 0L,
    val archived: Boolean = false,
    val order: Int = 0
)

/** 一次打卡记录，timestamp 精确到毫秒，可查看具体时间 */
data class CheckInRecord(
    val id: String = "",
    val itemId: String = "",
    val timestamp: Long = 0L,
    val note: String = ""
)

/** 同步状态 */
enum class SyncStatus { IDLE, WORKING, OK, ERROR }

data class SyncState(
    val status: SyncStatus = SyncStatus.IDLE,
    val message: String = "",
    val lastSyncAt: Long = 0L
)

data class SyncResult(val ok: Boolean, val message: String)
