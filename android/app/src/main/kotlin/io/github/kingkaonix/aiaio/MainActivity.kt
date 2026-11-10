package io.github.kingkaonix.aiaio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.kingkaonix.aiaio.ui.AiaioApp
import io.github.kingkaonix.aiaio.ui.theme.AiaioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiaioTheme {
                AiaioApp()
            }
        }
    }
}
