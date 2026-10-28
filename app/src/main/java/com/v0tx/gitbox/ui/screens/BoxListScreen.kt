package com.v0tx.gitbox.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
import com.v0tx.gitbox.data.model.BoxUi
import com.v0tx.gitbox.ui.MainViewModel
import com.v0tx.gitbox.ui.UiState
import com.v0tx.gitbox.ui.components.AppIconView
import com.v0tx.gitbox.ui.components.AppTopBar
import com.v0tx.gitbox.ui.components.HintText
import com.v0tx.gitbox.ui.components.SectionTitle
import com.v0tx.gitbox.ui.components.TagChip

/** Box 列表：Box 由代码内置，这里只负责切换与改外观，不能增删 */
@Composable
fun BoxListScreen(
    state: UiState,
    vm: MainViewModel,
    onEdit: (String) -> Unit,
    onSelected: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item { AppTopBar(title = "Box 列表") }
        item { SectionTitle("内置功能 · ${state.boxes.size} 个") }
        items(state.boxes, key = { it.id }) { box ->
            BoxRow(
                box = box,
                selected = box.id == state.currentBoxId,
                onSelect = {
                    vm.selectBox(box.id)
                    onSelected()
                },
                onEdit = { onEdit(box.id) }
            )
        }
        item { HintText("Box 是内置的小功能，只能在这里切换与修改外观；新功能由代码添加。") }
    }
}

@Composable
private fun BoxRow(
    box: BoxUi,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconView(iconKey = box.icon, colorHex = box.colorHex, size = 44)
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    text = box.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = box.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (selected) {
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        TagChip(text = "当前", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "修改外观")
            }
        }
    }
}
