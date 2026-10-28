package com.v0tx.gitbox.data.repo

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.v0tx.gitbox.data.model.CheckInItem
import com.v0tx.gitbox.data.model.CheckInRecord
import com.v0tx.gitbox.data.model.SyncResult
import com.v0tx.gitbox.data.storage.StorageBackend
import com.v0tx.gitbox.util.monthKey
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 打卡 Box 的数据读写。
 * 目录就是仓库根下的 checkin/，由 StoragePaths.boxDir 决定。
 */
class CheckInRepository(private val backend: StorageBackend, private val boxId: String) {

    private val gson = Gson()
    private val baseDir = StoragePaths.boxDir(boxId)

    private fun itemsPath() = "$baseDir/items.json"
    private fun recordsPath(monthKey: String) = "$baseDir/records/$monthKey.json"

    suspend fun loadItems(): List<CheckInItem> {
        val json = backend.readText(itemsPath()) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<ItemsFile>() {}
            gson.fromJson<ItemsFile>(json, type.type)?.items?.filter { it.id.isNotBlank() }
                ?.sortedBy { it.order } ?: emptyList()
        }.getOrDefault(emptyList())
    }

    suspend fun saveItems(items: List<CheckInItem>, message: String): SyncResult =
        backend.writeFiles(mapOf(itemsPath() to gson.toJson(ItemsFile(items))), message)

    /** 一次性读取多个月份的记录 */
    suspend fun loadRecords(monthKeys: Collection<String>): List<CheckInRecord> {
        val result = mutableListOf<CheckInRecord>()
        monthKeys.distinct().forEach { key ->
            val json = backend.readText(recordsPath(key)) ?: return@forEach
            runCatching {
                val type = object : TypeToken<RecordsFile>() {}
                gson.fromJson<RecordsFile>(json, type.type)?.records
            }.getOrNull()?.let { result.addAll(it) }
        }
        return result
    }

    suspend fun addRecord(record: CheckInRecord): SyncResult {
        val month = LocalDate.ofInstant(
            Instant.ofEpochMilli(record.timestamp), ZoneId.systemDefault()
        ).monthKey()
        val current = loadRecords(listOf(month)).toMutableList()
        current.add(record)
        return backend.writeFiles(
            mapOf(recordsPath(month) to gson.toJson(RecordsFile(month, current))),
            "打卡：${record.itemId}"
        )
    }

    suspend fun deleteRecord(record: CheckInRecord): SyncResult {
        val month = LocalDate.ofInstant(
            Instant.ofEpochMilli(record.timestamp), ZoneId.systemDefault()
        ).monthKey()
        val current = loadRecords(listOf(month)).toMutableList()
        current.removeAll { it.id == record.id }
        return backend.writeFiles(
            mapOf(recordsPath(month) to gson.toJson(RecordsFile(month, current))),
            "删除打卡记录"
        )
    }

    /** 列出已经产生过记录的月份 */
    suspend fun existingMonths(): List<String> =
        backend.listFiles("$baseDir/records").map { it.removeSuffix(".json") }

    private data class ItemsFile(val items: List<CheckInItem> = emptyList())

    private data class RecordsFile(
        val month: String = "",
        val records: List<CheckInRecord> = emptyList()
    )
}
