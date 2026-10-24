package io.github.albertrzad18.treningtracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme

// Stałe kolory dyscyplin: używane wszędzie (tabele, wykresy, kafelki).
val PoolBlue = Color(0xFF1E88E5)
val RoadGreen = Color(0xFF43A047)
val TrainerOrange = Color(0xFFFB8C00)

@Composable
fun TreningTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colorScheme) {
        // Wykresy (Vico) biorą kolory osi i opisów z motywu Material 3, więc działają w jasnym i ciemnym.
        ProvideVicoTheme(rememberM3VicoTheme(), content)
    }
}
