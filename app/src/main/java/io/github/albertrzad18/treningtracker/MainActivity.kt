package io.github.albertrzad18.treningtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.albertrzad18.treningtracker.ui.theme.TreningTrackerTheme
import io.github.albertrzad18.treningtracker.ui.debug.DebugScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TreningTrackerTheme {
                DebugScreen()
            }
        }
    }
}
