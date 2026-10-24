package io.github.albertrzad18.treningtracker.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.albertrzad18.treningtracker.BuildConfig
import io.github.albertrzad18.treningtracker.R

@Composable
fun DebugScreen(viewModel: DebugViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { viewModel.refresh() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.debug_title), style = MaterialTheme.typography.headlineSmall)
            if (BuildConfig.DEBUG) DemoDataSection(viewModel)
            when (val s = state) {
                DebugState.Loading -> CircularProgressIndicator()
                DebugState.Unavailable -> Text(stringResource(R.string.debug_unavailable))
                is DebugState.NeedsPermissions -> {
                    Text(stringResource(R.string.debug_needs_permissions))
                    Button(onClick = { permissionLauncher.launch(s.toRequest) }) {
                        Text(stringResource(R.string.debug_grant))
                    }
                }
                is DebugState.Error -> {
                    Text(stringResource(R.string.debug_error, s.message))
                    Button(onClick = viewModel::refresh) { Text(stringResource(R.string.debug_retry)) }
                }
                is DebugState.Report -> {
                    val clipboard = LocalClipboardManager.current
                    Button(onClick = { clipboard.setText(AnnotatedString(s.text)) }) {
                        Text(stringResource(R.string.debug_copy))
                    }
                    SelectionContainer {
                        Text(s.text, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

/**
 * Dane demo: tylko w buildzie debug. Pozwalają sprawdzić wykresy i tabelę bez zegarka,
 * a potem usunąć sztuczne treningi jednym kliknięciem.
 */
@Composable
private fun DemoDataSection(viewModel: DebugViewModel) {
    val message by viewModel.demoMessage.collectAsStateWithLifecycle()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.debug_demo_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.debug_demo_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::fillDemoData) { Text(stringResource(R.string.debug_demo_fill)) }
            OutlinedButton(onClick = viewModel::clearDemoData) { Text(stringResource(R.string.debug_demo_clear)) }
        }
        when (val m = message) {
            is DemoMessage.Added -> Text(stringResource(R.string.debug_demo_added, m.count), style = MaterialTheme.typography.bodySmall)
            DemoMessage.Removed -> Text(stringResource(R.string.debug_demo_removed), style = MaterialTheme.typography.bodySmall)
            null -> Unit
        }
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
    }
}
