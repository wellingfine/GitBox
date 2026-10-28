package com.v0tx.gitbox.ui.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.v0tx.gitbox.data.model.CheckInItem
import com.v0tx.gitbox.data.model.CheckInRecord
import com.v0tx.gitbox.data.model.SyncStatus
import com.v0tx.gitbox.ui.MainViewModel
import com.v0tx.gitbox.ui.UiState
import com.v0tx.gitbox.ui.components.AppIconView
import com.v0tx.gitbox.ui.components.AppTopBar
import com.v0tx.gitbox.ui.components.ColorPickerRow
import com.v0tx.gitbox.ui.components.EmptyHint
import com.v0tx.gitbox.ui.components.HintText
import com.v0tx.gitbox.ui.components.SectionTitle
import com.v0tx.gitbox.ui.components.TagChip
import com.v0tx.gitbox.ui.icons.BUILT_IN_ICONS
import com.v0tx.gitbox.ui.icons.DEFAULT_ITEM_ICON_KEY
import com.v0tx.gitbox.ui.icons.DEFAULT_COLOR
import com.v0tx.gitbox.ui.icons.iconImage
import com.v0tx.gitbox.util.displayTime
import com.v0tx.gitbox.util.toLocalDate
import com.v0tx.gitbox.util.weekLabel
import java.time.LocalDate

@Composable
fun CheckInHomeScreen(
    state: UiState,
    vm: MainViewModel,
    onOpenWall: () -> Unit,
    onOpenDay: (LocalDate) -> Unit
) {
    val items = state.checkIn.items
    val records = state.checkIn.records
    val itemMap = items.associateBy { it.id }
    val today = LocalDate.now()
    val todayRecords = records.filter { it.timestamp.toLocalDate() == today }
    val recentDays = (0..4).map { today.minusDays(it.toLong()) }

    var editingItem by remember { mutableStateOf<CheckInItem?>(null) }
    var creating by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                AppTopBar(
                    title = state.currentBox.name,
                    actions = {
                        IconButton(onClick = { vm.syncNow() }) {
                            if (state.sync.status == SyncStatus.WORKING) {
                                CircularProgressIndicator()
                            } else {
                                Icon(Icons.Filled.Refresh, contentDescription = "同步")
                            }
                        }
                        IconButton(onClick = onOpenWall) {
                            Icon(Icons.Filled.GridView, contentDescription = "记录墙")
                        }
                    }
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "今天 · ${today.monthValue} 月 ${today.dayOfMonth} 日 · ${today.weekLabel()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "已完成 ${todayRecords.size} / ${items.size} 项",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            item { SectionTitle("事项") }
            if (items.isEmpty()) {
                item { EmptyHint("还没有打卡事项，新建一个开始记录吧。") }
            } else {
                items(items, key = { it.id }) { item ->
                    CheckInItemCard(
                        item = item,
                        todayRecord = todayRecords.firstOrNull { it.itemId == item.id },
                        streak = streakOf(records, item.id),
                        total = records.count { it.itemId == item.id },
                        onToggle = { vm.toggleCheckIn(item.id) },
                        onEdit = { editingItem = item }
                    )
                }
            }
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    OutlinedButton(onClick = { creating = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text("新建事项", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }

            item { SectionTitle("最近 5 天") }
            items(recentDays) { date ->
                val dayRecords = records.filter { it.timestamp.toLocalDate() == date }
                DayRow(
                    date = date,
                    records = dayRecords,
                    itemMap = itemMap,
                    onClick = { onOpenDay(date) }
                )
            }
            item { HintText("点击任意一天可以查看打卡的具体时间。") }
        }

        if (creating) {
            CheckInItemDialog(
                title = "新建事项",
                initial = null,
                onDismiss = { creating = false },
                onSave = { name, icon, color ->
                    vm.addCheckInItem(name, icon, color)
                    creating = false
                },
                onDelete = null
            )
        }
        editingItem?.let { item ->
            CheckInItemDialog(
                title = "编辑事项",
                initial = item,
                onDismiss = { editingItem = null },
                onSave = { name, icon, color ->
                    vm.updateCheckInItem(item.copy(name = name, icon = icon, colorHex = color))
                    editingItem = null
                },
                onDelete = {
                    vm.deleteCheckInItem(item.id)
                    editingItem = null
                }
            )
        }
    }
}

@Composable
private fun CheckInItemCard(
    item: CheckInItem,
    todayRecord: CheckInRecord?,
    streak: Int,
    total: Int,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconView(iconKey = item.icon, colorHex = item.colorHex, size = 44)
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    TagChip(text = "连续 $streak 天")
                    TagChip(text = "共 $total 次")
                    if (todayRecord != null) {
                        TagChip(
                            text = "今日 ${todayRecord.timestamp.displayTime()}",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (todayRecord != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = if (todayRecord != null) "撤销打卡" else "打卡",
                    tint = if (todayRecord != null) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DayRow(
    date: LocalDate,
    records: List<CheckInRecord>,
    itemMap: Map<String, CheckInItem>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (date) {
                        LocalDate.now() -> "今天"
                        LocalDate.now().minusDays(1) -> "昨天"
                        else -> "${date.monthValue}/${date.dayOfMonth}"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${date.weekLabel()} · ${records.size} 次打卡",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                records.sortedBy { it.timestamp }.forEach { record ->
                    val item = itemMap[record.itemId]
                    AppIconView(
                        iconKey = item?.icon ?: DEFAULT_ITEM_ICON_KEY,
                        colorHex = item?.colorHex ?: DEFAULT_COLOR,
                        size = 30
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckInItemDialog(
    title: String,
    initial: CheckInItem?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var iconKey by remember { mutableStateOf(initial?.icon ?: DEFAULT_ITEM_ICON_KEY) }
    var colorHex by remember { mutableStateOf(initial?.colorHex ?: DEFAULT_COLOR) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIconView(iconKey = iconKey, colorHex = colorHex, size = 40)
                    Column(modifier = Modifier.padding(start = 10.dp)) {
                        Text("图标", style = MaterialTheme.typography.bodySmall)
                        Row {
                            BUILT_IN_ICONS.take(12).forEach { icon ->
                                IconButton(onClick = { iconKey = icon.key }) {
                                    Icon(
                                        imageVector = iconImage(icon.key),
                                        contentDescription = icon.label,
                                        tint = if (icon.key == iconKey) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                ColorPickerRow(selected = colorHex, onSelect = { colorHex = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onSave(name.trim(), iconKey, colorHex)
            }) { Text("保存") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}

private fun streakOf(records: List<CheckInRecord>, itemId: String): Int {
    val days = records.filter { it.itemId == itemId }
        .map { it.timestamp.toLocalDate() }
        .toSet()
    if (days.isEmpty()) return 0
    var cursor = LocalDate.now()
    if (cursor !in days) cursor = cursor.minusDays(1)
    var count = 0
    while (cursor in days) {
        count++
        cursor = cursor.minusDays(1)
    }
    return count
}
