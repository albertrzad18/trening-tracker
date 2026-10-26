package io.github.albertrzad18.treningtracker.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.TreningApp
import io.github.albertrzad18.treningtracker.data.settings.HomeCard
import io.github.albertrzad18.treningtracker.data.settings.ThemeMode
import io.github.albertrzad18.treningtracker.ui.common.ListRow
import io.github.albertrzad18.treningtracker.ui.common.Panel
import io.github.albertrzad18.treningtracker.ui.common.PrimaryButton
import io.github.albertrzad18.treningtracker.ui.common.SegmentedToggle
import io.github.albertrzad18.treningtracker.ui.common.TtDivider
import io.github.albertrzad18.treningtracker.ui.common.TtIcons
import io.github.albertrzad18.treningtracker.ui.common.TtTopBar
import io.github.albertrzad18.treningtracker.ui.common.ttClickable
import io.github.albertrzad18.treningtracker.ui.theme.LocalTtColors
import io.github.albertrzad18.treningtracker.ui.theme.TtMotion
import io.github.albertrzad18.treningtracker.ui.theme.TtShape
import io.github.albertrzad18.treningtracker.ui.theme.TtSpace
import io.github.albertrzad18.treningtracker.ui.theme.TtType
import io.github.albertrzad18.treningtracker.ui.theme.ttDuration

/** Które ustawienie edytujemy w arkuszu. */
private enum class SettingSheet { MAX_HR, POOL, THEME }

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = (context.applicationContext as TreningApp).settings
    val state by settings.state.collectAsStateWithLifecycle()
    val colors = LocalTtColors.current
    var sheet by remember { mutableStateOf<SettingSheet?>(null) }
    var stepsOpen by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TtTopBar(stringResource(R.string.settings_title), onBack)
        Panel(
            modifier = Modifier
                .padding(horizontal = TtSpace.screenMargin)
                .padding(bottom = TtSpace.x6),
        ) {
            ListRow(
                title = stringResource(R.string.settings_max_hr),
                value = stringResource(R.string.settings_max_hr_value, state.maxHeartRate),
                onClick = { sheet = SettingSheet.MAX_HR },
                divider = false,
            )
            ListRow(
                title = stringResource(R.string.settings_pool),
                value = stringResource(R.string.settings_pool_unit, state.poolLengthM),
                onClick = { sheet = SettingSheet.POOL },
            )
            ListRow(
                title = stringResource(R.string.settings_theme),
                value = stringResource(state.themeMode.labelRes()),
                onClick = { sheet = SettingSheet.THEME },
            )
            TtDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 64.dp)
                    .ttClickable { stepsOpen = !stepsOpen }
                    .padding(vertical = TtSpace.x3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.settings_howto),
                    style = TtType.body.copy(fontWeight = FontWeight.Medium),
                    color = colors.ink,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    if (stepsOpen) TtIcons.ExpandLess else TtIcons.ExpandMore,
                    contentDescription = null,
                    tint = colors.ink2,
                )
            }
            val duration = ttDuration(TtMotion.BASE)
            AnimatedVisibility(
                visible = stepsOpen,
                enter = fadeIn(tween(duration, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(duration, easing = FastOutSlowInEasing)),
            ) {
                Column(Modifier.padding(bottom = TtSpace.x3)) {
                    healthSyncSteps().forEachIndexed { index, step ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = TtSpace.x2),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(
                                Modifier.size(24.dp).border(1.dp, colors.line, TtShape.full),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${index + 1}", style = TtType.caption, color = colors.ink2)
                            }
                            Text(step, style = TtType.bodyS, color = colors.ink)
                        }
                    }
                }
            }
        }
        CardsPanel(state.hiddenCards, settings::setCardVisible)
        BackgroundSyncPanel()
    }

    when (sheet) {
        SettingSheet.MAX_HR -> SettingSheetBox(
            title = stringResource(R.string.settings_max_hr),
            subtitle = stringResource(R.string.settings_max_hr_desc),
            onDismiss = { sheet = null },
        ) {
            var input by remember { mutableStateOf(state.maxHeartRate.toString()) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(1.dp, colors.line, TtShape.field)
                    .padding(horizontal = TtSpace.x4),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BasicTextField(
                    value = input,
                    onValueChange = { text ->
                        input = text.filter { it.isDigit() }.take(3)
                        input.toIntOrNull()?.let(settings::setMaxHeartRate)
                    },
                    modifier = Modifier.weight(1f),
                    textStyle = TtType.value.copy(fontSize = 22.sp, fontWeight = FontWeight.Medium, color = colors.ink),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    singleLine = true,
                )
                Text("bpm", style = TtType.bodyS, color = colors.ink2)
            }
        }
        SettingSheet.POOL -> SettingSheetBox(
            title = stringResource(R.string.settings_pool),
            subtitle = null,
            onDismiss = { sheet = null },
        ) {
            SegmentedToggle(
                options = listOf(25, 50),
                selected = state.poolLengthM,
                label = { stringResource(R.string.settings_pool_unit, it) },
                onSelect = settings::setPoolLength,
            )
        }
        SettingSheet.THEME -> SettingSheetBox(
            title = stringResource(R.string.settings_theme),
            subtitle = null,
            onDismiss = { sheet = null },
        ) {
            SegmentedToggle(
                options = ThemeMode.entries,
                selected = state.themeMode,
                label = { stringResource(it.labelRes()) },
                onSelect = settings::setThemeMode,
            )
        }
        null -> Unit
    }
}

/** Arkusz edycji jednego ustawienia: tytuł, wyjaśnienie, kontrolka i „Gotowe”. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SettingSheetBox(
    title: String,
    subtitle: String?,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = LocalTtColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = TtShape.sheet,
        containerColor = colors.surface,
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = TtSpace.x3), contentAlignment = Alignment.Center) {
                Box(Modifier.width(36.dp).height(4.dp).background(colors.line, RoundedCornerShape(2.dp)))
            }
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = TtSpace.screenMargin)
                .padding(top = TtSpace.x4, bottom = TtSpace.x8)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, style = TtType.title, color = colors.ink)
            if (subtitle != null) Text(subtitle, style = TtType.bodyS, color = colors.ink2)
            content()
            PrimaryButton(
                text = stringResource(R.string.settings_done),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun healthSyncSteps(): List<String> = listOf(
    stringResource(R.string.settings_step_1),
    stringResource(R.string.settings_step_2),
    stringResource(R.string.settings_step_3),
    stringResource(R.string.settings_step_4),
    stringResource(R.string.settings_step_5),
)

private fun ThemeMode.labelRes() = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}

/** Przełączniki widoczności kart na Dashboardzie, w Miesiącu i w Treningach. */
@Composable
private fun CardsPanel(hidden: Set<HomeCard>, onToggle: (HomeCard, Boolean) -> Unit) {
    val colors = LocalTtColors.current
    Panel(modifier = Modifier.padding(horizontal = TtSpace.screenMargin).padding(bottom = TtSpace.x6)) {
        Text(
            stringResource(R.string.settings_cards_title),
            style = TtType.title,
            color = colors.ink,
            modifier = Modifier.padding(bottom = TtSpace.x2),
        )
        HomeCard.entries.forEachIndexed { index, card ->
            if (index > 0) TtDivider()
            Row(
                Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(card.labelRes()),
                    style = TtType.body,
                    color = colors.ink,
                    modifier = Modifier.weight(1f),
                )
                androidx.compose.material3.Switch(
                    checked = card !in hidden,
                    onCheckedChange = { onToggle(card, it) },
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedThumbColor = colors.onAccent,
                        checkedTrackColor = colors.accent,
                        uncheckedThumbColor = colors.ink2,
                        uncheckedTrackColor = colors.bg,
                        uncheckedBorderColor = colors.line,
                    ),
                )
            }
        }
    }
}

/**
 * Pomoc w synchronizacji bez zaglądania do Huawei Health: skróty do ustawień obu aplikacji pośredniczących
 * (bateria, autostart) i do samego Health Sync. Apka nie może sama uruchomić pobierania z zegarka.
 */
@Composable
private fun BackgroundSyncPanel() {
    val context = LocalContext.current
    val colors = LocalTtColors.current
    Panel(modifier = Modifier.padding(horizontal = TtSpace.screenMargin).padding(bottom = TtSpace.x6)) {
        Text(
            stringResource(R.string.settings_bg_title),
            style = TtType.title,
            color = colors.ink,
        )
        Text(
            stringResource(R.string.settings_bg_hint),
            style = TtType.bodyS,
            color = colors.ink2,
            modifier = Modifier.padding(top = TtSpace.x2, bottom = TtSpace.x2),
        )
        ListRow(
            title = stringResource(R.string.settings_open_health_sync),
            onClick = { context.packageManager.getLaunchIntentForPackage("nl.appyhapps.healthsync")?.let(context::startActivity) },
        )
        ListRow(
            title = stringResource(R.string.settings_hs_app_settings),
            onClick = { openAppSettings(context, "nl.appyhapps.healthsync") },
        )
        ListRow(
            title = stringResource(R.string.settings_hh_app_settings),
            onClick = { openAppSettings(context, "com.huawei.health") },
        )
    }
}

private fun openAppSettings(context: android.content.Context, packageName: String) {
    val intent = android.content.Intent(
        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        android.net.Uri.parse("package:$packageName"),
    ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

private fun HomeCard.labelRes() = when (this) {
    HomeCard.POOL -> R.string.settings_card_pool
    HomeCard.ROAD -> R.string.settings_card_road
    HomeCard.TRAINER -> R.string.settings_card_trainer
    HomeCard.WEEK -> R.string.settings_card_week
    HomeCard.LATEST -> R.string.settings_card_latest
    HomeCard.GOALS -> R.string.settings_card_goals
}
