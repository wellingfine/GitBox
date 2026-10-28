package com.v0tx.gitbox.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.v0tx.gitbox.ui.MainViewModel
import com.v0tx.gitbox.ui.UiState
import com.v0tx.gitbox.ui.components.AppIconView
import com.v0tx.gitbox.ui.components.AppTopBar
import com.v0tx.gitbox.ui.components.ColorPickerRow
import com.v0tx.gitbox.ui.components.HintText
import com.v0tx.gitbox.ui.components.IconPickerDialog
import com.v0tx.gitbox.ui.components.SectionTitle
import com.v0tx.gitbox.ui.icons.BUILT_IN_ICONS

/** 修改 Box 的显示名 / 图标 / 颜色，保存在 git 上的 .gitbox/config.json */
@Composable
fun BoxAppearanceScreen(
    state: UiState,
    vm: MainViewModel,
    boxId: String,
    onBack: () -> Unit
) {
    val box = state.boxes.firstOrNull { it.id == boxId } ?: state.currentBox
    var name by remember { mutableStateOf(box.name) }
    var iconKey by remember { mutableStateOf(box.icon) }
    var colorHex by remember { mutableStateOf(box.colorHex) }
    var showIconPicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item { AppTopBar(title = "外观设置", onBack = onBack) }
            item { SectionTitle("基本信息") }
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { showIconPicker = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIconView(iconKey = iconKey, colorHex = colorHex, size = 48)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text("图标", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "点击更换（内置 ${BUILT_IN_ICONS.size} 个图标）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item { ColorPickerRow(selected = colorHex, onSelect = { colorHex = it }) }
            item {
                Button(
                    onClick = {
                        vm.updateBoxSetting(boxId, name, iconKey, colorHex)
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) { Text("保存") }
            }
            item {
                TextButton(
                    onClick = {
                        vm.resetBoxSetting(boxId)
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text("恢复默认外观", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { HintText("外观设置会保存在 .gitbox/config.json 里，跟着 git 走。") }
        }

        if (showIconPicker) {
            IconPickerDialog(
                selectedKey = iconKey,
                onDismiss = { showIconPicker = false },
                onSelect = { iconKey = it.key }
            )
        }
    }
}
