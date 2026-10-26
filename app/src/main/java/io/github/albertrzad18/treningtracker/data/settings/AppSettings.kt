package io.github.albertrzad18.treningtracker.data.settings

import android.content.Context
import io.github.albertrzad18.treningtracker.domain.DEFAULT_MAX_HEART_RATE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Motyw apki: zgodnie z systemem, zawsze jasny albo zawsze ciemny. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Karty, które można pokazać albo schować w Ustawieniach (Dashboard, Miesiąc i Treningi). */
enum class HomeCard { POOL, ROAD, TRAINER, WEEK, LATEST, GOALS }

/** Ustawienia użytkownika: tętno maksymalne (do stref), długość basenu, motyw i schowane karty. */
data class Settings(
    val maxHeartRate: Int = DEFAULT_MAX_HEART_RATE,
    val poolLengthM: Int = 25,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val hiddenCards: Set<HomeCard> = emptySet(),
) {
    fun isVisible(card: HomeCard) = card !in hiddenCards
}

class AppSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<Settings> = _state.asStateFlow()

    fun setMaxHeartRate(value: Int) = save { putInt(KEY_MAX_HR, value.coerceIn(120, 230)) }

    fun setPoolLength(meters: Int) = save { putInt(KEY_POOL, meters) }

    fun setThemeMode(mode: ThemeMode) = save { putString(KEY_THEME, mode.name) }

    /** Pokazuje albo chowa kartę; zapisujemy tylko schowane, więc nowa karta w kolejnej wersji jest domyślnie widoczna. */
    fun setCardVisible(card: HomeCard, visible: Boolean) {
        val hidden = _state.value.hiddenCards.let { if (visible) it - card else it + card }
        save { putStringSet(KEY_HIDDEN_CARDS, hidden.map { it.name }.toSet()) }
    }

    private fun save(block: android.content.SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(block).apply()
        _state.value = read()
    }

    private fun read() = Settings(
        maxHeartRate = prefs.getInt(KEY_MAX_HR, DEFAULT_MAX_HEART_RATE),
        poolLengthM = prefs.getInt(KEY_POOL, 25),
        themeMode = runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, null) ?: "") }
            .getOrDefault(ThemeMode.SYSTEM),
        hiddenCards = prefs.getStringSet(KEY_HIDDEN_CARDS, emptySet()).orEmpty()
            .mapNotNull { name -> HomeCard.entries.firstOrNull { it.name == name } }
            .toSet(),
    )

    private companion object {
        const val KEY_MAX_HR = "max_heart_rate"
        const val KEY_POOL = "pool_length"
        const val KEY_THEME = "theme_mode"
        const val KEY_HIDDEN_CARDS = "hidden_cards"
    }
}

/** Karta podsumowania odpowiadająca dyscyplinie. */
fun io.github.albertrzad18.treningtracker.domain.Discipline.card(): HomeCard = when (this) {
    io.github.albertrzad18.treningtracker.domain.Discipline.POOL -> HomeCard.POOL
    io.github.albertrzad18.treningtracker.domain.Discipline.ROAD -> HomeCard.ROAD
    io.github.albertrzad18.treningtracker.domain.Discipline.TRAINER -> HomeCard.TRAINER
}
