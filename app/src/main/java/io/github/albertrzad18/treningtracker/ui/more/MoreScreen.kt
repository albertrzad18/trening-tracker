package io.github.albertrzad18.treningtracker.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.TreningApp
import io.github.albertrzad18.treningtracker.data.sync.SyncScheduler
import io.github.albertrzad18.treningtracker.ui.common.LabelText
import io.github.albertrzad18.treningtracker.ui.common.ListRow
import io.github.albertrzad18.treningtracker.ui.common.Panel
import io.github.albertrzad18.treningtracker.ui.common.SecondaryButton
import io.github.albertrzad18.treningtracker.ui.common.TtIcons
import io.github.albertrzad18.treningtracker.ui.common.TtTopBar
import io.github.albertrzad18.treningtracker.ui.dashboard.DashboardViewModel
import io.github.albertrzad18.treningtracker.ui.format.formatDateTime
import io.github.albertrzad18.treningtracker.ui.format.formatHoursAndMinutes
import io.github.albertrzad18.treningtracker.ui.format.pluralPl
import io.github.albertrzad18.treningtracker.ui.goals.GoalsViewModel
import io.github.albertrzad18.treningtracker.ui.records.RecordsViewModel
import io.github.albertrzad18.treningtracker.ui.theme.LocalTtColors
import io.github.albertrzad18.treningtracker.ui.theme.TtSpace
import io.github.albertrzad18.treningtracker.ui.theme.TtType
import java.time.Instant
import java.time.ZoneId

/**
 * „Więcej”: stan synchronizacji i wejścia do ekranów podrzędnych.
 * Każdy wiersz ma podtytuł z prawdziwą informacją, więc widać stan apki bez wchodzenia głębiej.
 */
@Composable
fun MoreScreen(
    onBack: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenSummaries: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenSettings: () -> Unit,
    dashboardVm: DashboardViewModel = viewModel(),
    recordsVm: RecordsViewModel = viewModel(),
    goalsVm: GoalsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val app = context.applicationContext as TreningApp
    val status by app.syncPrefs.status.collectAsStateWithLifecycle()
    val settings by app.settings.state.collectAsStateWithLifecycle()
    val dashboard by dashboardVm.state.collectAsStateWithLifecycle()
    val records by recordsVm.data.collectAsStateWithLifecycle()
    val goals by goalsVm.rows.collectAsStateWithLifecycle()
    val colors = LocalTtColors.current

    Column(Modifier.fillMaxSize()) {
    TtTopBar(stringResource(R.string.nav_more), onBack)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TtSpace.screenMargin)
            .padding(top = TtSpace.x2, bottom = TtSpace.x6),
        verticalArrangement = Arrangement.spacedBy(TtSpace.panelGap),
    ) {
        Panel {
            Row(
                Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TtSpace.x3),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.more_sync_label), style = TtType.bodyS, color = colors.ink2)
                    val last = status.lastSyncMillis?.let {
                        formatDateTime(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime())
                    }
                    Text(
                        last ?: stringResource(R.string.more_never),
                        style = TtType.body.copy(fontWeight = FontWeight.Medium),
                        color = colors.ink,
                    )
                    status.lastError?.let {
                        Text(
                            stringResource(R.string.more_error, it),
                            style = TtType.bodyS,
                            color = colors.error,
                        )
                    }
                }
                SecondaryButton(
                    text = stringResource(R.string.more_sync_button),
                    onClick = { SyncScheduler.syncNow(context) },
                    small = true,
                )
            }
        }

        Panel {
            LabelText(stringResource(R.string.more_group_overview), modifier = Modifier.padding(bottom = TtSpace.x2))
            ListRow(
                title = stringResource(R.string.summaries_title),
                subtitle = dashboard?.let {
                    stringResource(R.string.more_summaries_sub, formatHoursAndMinutes(it.week.duration))
                },
                icon = TtIcons.Summaries,
                onClick = onOpenSummaries,
            )
            ListRow(
                title = stringResource(R.string.more_records),
                subtitle = records?.let { data ->
                    pluralPl(
                        data.count,
                        stringResource(R.string.unit_record_one),
                        stringResource(R.string.unit_record_few),
                        stringResource(R.string.unit_record_many),
                    )
                },
                icon = TtIcons.Trophy,
                onClick = onOpenRecords,
            )
            ListRow(
                title = stringResource(R.string.goals_title),
                subtitle = goals?.let { list ->
                    pluralPl(
                        list.size,
                        stringResource(R.string.unit_goal_one),
                        stringResource(R.string.unit_goal_few),
                        stringResource(R.string.unit_goal_many),
                    )
                },
                icon = TtIcons.Goals,
                onClick = onOpenGoals,
            )
        }

        Panel {
            LabelText(stringResource(R.string.more_group_app), modifier = Modifier.padding(bottom = TtSpace.x2))
            ListRow(
                title = stringResource(R.string.settings_title),
                subtitle = stringResource(
                    R.string.more_settings_sub,
                    settings.maxHeartRate,
                    settings.poolLengthM,
                ),
                icon = TtIcons.Settings,
                onClick = onOpenSettings,
            )
            ListRow(
                title = stringResource(R.string.more_diagnostics_title),
                subtitle = stringResource(R.string.more_diagnostics_sub),
                icon = TtIcons.Diagnostics,
                onClick = onOpenDiagnostics,
            )
        }
        Spacer(Modifier.height(TtSpace.x1))
    }
    }
}
