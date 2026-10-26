package io.github.albertrzad18.treningtracker.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import io.github.albertrzad18.treningtracker.data.sync.SyncScheduler
import kotlinx.coroutines.delay

/** Najkrótszy czas kręcenia ikony, żeby szybka synchronizacja (poniżej sekundy) też była widoczna. */
private const val MIN_SPIN_MILLIS = 700L

/** Czy trwa synchronizacja: `true` od jej startu i jeszcze chwilę po końcu. Do animacji przycisków. */
@Composable
fun rememberSyncing(): State<Boolean> {
    val context = LocalContext.current.applicationContext
    return produceState(initialValue = false, context) {
        SyncScheduler.isSyncing(context).collect { running ->
            if (running) {
                value = true
            } else if (value) {
                delay(MIN_SPIN_MILLIS)
                value = false
            }
        }
    }
}
