package com.v0tx.gitbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.v0tx.gitbox.data.model.AppConfig
import com.v0tx.gitbox.ui.GitBoxApp
import com.v0tx.gitbox.ui.MainViewModel
import com.v0tx.gitbox.ui.theme.GitBoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()
            val state by vm.state.collectAsStateWithLifecycle()
            val darkTheme = when (state.config.themeMode) {
                AppConfig.THEME_MODE_DARK -> true
                AppConfig.THEME_MODE_LIGHT -> false
                else -> isSystemInDarkTheme()
            }
            GitBoxTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GitBoxApp(vm = vm)
                }
            }
        }
    }
}
