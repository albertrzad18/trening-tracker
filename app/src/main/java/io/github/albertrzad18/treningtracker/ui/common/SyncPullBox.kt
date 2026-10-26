package io.github.albertrzad18.treningtracker.ui.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.albertrzad18.treningtracker.data.sync.SyncScheduler

/**
 * Pociągnięcie w dół na samej górze listy uruchamia synchronizację (jak w innych apkach).
 * Wskaźnik pokazuje się, dopóki trwa synchronizacja. Owija każdy główny ekran w `AppRoot`.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SyncPullBox(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val syncing by rememberSyncing()
    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = syncing,
        onRefresh = { SyncScheduler.syncNow(context) },
        modifier = Modifier.fillMaxSize(),
    ) { content() }
}
