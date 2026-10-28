package com.v0tx.gitbox.data.storage

import com.v0tx.gitbox.data.git.GitEngine
import com.v0tx.gitbox.data.model.StorageConfig
import com.v0tx.gitbox.data.model.SyncResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** git 存储：写入文件后立即 commit + push */
class GitBackend(private val workDir: File, private val cfg: StorageConfig) : StorageBackend {

    override val kind: StorageKind = StorageKind.GIT

    private val engine = GitEngine(workDir, cfg)

    private fun resolve(path: String): File {
        val target = File(workDir, path)
        require(target.canonicalPath.startsWith(workDir.canonicalPath)) { "非法路径: $path" }
        return target
    }

    override suspend fun readText(path: String): String? = withContext(Dispatchers.IO) {
        val file = resolve(path)
        if (!file.exists()) null else runCatching { file.readText() }.getOrNull()
    }

    override suspend fun writeFiles(files: Map<String, String>, message: String): SyncResult =
        withContext(Dispatchers.IO) {
            val written = runCatching {
                files.forEach { (path, content) ->
                    val file = resolve(path)
                    file.parentFile?.mkdirs()
                    file.writeText(content)
                }
            }
            if (written.isFailure) {
                return@withContext SyncResult(false, written.exceptionOrNull()?.message ?: "写入失败")
            }
            val result = engine.commitAndPush(message)
            if (result.ok) return@withContext result
            // 推送失败通常是远端有新提交，先拉取再重试一次
            val pulled = engine.pull()
            if (!pulled.ok) return@withContext result
            val retry = engine.pushPending()
            if (retry.ok) SyncResult(true, "已保存并推送") else SyncResult(false, retry.message)
        }

    override suspend fun listFiles(dir: String): List<String> = withContext(Dispatchers.IO) {
        val folder = resolve(dir)
        if (!folder.exists()) emptyList() else folder.listFiles()?.map { it.name }?.sorted() ?: emptyList()
    }

    override suspend fun pull(): SyncResult = withContext(Dispatchers.IO) { engine.pull() }

    override suspend fun pushPending(): SyncResult = withContext(Dispatchers.IO) { engine.pushPending() }

    override suspend fun deleteFile(path: String): SyncResult = withContext(Dispatchers.IO) {
        val file = resolve(path)
        runCatching { if (file.exists()) file.delete() }
        engine.commitAndPush("删除 $path")
    }

    fun resetLocal(): Boolean = engine.resetLocal()
}
