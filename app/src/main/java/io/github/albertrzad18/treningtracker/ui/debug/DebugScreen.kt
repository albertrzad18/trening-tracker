package io.github.albertrzad18.treningtracker.ui.debug

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.albertrzad18.treningtracker.BuildConfig
import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.ui.common.Panel
import io.github.albertrzad18.treningtracker.ui.common.PrimaryButton
import io.github.albertrzad18.treningtracker.ui.common.SecondaryButton
import io.github.albertrzad18.treningtracker.ui.common.SkeletonBlock
import io.github.albertrzad18.treningtracker.ui.common.TtDivider
import io.github.albertrzad18.treningtracker.ui.common.TtIcons
import io.github.albertrzad18.treningtracker.ui.common.TtTopBar
import io.github.albertrzad18.treningtracker.ui.format.formatDate
import io.github.albertrzad18.treningtracker.ui.format.formatNumberShort
import io.github.albertrzad18.treningtracker.ui.theme.LocalTtColors
import io.github.albertrzad18.treningtracker.ui.theme.TtShape
import io.github.albertrzad18.treningtracker.ui.theme.TtSpace
import io.github.albertrzad18.treningtracker.ui.theme.TtType
import kotlin.math.log10

@Composable
fun DebugScreen(onBack: () -> Unit, viewModel: DebugViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalTtColors.current
    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { viewModel.refresh() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = TtSpace.x6),
    ) {
        TtTopBar(stringResource(R.string.debug_title), onBack)
        Column(
            Modifier.padding(horizontal = TtSpace.screenMargin),
            verticalArrangement = Arrangement.spacedBy(TtSpace.x3),
        ) {
            Text(stringResource(R.string.debug_intro), style = TtType.bodyS, color = colors.ink2)
            when (val s = state) {
                DebugState.Loading -> Column(verticalArrangement = Arrangement.spacedBy(TtSpace.x3)) {
                    repeat(5) { SkeletonBlock(height = 44.dp) }
                }
                DebugState.Unavailable -> Text(
                    stringResource(R.string.debug_unavailable),
                    style = TtType.body,
                    color = colors.ink2,
                )
                is DebugState.NeedsPermissions -> Column(verticalArrangement = Arrangement.spacedBy(TtSpace.x4)) {
                    Text(stringResource(R.string.debug_needs_permissions), style = TtType.body, color = colors.ink2)
                    PrimaryButton(
                        text = stringResource(R.string.debug_grant),
                        onClick = { permissionLauncher.launch(s.toRequest) },
                    )
                }
                is DebugState.Error -> Column(verticalArrangement = Arrangement.spacedBy(TtSpace.x4)) {
                    Text(
                        stringResource(R.string.debug_error, s.message),
                        style = TtType.body,
                        color = colors.error,
                    )
                    SecondaryButton(text = stringResource(R.string.debug_retry), onClick = viewModel::refresh)
                }
                is DebugState.Report -> ReportTable(s)
            }
        }
        if (BuildConfig.DEBUG) {
            DemoDataPanel(viewModel, Modifier.padding(horizontal = TtSpace.screenMargin, vertical = TtSpace.panelGap))
        }
    }
}

/** Tabela: nazwa typu danych, liczba rekordów i cienki pasek w skali logarytmicznej. */
@Composable
private fun ReportTable(report: DebugState.Report) {
    val colors = LocalTtColors.current
    val clipboard = LocalClipboardManager.current
    val max = report.rows.maxOfOrNull { it.count } ?: 0
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TtSpace.x3)) {
        Column(Modifier.fillMaxWidth()) {
            report.rows.forEach { row ->
                TtDivider()
                Column(
                    Modifier.fillMaxWidth().padding(vertical = TtSpace.x3),
                    verticalArrangement = Arrangement.spacedBy(TtSpace.x2),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        Text(row.label, style = TtType.mono, color = colors.ink, modifier = Modifier.weight(1f))
                        Text(
                            formatNumberShort(row.count.toDouble()),
                            style = TtType.body.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.ink,
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(TtShape.full).background(colors.track)) {
                        val fraction = logFraction(row.count, max)
                        if (fraction > 0f) {
                            Box(
                                Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .clip(TtShape.full)
                                    .background(colors.ink),
                            )
                        }
                    }
                }
            }
            if (report.from != null && report.to != null) {
                TtDivider()
                Text(
                    stringResource(R.string.debug_range, formatDate(report.from), formatDate(report.to)),
                    style = TtType.bodyS,
                    color = colors.ink2,
                    modifier = Modifier.padding(top = TtSpace.x3),
                )
            }
        }
        SecondaryButton(
            text = stringResource(R.string.debug_copy),
            onClick = { clipboard.setText(AnnotatedString(report.text)) },
            icon = TtIcons.Copy,
        )
    }
}

/** Skala logarytmiczna: inaczej jeden typ z tysiącami rekordów spłaszcza wszystkie pozostałe. */
private fun logFraction(count: Int, max: Int): Float {
    if (count <= 0 || max <= 0) return 0f
    return (log10(1.0 + count) / log10(1.0 + max)).toFloat()
}

/**
 * Dane demo: tylko w buildzie debug. Pozwalają sprawdzić wykresy i listy bez zegarka,
 * a potem usunąć sztuczne treningi jednym kliknięciem.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DemoDataPanel(viewModel: DebugViewModel, modifier: Modifier = Modifier) {
    val colors = LocalTtColors.current
    val message by viewModel.demoMessage.collectAsStateWithLifecycle()
    Panel(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(TtSpace.x3)) {
            Text(stringResource(R.string.debug_debug_only), style = TtType.title, color = colors.ink)
            Text(stringResource(R.string.debug_demo_desc), style = TtType.bodyS, color = colors.ink2)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(TtSpace.x2),
                verticalArrangement = Arrangement.spacedBy(TtSpace.x2),
            ) {
                SecondaryButton(text = stringResource(R.string.debug_demo_fill), onClick = viewModel::fillDemoData)
                SecondaryButton(text = stringResource(R.string.debug_demo_clear), onClick = viewModel::clearDemoData)
                SecondaryButton(text = stringResource(R.string.debug_demo_today), onClick = viewModel::addTodayWorkout)
            }
            when (val m = message) {
                is DemoMessage.Added -> Text(
                    stringResource(R.string.debug_demo_added, m.count),
                    style = TtType.bodyS,
                    color = colors.ink2,
                )
                DemoMessage.Removed -> Text(
                    stringResource(R.string.debug_demo_removed),
                    style = TtType.bodyS,
                    color = colors.ink2,
                )
                null -> Unit
            }
        }
    }
}
