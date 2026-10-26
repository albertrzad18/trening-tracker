package io.github.albertrzad18.treningtracker.data.sync

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Stan synchronizacji zapisany w SharedPreferences: token Changes API, czas ostatniej synchronizacji, ostatni błąd. */
class SyncPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("sync", Context.MODE_PRIVATE)

    var changesToken: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    /** Czy ostatni pełny odczyt miał uprawnienie do historii; bez niego Health Connect pokazuje tylko 30 dni wstecz. */
    var fullReadHadHistory: Boolean
        get() = prefs.getBoolean(KEY_FULL_HISTORY, false)
        set(value) = prefs.edit().putBoolean(KEY_FULL_HISTORY, value).apply()

    private val _status = MutableStateFlow(readStatus())

    /** Obserwowalny status do ekranu „Więcej”. */
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    fun saveSuccess(timeMillis: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC, timeMillis).remove(KEY_LAST_ERROR).apply()
        _status.value = readStatus()
    }

    fun saveError(message: String) {
        prefs.edit().putString(KEY_LAST_ERROR, message).apply()
        _status.value = readStatus()
    }

    private fun readStatus() = SyncStatus(
        lastSyncMillis = prefs.getLong(KEY_LAST_SYNC, 0L).takeIf { it > 0 },
        lastError = prefs.getString(KEY_LAST_ERROR, null),
    )

    private companion object {
        const val KEY_TOKEN = "changes_token"
        const val KEY_FULL_HISTORY = "full_read_had_history"
        const val KEY_LAST_SYNC = "last_sync"
        const val KEY_LAST_ERROR = "last_error"
    }
}

data class SyncStatus(val lastSyncMillis: Long?, val lastError: String?)
