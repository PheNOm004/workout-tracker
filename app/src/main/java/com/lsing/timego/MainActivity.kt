package com.lsing.timego

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.lsing.timego.ui.TimeGoNavHost
import com.lsing.timego.ui.theme.TimeGoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(android.graphics.Color.parseColor("#0D1113"))
        enableEdgeToEdge()
        setContent {
            TimeGoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TimeGoNavHost()
                }
            }
        }
    }
}
