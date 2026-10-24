package io.github.albertrzad18.treningtracker.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.TreningApp
import io.github.albertrzad18.treningtracker.data.sync.SyncScheduler
import io.github.albertrzad18.treningtracker.ui.format.formatDateTime
import java.time.Instant
import java.time.ZoneId

@Composable
fun MoreScreen(onOpenDiagnostics: () -> Unit) {
    val context = LocalContext.current
    val prefs = (context.applicationContext as TreningApp).syncPrefs
    val status by prefs.status.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.more_sync_title), style = MaterialTheme.typography.titleLarge)
        val last = status.lastSyncMillis?.let {
            formatDateTime(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime())
        }
        Text(
            if (last != null) stringResource(R.string.more_last_sync, last) else stringResource(R.string.more_never),
            style = MaterialTheme.typography.bodyLarge,
        )
        status.lastError?.let {
            Text(stringResource(R.string.more_error, it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        Button(onClick = { SyncScheduler.syncNow(context) }) { Text(stringResource(R.string.sync_now)) }
        OutlinedButton(onClick = onOpenDiagnostics) { Text(stringResource(R.string.more_diagnostics)) }
    }
}
