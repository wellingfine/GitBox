package com.v0tx.gitbox.data.storage

import com.v0tx.gitbox.data.model.SyncResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 未配置 git 时的本地存储：直接写到 app 私有目录 */
class LocalBackend(private val rootDir: File) : StorageBackend {

    override val kind: StorageKind = StorageKind.LOCAL

    private fun resolve(path: String): File {
        val target = File(rootDir, path)
        val canonicalRoot = rootDir.canonicalPath
        val canonicalTarget = target.canonicalPath
        require(canonicalTarget.startsWith(canonicalRoot)) { "非法路径: $path" }
        return target
    }

    override suspend fun readText(path: String): String? = withContext(Dispatchers.IO) {
        val file = resolve(path)
        if (!file.exists()) null else runCatching { file.readText() }.getOrNull()
    }

    override suspend fun writeFiles(files: Map<String, String>, message: String): SyncResult =
        withContext(Dispatchers.IO) {
            runCatching {
                files.forEach { (path, content) ->
                    val file = resolve(path)
                    file.parentFile?.mkdirs()
                    file.writeText(content)
                }
            }.fold(
                onSuccess = { SyncResult(true, "已保存到本地") },
                onFailure = { SyncResult(false, it.message ?: "保存失败") }
            )
        }

    override suspend fun listFiles(dir: String): List<String> = withContext(Dispatchers.IO) {
        val folder = resolve(dir)
        if (!folder.exists()) return@withContext emptyList()
        folder.listFiles()?.map { it.name }?.sorted() ?: emptyList()
    }

    override suspend fun pull(): SyncResult = SyncResult(true, "本地存储无需同步")

    override suspend fun pushPending(): SyncResult = SyncResult(true, "本地存储无需同步")

    override suspend fun deleteFile(path: String): SyncResult = withContext(Dispatchers.IO) {
        val file = resolve(path)
        val ok = !file.exists() || file.delete()
        SyncResult(ok, if (ok) "已删除" else "删除失败")
    }
}
