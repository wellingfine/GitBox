package com.v0tx.gitbox.data.repo

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.v0tx.gitbox.data.model.KeyMark
import com.v0tx.gitbox.data.model.SyncResult
import com.v0tx.gitbox.data.storage.StorageBackend
import com.v0tx.gitbox.util.monthKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 打点 Box 的数据读写，记录按月拆分到 keymark/records/<yyyy-MM>.json。
 */
class KeyMarkRepository(private val backend: StorageBackend, private val boxId: String) {

    private val gson = Gson()
    private val baseDir = StoragePaths.boxDir(boxId)
    private val writeLock = Mutex()

    private fun recordsPath(monthKey: String) = "$baseDir/records/$monthKey.json"

    private fun monthOf(timestamp: Long): String =
        LocalDate.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).monthKey()

    suspend fun load(monthKeys: Collection<String>): List<KeyMark> {
        val result = mutableListOf<KeyMark>()
        monthKeys.distinct().forEach { key ->
            val json = backend.readText(recordsPath(key)) ?: return@forEach
            runCatching {
                val type = object : TypeToken<RecordsFile>() {}
                gson.fromJson<RecordsFile>(json, type.type)?.records
            }.getOrNull()?.let { result.addAll(it) }
        }
        return result
    }

    /** 连续点按会并发写同一个月文件，用锁包住读改写，避免互相覆盖 */
    suspend fun add(record: KeyMark): SyncResult = writeLock.withLock {
        val month = monthOf(record.timestamp)
        val current = load(listOf(month)).toMutableList()
        current.add(record)
        backend.writeFiles(
            mapOf(recordsPath(month) to gson.toJson(RecordsFile(month, current))),
            if (record.isSeparator) "打点：加分隔线" else "打点：记录时间"
        )
    }

    suspend fun delete(record: KeyMark): SyncResult = writeLock.withLock {
        val month = monthOf(record.timestamp)
        val current = load(listOf(month)).toMutableList()
        current.removeAll { it.id == record.id }
        backend.writeFiles(
            mapOf(recordsPath(month) to gson.toJson(RecordsFile(month, current))),
            "打点：删除记录"
        )
    }

    suspend fun existingMonths(): List<String> =
        backend.listFiles("$baseDir/records").map { it.removeSuffix(".json") }

    private data class RecordsFile(
        val month: String = "",
        val records: List<KeyMark> = emptyList()
    )
}
