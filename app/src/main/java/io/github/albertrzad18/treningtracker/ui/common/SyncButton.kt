package io.github.albertrzad18.treningtracker.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.data.sync.SyncScheduler
import io.github.albertrzad18.treningtracker.ui.theme.LocalTtColors
import io.github.albertrzad18.treningtracker.ui.theme.TtShape

/**
 * Okrągły przycisk „Synchronizuj” dostępny na każdym głównym widoku. Ikona kręci się, gdy trwa synchronizacja,
 * a w tym czasie kolejne dotknięcia są ignorowane.
 */
@Composable
fun SyncButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = LocalTtColors.current
    val syncing by rememberSyncing()
    val label = stringResource(R.string.sync_now)
    Box(
        modifier
            .size(52.dp)
            .clip(TtShape.full)
            .background(colors.accent)
            .ttClickable(onClick = { if (!syncing) SyncScheduler.syncNow(context) })
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            TtIcons.Sync,
            contentDescription = null,
            tint = colors.onAccent,
            modifier = Modifier.size(24.dp).rotate(spinAngle(syncing)),
        )
    }
}

/**
 * Pociągnięcie w dół na samej górze listy uruchamia synchronizację (jak w innych apkach).
 * Wskaźnik pokazuje się, dopóki trwa synchronizacja.
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
