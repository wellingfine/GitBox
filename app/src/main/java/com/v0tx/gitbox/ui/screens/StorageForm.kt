package com.v0tx.gitbox.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.v0tx.gitbox.data.model.StorageConfig

/** 全局唯一的存储设置表单：只保存在本机，不会写入 git */
class StorageFormState {
    var url: String by mutableStateOf("")
    var token: String by mutableStateOf("")
    var branch: String by mutableStateOf("main")
    var userName: String by mutableStateOf("git")
    var authorName: String by mutableStateOf("GitBox")
    var authorEmail: String by mutableStateOf("gitbox@gitbox.local")

    fun load(cfg: StorageConfig) {
        url = cfg.gitUrl
        token = cfg.token
        branch = cfg.branch.ifBlank { "main" }
        userName = cfg.userName.ifBlank { "git" }
        authorName = cfg.authorName.ifBlank { "GitBox" }
        authorEmail = cfg.authorEmail.ifBlank { "gitbox@gitbox.local" }
    }

    fun toConfig(): StorageConfig = StorageConfig(
        gitUrl = url.trim(),
        token = token.trim(),
        branch = branch.trim().ifBlank { "main" },
        userName = userName.trim().ifBlank { "git" },
        authorName = authorName.trim().ifBlank { "GitBox" },
        authorEmail = authorEmail.trim().ifBlank { "gitbox@gitbox.local" }
    )
}

@Composable
fun StorageFormFields(form: StorageFormState) {
    var tokenVisible by mutableStateOf(false)

    OutlinedTextField(
        value = form.url,
        onValueChange = { form.url = it },
        label = { Text("Git 地址") },
        placeholder = { Text("https://github.com/name/repo.git") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )
    OutlinedTextField(
        value = form.token,
        onValueChange = { form.token = it },
        label = { Text("Token（私有仓库必填）") },
        placeholder = { Text("ghp_xxx / glpat-xxx") },
        singleLine = true,
        visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { tokenVisible = !tokenVisible }) {
                Icon(
                    imageVector = if (tokenVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = if (tokenVisible) "隐藏" else "显示"
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )
    OutlinedTextField(
        value = form.branch,
        onValueChange = { form.branch = it },
        label = { Text("分支") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )
    OutlinedTextField(
        value = form.userName,
        onValueChange = { form.userName = it },
        label = { Text("认证用户名") },
        supportingText = { Text("GitHub 通常填 git 或你的用户名") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )
    OutlinedTextField(
        value = form.authorName,
        onValueChange = { form.authorName = it },
        label = { Text("提交作者名") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )
    OutlinedTextField(
        value = form.authorEmail,
        onValueChange = { form.authorEmail = it },
        label = { Text("提交作者邮箱") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )
    Text(
        text = "地址留空时数据只保存在本机；Token 只会写进本机配置，不会提交到 git。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}
