package io.github.albertrzad18.treningtracker.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
                .safeDrawingPadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.debug_title), style = MaterialTheme.typography.headlineSmall)
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
