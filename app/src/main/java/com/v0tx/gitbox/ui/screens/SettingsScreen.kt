package com.v0tx.gitbox.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.v0tx.gitbox.BuildConfig
import com.v0tx.gitbox.data.model.AppConfig
import com.v0tx.gitbox.data.model.SyncStatus
import com.v0tx.gitbox.ui.MainViewModel
import com.v0tx.gitbox.ui.UiState
import com.v0tx.gitbox.ui.components.AppTopBar
import com.v0tx.gitbox.ui.components.ConfirmDialog
import com.v0tx.gitbox.ui.components.HintText
import com.v0tx.gitbox.ui.components.SectionTitle
import com.v0tx.gitbox.ui.components.TagChip

@Composable
fun SettingsScreen(state: UiState, vm: MainViewModel) {
    val storage = state.settings.storage
    val form = remember(storage) { StorageFormState().apply { load(storage) } }
    var showReset by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item { AppTopBar(title = "配置") }

        item { SectionTitle("存储") }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row {
                        TagChip(
                            text = if (storage.isGit) "Git 存储" else "本机目录",
                            color = if (storage.isGit) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.tertiary
                        )
                    }
                    if (storage.isGit) {
                        Text(
                            text = storage.gitUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Text(
                            text = "分支：${storage.branch}　Token：${if (storage.token.isBlank()) "未填写" else "已配置"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "未配置 git 地址，数据保存在本机目录中",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    state.sync.let { sync ->
                        if (sync.message.isNotBlank()) {
                            Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 6.dp)
                            ) {
                                if (sync.status == SyncStatus.WORKING) {
                                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                                }
                                Text(
                                    text = "同步：${sync.message}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when (sync.status) {
                                        SyncStatus.ERROR -> MaterialTheme.colorScheme.error
                                        SyncStatus.OK -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        OutlinedButton(onClick = { vm.syncNow() }) { Text("拉取") }
                        OutlinedButton(onClick = { vm.pushNow() }) { Text("推送") }
                        OutlinedButton(onClick = { showReset = true }) { Text("重置本地") }
                    }
                    Button(
                        onClick = { vm.saveConfigNow() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) { Text("保存配置") }
                    Text(
                        text = "改动只保存在本机，点「保存配置」才一次性提交并推送；启动时会自动尝试拉取，本地未提交的改动不会被覆盖。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        item { SectionTitle("存储设置") }
        item { StorageFormFields(form) }
        item {
            Button(
                onClick = { vm.updateStorage(form.toConfig()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) { Text("保存存储设置") }
        }

        item { SectionTitle("外观") }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                ThemeChip("跟随系统", state.config.themeMode == AppConfig.THEME_MODE_SYSTEM) {
                    vm.setThemeMode(AppConfig.THEME_MODE_SYSTEM)
                }
                ThemeChip("浅色", state.config.themeMode == AppConfig.THEME_MODE_LIGHT) {
                    vm.setThemeMode(AppConfig.THEME_MODE_LIGHT)
                }
                ThemeChip("深色", state.config.themeMode == AppConfig.THEME_MODE_DARK) {
                    vm.setThemeMode(AppConfig.THEME_MODE_DARK)
                }
            }
        }

        item { SectionTitle("数据存放方式") }
        item {
            HintText(
                "仓库目录结构：\n" +
                    "· .gitbox/config.json —— 应用配置（主题、各 Box 的显示名/图标）\n" +
                    "· checkin/ —— 打卡的数据目录，每个 Box 一个\n" +
                    "· git 地址与 Token 只保存在本机，不会提交到仓库"
            )
        }

        item { SectionTitle("关于") }
        item { HintText("GitBox —— 用 git 仓库当云存储的小应用集合") }
        item {
            Text(
                text = "make by v0tx [v${BuildConfig.VERSION_NAME}]",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            )
        }
    }

    if (showReset) {
        ConfirmDialog(
            title = "重置本地仓库",
            text = "将删除本机仓库副本并重新拉取，未推送的提交会丢失，确定继续吗？",
            confirmLabel = "重置",
            destructive = true,
            onConfirm = { vm.resetLocalRepo() },
            onDismiss = { showReset = false }
        )
    }
}

@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}
