package com.v0tx.gitbox.ui.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.v0tx.gitbox.data.model.CheckInItem
import com.v0tx.gitbox.data.model.CheckInRecord
import com.v0tx.gitbox.ui.MainViewModel
import com.v0tx.gitbox.ui.UiState
import com.v0tx.gitbox.ui.components.AppIconView
import com.v0tx.gitbox.ui.components.AppTopBar
import com.v0tx.gitbox.ui.components.EmptyHint
import com.v0tx.gitbox.ui.components.SectionTitle
import com.v0tx.gitbox.ui.components.TagChip
import com.v0tx.gitbox.ui.icons.DEFAULT_COLOR
import com.v0tx.gitbox.ui.icons.DEFAULT_ITEM_ICON_KEY
import com.v0tx.gitbox.util.displayTime
import com.v0tx.gitbox.util.toLocalDate
import com.v0tx.gitbox.util.weekLabel
import java.time.LocalDate

@Composable
fun CheckInDayScreen(
    state: UiState,
    vm: MainViewModel,
    date: LocalDate,
    onBack: () -> Unit
) {
    val records = state.checkIn.records
        .filter { it.timestamp.toLocalDate() == date }
        .sortedBy { it.timestamp }
    val itemMap = state.checkIn.items.associateBy { it.id }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            AppTopBar(
                title = "${date.monthValue}月${date.dayOfMonth}日 · ${date.weekLabel()}",
                onBack = onBack
            )
        }
        if (records.isEmpty()) {
            item { EmptyHint("这一天还没有打卡记录。") }
        } else {
            item { SectionTitle("共 ${records.size} 次打卡") }
            items(records, key = { it.id }) { record ->
                RecordRow(
                    record = record,
                    item = itemMap[record.itemId],
                    onDelete = { vm.deleteCheckInRecord(record.id) }
                )
            }
        }
    }
}

@Composable
private fun RecordRow(
    record: CheckInRecord,
    item: CheckInItem?,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconView(
                iconKey = item?.icon ?: DEFAULT_ITEM_ICON_KEY,
                colorHex = item?.colorHex ?: DEFAULT_COLOR,
                size = 38
            )
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    text = item?.name ?: "已删除的事项",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    TagChip(text = record.timestamp.displayTime(), color = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "删除记录")
            }
        }
    }
}
