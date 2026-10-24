package io.github.albertrzad18.treningtracker.data.settings

import android.content.Context
import io.github.albertrzad18.treningtracker.domain.DEFAULT_MAX_HEART_RATE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Motyw apki: zgodnie z systemem, zawsze jasny albo zawsze ciemny. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Ustawienia użytkownika: tętno maksymalne (do stref), długość basenu i motyw. */
data class Settings(
    val maxHeartRate: Int = DEFAULT_MAX_HEART_RATE,
    val poolLengthM: Int = 25,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

class AppSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<Settings> = _state.asStateFlow()

    fun setMaxHeartRate(value: Int) = save { putInt(KEY_MAX_HR, value.coerceIn(120, 230)) }

    fun setPoolLength(meters: Int) = save { putInt(KEY_POOL, meters) }

    fun setThemeMode(mode: ThemeMode) = save { putString(KEY_THEME, mode.name) }

    private fun save(block: android.content.SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(block).apply()
        _state.value = read()
    }

    private fun read() = Settings(
        maxHeartRate = prefs.getInt(KEY_MAX_HR, DEFAULT_MAX_HEART_RATE),
        poolLengthM = prefs.getInt(KEY_POOL, 25),
        themeMode = runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, null) ?: "") }
            .getOrDefault(ThemeMode.SYSTEM),
    )

    private companion object {
        const val KEY_MAX_HR = "max_heart_rate"
        const val KEY_POOL = "pool_length"
        const val KEY_THEME = "theme_mode"
    }
}
