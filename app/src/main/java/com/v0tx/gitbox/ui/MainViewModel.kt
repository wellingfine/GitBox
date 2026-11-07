package com.v0tx.gitbox.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.v0tx.gitbox.data.model.AppConfig
import com.v0tx.gitbox.data.model.BoxSetting
import com.v0tx.gitbox.data.model.BoxUi
import com.v0tx.gitbox.data.model.BuiltInBoxes
import com.v0tx.gitbox.data.model.CheckInItem
import com.v0tx.gitbox.data.model.CheckInRecord
import com.v0tx.gitbox.data.model.KeyMark
import com.v0tx.gitbox.data.model.LocalSettings
import com.v0tx.gitbox.data.model.StorageConfig
import com.v0tx.gitbox.data.model.SyncResult
import com.v0tx.gitbox.data.model.SyncState
import com.v0tx.gitbox.data.model.SyncStatus
import com.v0tx.gitbox.data.model.with
import com.v0tx.gitbox.data.repo.CheckInRepository
import com.v0tx.gitbox.data.repo.KeyMarkRepository
import com.v0tx.gitbox.data.repo.StorageRepository
import com.v0tx.gitbox.data.storage.StorageBackend
import com.v0tx.gitbox.util.monthKey
import com.v0tx.gitbox.util.toLocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

data class CheckInState(
    val items: List<CheckInItem> = emptyList(),
    val records: List<CheckInRecord> = emptyList(),
    val loading: Boolean = false
)

data class KeyMarkState(
    val records: List<KeyMark> = emptyList(),
    val loading: Boolean = false
)

data class UiState(
    val ready: Boolean = false,
    val settings: LocalSettings = LocalSettings(),
    val config: AppConfig = AppConfig(),
    val currentBoxId: String = BuiltInBoxes.DEFAULT_ID,
    val sync: SyncState = SyncState(),
    val checkIn: CheckInState = CheckInState(),
    val keyMark: KeyMarkState = KeyMarkState()
) {
    /** Box 由代码定义，这里合并用户在 git 配置里保存的显示设置 */
    val boxes: List<BoxUi>
        get() = BuiltInBoxes.ALL.map { it.with(config.settingFor(it.id)) }

    val currentBox: BoxUi
        get() = BuiltInBoxes.byId(currentBoxId)?.with(config.settingFor(currentBoxId))
            ?: BuiltInBoxes.CHECK_IN.with(null)
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = StorageRepository(app)
    private var backend: StorageBackend? = null
    private var checkInRepo: CheckInRepository? = null
    private var keyMarkRepo: KeyMarkRepository? = null
    private var lastMarkRejectTipAt = 0L

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        viewModelScope.launch { bootstrap() }
    }

    // ---------- 启动 ----------

    private suspend fun bootstrap() {
        val settings = repo.loadSettings()
        val storage = repo.backendFor(settings.storage)
        backend = storage
        checkInRepo = CheckInRepository(storage, BuiltInBoxes.CHECK_IN.id)
        keyMarkRepo = KeyMarkRepository(storage, BuiltInBoxes.KEY_MARK.id)
        _state.update {
            it.copy(settings = settings, currentBoxId = settings.currentBoxId, ready = true)
        }
        loadConfigInternal()
        loadBoxDataInternal(settings.currentBoxId)
        if (settings.storage.isGit) syncInternal()
    }

    private fun currentBackend(): StorageBackend =
        backend ?: repo.backendFor(_state.value.settings.storage).also { backend = it }

    private suspend fun loadConfigInternal() {
        val config = withContext(Dispatchers.IO) { repo.loadConfig(currentBackend()) }
        _state.update { it.copy(config = config) }
    }

    private suspend fun loadBoxDataInternal(boxId: String) {
        when (boxId) {
            BuiltInBoxes.CHECK_IN.id -> loadCheckInInternal()
            BuiltInBoxes.KEY_MARK.id -> loadKeyMarkInternal()
        }
    }

    // ---------- Box 选择（Box 由代码注册，只能切换不能增删） ----------

    fun selectBox(boxId: String) {
        viewModelScope.launch {
            val settings = repo.loadSettings().copy(currentBoxId = boxId)
            repo.saveSettings(settings)
            _state.update { it.copy(settings = settings, currentBoxId = boxId) }
            loadBoxDataInternal(boxId)
        }
    }

    /** 修改 Box 的显示名/图标/颜色，保存在 git 配置里 */
    fun updateBoxSetting(boxId: String, name: String, icon: String, colorHex: String) {
        viewModelScope.launch {
            val setting = BoxSetting(id = boxId, name = name.trim(), icon = icon, colorHex = colorHex)
            val config = _state.value.config.copy(
                boxes = _state.value.config.boxes.filter { it.id != boxId } + setting
            )
            _state.update { it.copy(config = config) }
            val result = withContext(Dispatchers.IO) { repo.saveConfig(currentBackend(), config) }
            _events.tryEmit(if (result.ok) "已保存到存储" else "保存失败：${result.message}")
        }
    }

    fun resetBoxSetting(boxId: String) {
        viewModelScope.launch {
            val config = _state.value.config.copy(boxes = _state.value.config.boxes.filter { it.id != boxId })
            _state.update { it.copy(config = config) }
            withContext(Dispatchers.IO) { repo.saveConfig(currentBackend(), config) }
        }
    }

    // ---------- 存储设置（全局一份） ----------

    fun updateStorage(cfg: StorageConfig) {
        viewModelScope.launch {
            val settings = repo.loadSettings().copy(storage = cfg)
            repo.saveSettings(settings)
            backend = withContext(Dispatchers.IO) { repo.backendFor(cfg) }
            checkInRepo = CheckInRepository(backend!!, BuiltInBoxes.CHECK_IN.id)
            keyMarkRepo = KeyMarkRepository(backend!!, BuiltInBoxes.KEY_MARK.id)
            _state.update {
                it.copy(settings = settings, sync = SyncState(SyncStatus.WORKING, "连接中…"))
            }
            val result = withContext(Dispatchers.IO) { backend!!.pull() }
            _state.update {
                it.copy(
                    sync = SyncState(
                        status = if (result.ok) SyncStatus.OK else SyncStatus.ERROR,
                        message = result.message,
                        lastSyncAt = System.currentTimeMillis()
                    )
                )
            }
            _events.tryEmit(if (result.ok) "存储设置已保存" else "同步失败：${result.message}")
            loadConfigInternal()
            loadBoxDataInternal(_state.value.currentBoxId)
        }
    }

    fun syncNow() {
        viewModelScope.launch { syncInternal() }
    }

    private suspend fun syncInternal() {
        _state.update { it.copy(sync = SyncState(SyncStatus.WORKING, "同步中…")) }
        val result = withContext(Dispatchers.IO) { currentBackend().pull() }
        _state.update {
            it.copy(
                sync = SyncState(
                    status = if (result.ok) SyncStatus.OK else SyncStatus.ERROR,
                    message = result.message,
                    lastSyncAt = System.currentTimeMillis()
                )
            )
        }
        if (result.ok) {
            loadConfigInternal()
            loadBoxDataInternal(_state.value.currentBoxId)
        } else {
            _events.tryEmit("同步失败：${result.message}")
        }
    }

    fun pushNow() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { currentBackend().pushPending() }
            _events.tryEmit(if (result.ok) "已推送" else "推送失败：${result.message}")
        }
    }

    /** 配置页「保存配置」：把攒在本地的改动一次性提交并推送 */
    fun saveConfigNow() {
        viewModelScope.launch {
            _state.update { it.copy(sync = SyncState(SyncStatus.WORKING, "提交中…")) }
            val result = withContext(Dispatchers.IO) { currentBackend().commitAll("保存 GitBox 配置") }
            _state.update {
                it.copy(
                    sync = SyncState(
                        status = if (result.ok) SyncStatus.OK else SyncStatus.ERROR,
                        message = result.message,
                        lastSyncAt = System.currentTimeMillis()
                    )
                )
            }
            _events.tryEmit(result.message)
        }
    }

    fun resetLocalRepo() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.resetLocal() }
            backend = null
            checkInRepo = null
            keyMarkRepo = null
            _events.tryEmit("本地仓库已重置，重新拉取中")
            bootstrap()
        }
    }

    // ---------- 配置 ----------

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            val config = _state.value.config.copy(themeMode = mode)
            _state.update { it.copy(config = config) }
            withContext(Dispatchers.IO) { repo.saveConfig(currentBackend(), config) }
        }
    }

    // ---------- 打卡（checkin Box） ----------

    fun loadCheckIn(boxId: String = BuiltInBoxes.CHECK_IN.id) {
        if (boxId != BuiltInBoxes.CHECK_IN.id) return
        viewModelScope.launch { loadCheckInInternal() }
    }

    private suspend fun loadCheckInInternal() {
        _state.update { it.copy(checkIn = it.checkIn.copy(loading = true)) }
        val data = withContext(Dispatchers.IO) {
            val cr = checkInRepo ?: return@withContext Pair(emptyList<CheckInItem>(), emptyList<CheckInRecord>())
            val items = cr.loadItems()
            val months = recentMonthKeys(13) + cr.existingMonths()
            val records = cr.loadRecords(months)
            Pair(items, records)
        }
        _state.update {
            it.copy(checkIn = CheckInState(items = data.first, records = data.second))
        }
    }

    fun addCheckInItem(name: String, icon: String, colorHex: String) {
        viewModelScope.launch {
            val items = _state.value.checkIn.items.toMutableList()
            items.add(
                CheckInItem(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    icon = icon,
                    colorHex = colorHex,
                    createdAt = System.currentTimeMillis(),
                    order = items.size
                )
            )
            saveCheckInItems(items, "新增打卡事项：$name")
        }
    }

    fun updateCheckInItem(item: CheckInItem) {
        viewModelScope.launch {
            val items = _state.value.checkIn.items.map { if (it.id == item.id) item else it }
            saveCheckInItems(items, "更新打卡事项：${item.name}")
        }
    }

    fun deleteCheckInItem(itemId: String) {
        viewModelScope.launch {
            val items = _state.value.checkIn.items.filter { it.id != itemId }
            saveCheckInItems(items, "删除打卡事项")
        }
    }

    /** 今天已打卡则撤销，否则打卡 */
    fun toggleCheckIn(itemId: String) {
        viewModelScope.launch {
            val cr = checkInRepo ?: return@launch
            val today = LocalDate.now()
            val existing = _state.value.checkIn.records.filter {
                it.itemId == itemId && it.timestamp.toLocalDate() == today
            }
            val records = _state.value.checkIn.records.toMutableList()
            if (existing.isNotEmpty()) {
                val target = existing.maxByOrNull { it.timestamp } ?: return@launch
                records.remove(target)
                _state.update { it.copy(checkIn = it.checkIn.copy(records = records)) }
                val result = withContext(Dispatchers.IO) { cr.deleteRecord(target) }
                _events.tryEmit(if (result.ok) "已撤销今天的打卡" else "撤销失败：${result.message}")
            } else {
                val record = CheckInRecord(
                    id = UUID.randomUUID().toString(),
                    itemId = itemId,
                    timestamp = System.currentTimeMillis()
                )
                records.add(record)
                _state.update { it.copy(checkIn = it.checkIn.copy(records = records)) }
                val result = withContext(Dispatchers.IO) { cr.addRecord(record) }
                _events.tryEmit(if (result.ok) "打卡成功" else "打卡已记录，但同步失败：${result.message}")
            }
        }
    }

    fun deleteCheckInRecord(recordId: String) {
        viewModelScope.launch {
            val cr = checkInRepo ?: return@launch
            val target = _state.value.checkIn.records.firstOrNull { it.id == recordId } ?: return@launch
            val records = _state.value.checkIn.records.filter { it.id != recordId }
            _state.update { it.copy(checkIn = it.checkIn.copy(records = records)) }
            val result = withContext(Dispatchers.IO) { cr.deleteRecord(target) }
            _events.tryEmit(if (result.ok) "已删除记录" else "删除失败：${result.message}")
        }
    }

    private suspend fun saveCheckInItems(items: List<CheckInItem>, message: String) {
        val cr = checkInRepo ?: return
        _state.update { it.copy(checkIn = it.checkIn.copy(items = items)) }
        val result = withContext(Dispatchers.IO) { cr.saveItems(items, message) }
        if (!result.ok) _events.tryEmit("保存失败：${result.message}")
    }

    // ---------- 打点（keymark Box） ----------

    /** 屏幕常亮开关，只存本机，切页面时由界面负责清除 flag */
    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            val settings = repo.loadSettings().copy(keepScreenOn = enabled)
            repo.saveSettings(settings)
            _state.update { it.copy(settings = settings) }
        }
    }

    private suspend fun loadKeyMarkInternal() {
        _state.update { it.copy(keyMark = it.keyMark.copy(loading = true)) }
        val records = withContext(Dispatchers.IO) {
            val kr = keyMarkRepo ?: return@withContext emptyList<KeyMark>()
            kr.load(recentMonthKeys(13) + kr.existingMonths())
        }
        _state.update {
            it.copy(keyMark = KeyMarkState(records = records.sortedByDescending { it.timestamp }))
        }
    }

    /** 按一下记下当前时间；两次打点至少隔 1 秒，分隔线不受限 */
    fun addKeyMark(kind: String = KeyMark.KIND_MARK) {
        viewModelScope.launch {
            val kr = keyMarkRepo ?: return@launch
            val now = System.currentTimeMillis()
            if (kind == KeyMark.KIND_MARK) {
                val last = _state.value.keyMark.records
                    .firstOrNull { !it.isSeparator }?.timestamp ?: 0L
                val elapsed = now - last
                if (elapsed in 0 until 1000L) {
                    if (now - lastMarkRejectTipAt >= 1000L) {
                        lastMarkRejectTipAt = now
                        _events.tryEmit("两次打点至少间隔 1 秒")
                    }
                    return@launch
                }
            }
            val record = KeyMark(
                id = UUID.randomUUID().toString(),
                timestamp = now,
                kind = kind
            )
            val records = (_state.value.keyMark.records + record).sortedByDescending { it.timestamp }
            _state.update { it.copy(keyMark = it.keyMark.copy(records = records)) }
            val result = withContext(Dispatchers.IO) { kr.add(record) }
            if (!result.ok) _events.tryEmit("已记录，但同步失败：${result.message}")
        }
    }

    /** 直接删除，不做二次确认 */
    fun deleteKeyMark(recordId: String) {
        viewModelScope.launch {
            val kr = keyMarkRepo ?: return@launch
            val target = _state.value.keyMark.records.firstOrNull { it.id == recordId } ?: return@launch
            val records = _state.value.keyMark.records.filter { it.id != recordId }
            _state.update { it.copy(keyMark = it.keyMark.copy(records = records)) }
            val result = withContext(Dispatchers.IO) { kr.delete(target) }
            if (!result.ok) _events.tryEmit("删除失败：${result.message}")
        }
    }

    companion object {
        fun recentMonthKeys(count: Int): List<String> {
            val now = LocalDate.now()
            return (0 until count).map { now.minusMonths(it.toLong()).monthKey() }
        }
    }
}
