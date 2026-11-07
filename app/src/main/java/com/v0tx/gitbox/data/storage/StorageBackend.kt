package com.v0tx.gitbox.data.storage

import com.v0tx.gitbox.data.model.SyncResult

/**
 * 统一存储抽象：上层小 app 只关心读写文件，
 * 由具体实现决定数据是落到 git 仓库还是本地目录。
 */
interface StorageBackend {

    /** git 或 local */
    val kind: StorageKind

    suspend fun readText(path: String): String?

    /**
     * 批量写入文件，写完后立即提交并推送（git 模式）。
     * 返回同步结果，失败时数据仍在本地仓库/本地目录中，不会丢失。
     */
    suspend fun writeFiles(files: Map<String, String>, message: String): SyncResult

    suspend fun listFiles(dir: String): List<String>

    /** 从远端拉取最新内容 */
    suspend fun pull(): SyncResult

    /** 把本地未推送的提交推送上去 */
    suspend fun pushPending(): SyncResult

    /** 把攒在本地的改动一次性提交并推送（配置页「保存配置」） */
    suspend fun commitAll(message: String): SyncResult

    suspend fun deleteFile(path: String): SyncResult
}

enum class StorageKind { GIT, LOCAL }
