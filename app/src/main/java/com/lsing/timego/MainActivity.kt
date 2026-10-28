package com.lsing.timego

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.lsing.timego.data.SettingsRepository
import com.lsing.timego.data.ThemeMode
import com.lsing.timego.ui.TimeGoNavHost
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.lsing.timego.ui.theme.TimeGoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(android.graphics.Color.parseColor("#0E0F12"))
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val settings = SettingsRepository(applicationContext)
        // Read once up front (a few bytes from DataStore) so a light-mode user never sees a dark flash.
        val initialMode = runBlocking { settings.themeMode.first() }
        setContent {
            val mode by settings.themeMode.collectAsState(initial = initialMode)
            val darkTheme = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            DisposableEffect(darkTheme) {
                val transparent = android.graphics.Color.TRANSPARENT
                val barStyle = if (darkTheme) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                window.decorView.setBackgroundColor(android.graphics.Color.parseColor(if (darkTheme) "#0D1113" else "#F2F0EC"))
                onDispose {}
            }
            TimeGoTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TimeGoNavHost()
                }
            }
        }
    }
}
