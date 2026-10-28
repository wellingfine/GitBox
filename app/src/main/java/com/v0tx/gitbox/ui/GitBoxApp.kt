package com.v0tx.gitbox.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v0tx.gitbox.ui.checkin.CheckInDayScreen
import com.v0tx.gitbox.ui.checkin.CheckInHomeScreen
import com.v0tx.gitbox.ui.checkin.CheckInWallScreen
import com.v0tx.gitbox.ui.nav.Screen
import com.v0tx.gitbox.ui.nav.TAB_ROOTS
import com.v0tx.gitbox.ui.screens.BoxAppearanceScreen
import com.v0tx.gitbox.ui.screens.BoxHomeScreen
import com.v0tx.gitbox.ui.screens.BoxListScreen
import com.v0tx.gitbox.ui.screens.SettingsScreen

@Composable
fun GitBoxApp(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.events.collect { snackbarHostState.showSnackbar(it) }
    }

    var tab by remember { mutableIntStateOf(0) }
    val stack = remember { mutableStateListOf<Screen>(Screen.BoxHome) }

    fun switchTab(index: Int) {
        tab = index
        stack.clear()
        stack.add(TAB_ROOTS[index])
    }

    fun navigate(screen: Screen) {
        stack.add(screen)
    }

    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    BackHandler(enabled = stack.size > 1 || tab != 0) {
        if (stack.size > 1) pop() else switchTab(0)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                TAB_ROOTS.forEachIndexed { index, root ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { if (tab != index) switchTab(index) },
                        icon = {
                            Icon(
                                imageVector = when (root) {
                                    Screen.BoxHome -> Icons.Filled.Home
                                    Screen.BoxList -> Icons.Filled.Apps
                                    else -> Icons.Filled.Settings
                                },
                                contentDescription = when (root) {
                                    Screen.BoxHome -> "Box"
                                    Screen.BoxList -> "Box 列表"
                                    else -> "配置"
                                }
                            )
                        }
                    )
                }
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (val screen = stack.last()) {
                Screen.BoxHome -> BoxHomeScreen(
                    state = state,
                    vm = vm,
                    onOpenWall = { navigate(Screen.CheckInWall) },
                    onOpenDay = { navigate(Screen.CheckInDay(it)) }
                )

                Screen.BoxList -> BoxListScreen(
                    state = state,
                    vm = vm,
                    onEdit = { navigate(Screen.BoxAppearance(it)) },
                    onSelected = { switchTab(0) }
                )

                Screen.Settings -> SettingsScreen(state = state, vm = vm)

                is Screen.BoxAppearance -> BoxAppearanceScreen(
                    state = state,
                    vm = vm,
                    boxId = screen.boxId,
                    onBack = { pop() }
                )

                is Screen.CheckInDay -> CheckInDayScreen(
                    state = state,
                    vm = vm,
                    date = screen.date,
                    onBack = { pop() }
                )

                Screen.CheckInWall -> CheckInWallScreen(
                    state = state,
                    vm = vm,
                    onBack = { pop() },
                    onOpenDay = { navigate(Screen.CheckInDay(it)) }
                )
            }
        }
    }
}
