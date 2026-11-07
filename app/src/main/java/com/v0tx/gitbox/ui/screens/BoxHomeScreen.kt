package com.v0tx.gitbox.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.v0tx.gitbox.data.model.BuiltInBoxes
import com.v0tx.gitbox.ui.MainViewModel
import com.v0tx.gitbox.ui.UiState
import com.v0tx.gitbox.ui.checkin.CheckInHomeScreen
import com.v0tx.gitbox.ui.keymark.KeyMarkHomeScreen
import java.time.LocalDate

/**
 * 第一个 Tab：直接显示当前 Box 的功能。
 * Box 由代码注册（BuiltInBoxes），新增功能时在这里加一个分支。
 */
@Composable
fun BoxHomeScreen(
    state: UiState,
    vm: MainViewModel,
    onOpenWall: () -> Unit,
    onOpenDay: (LocalDate) -> Unit
) {
    when (state.currentBoxId) {
        BuiltInBoxes.CHECK_IN.id -> CheckInHomeScreen(
            state = state,
            vm = vm,
            onOpenWall = onOpenWall,
            onOpenDay = onOpenDay
        )

        BuiltInBoxes.KEY_MARK.id -> KeyMarkHomeScreen(state = state, vm = vm)

        else -> NotImplementedBox(state)
    }
}

@Composable
private fun NotImplementedBox(state: UiState) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "「${state.currentBox.name}」还没实现功能",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp)
        )
    }
}
