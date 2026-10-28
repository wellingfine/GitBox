package com.v0tx.gitbox.data.repo

import android.content.Context
import com.google.gson.Gson
import com.v0tx.gitbox.data.model.AppConfig
import com.v0tx.gitbox.data.model.LocalSettings
import com.v0tx.gitbox.data.model.StorageConfig
import com.v0tx.gitbox.data.model.SyncResult
import com.v0tx.gitbox.data.storage.GitBackend
import com.v0tx.gitbox.data.storage.LocalBackend
import com.v0tx.gitbox.data.storage.StorageBackend
import java.io.File

/** 仓库内的目录约定：应用配置在 .gitbox 下，每个 Box 直接用 boxId 做自己的目录 */
object StoragePaths {
    /** 应用自身的配置（除 git 地址与 token 外都在这里） */
    const val CONFIG = ".gitbox/config.json"

    /** 每个 Box 在仓库里的专属目录，例如 checkin/ */
    fun boxDir(boxId: String): String = boxId

    fun boxFile(boxId: String, name: String): String = "${boxDir(boxId)}/$name"
}

/**
 * 全局只有一份存储设置（git 仓库或本机目录），所有 Box 共用它。
 * 本机只保存存储设置与当前 Box；其余配置都在仓库的 .gitbox/config.json 里。
 */
class StorageRepository(context: Context) {

    private val gson = Gson()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val rootDir = File(context.filesDir, "gitbox")
    private val repoDir = File(rootDir, "repo").apply { mkdirs() }
    private val localDir = File(rootDir, "local").apply { mkdirs() }

    fun loadSettings(): LocalSettings {
        val json = prefs.getString(KEY_SETTINGS, null) ?: return LocalSettings()
        return runCatching { gson.fromJson(json, LocalSettings::class.java) }.getOrNull()
            ?: LocalSettings()
    }

    fun saveSettings(settings: LocalSettings) {
        prefs.edit().putString(KEY_SETTINGS, gson.toJson(settings)).apply()
    }

    fun backendFor(cfg: StorageConfig): StorageBackend =
        if (cfg.isGit) GitBackend(repoDir, cfg) else LocalBackend(localDir)

    suspend fun loadConfig(backend: StorageBackend): AppConfig {
        val json = backend.readText(StoragePaths.CONFIG) ?: return AppConfig()
        return runCatching { gson.fromJson(json, AppConfig::class.java) }.getOrNull()?.let {
            it.copy(
                themeMode = it.themeMode.ifBlank { AppConfig.THEME_MODE_SYSTEM },
                boxes = it.boxes.filter { s -> s.id.isNotBlank() }
            )
        } ?: AppConfig()
    }

    suspend fun saveConfig(backend: StorageBackend, config: AppConfig): SyncResult =
        backend.writeFiles(mapOf(StoragePaths.CONFIG to gson.toJson(config)), "更新 GitBox 配置")

    fun resetLocal(): Boolean = runCatching { repoDir.deleteRecursively() }.isSuccess

    companion object {
        private const val PREFS_NAME = "gitbox_local"
        private const val KEY_SETTINGS = "settings"
    }
}
