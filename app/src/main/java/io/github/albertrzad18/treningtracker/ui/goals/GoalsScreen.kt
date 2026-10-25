package io.github.albertrzad18.treningtracker.ui.goals

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.TreningApp
import io.github.albertrzad18.treningtracker.data.db.GoalEntity
import io.github.albertrzad18.treningtracker.data.db.toDomain
import io.github.albertrzad18.treningtracker.domain.DateRange
import io.github.albertrzad18.treningtracker.domain.Discipline
import io.github.albertrzad18.treningtracker.domain.Goal
import io.github.albertrzad18.treningtracker.domain.GoalMetric
import io.github.albertrzad18.treningtracker.domain.GoalPeriod
import io.github.albertrzad18.treningtracker.domain.GoalProgress
import io.github.albertrzad18.treningtracker.domain.goalHistory
import io.github.albertrzad18.treningtracker.domain.goalProgress
import io.github.albertrzad18.treningtracker.domain.goalStreak
import io.github.albertrzad18.treningtracker.ui.common.LabelText
import io.github.albertrzad18.treningtracker.ui.common.Panel
import io.github.albertrzad18.treningtracker.ui.common.PrimaryButton
import io.github.albertrzad18.treningtracker.ui.common.SecondaryButton
import io.github.albertrzad18.treningtracker.ui.common.SkeletonBlock
import io.github.albertrzad18.treningtracker.ui.common.TtChip
import io.github.albertrzad18.treningtracker.ui.common.TtIcons
import io.github.albertrzad18.treningtracker.ui.common.TtTopBar
import io.github.albertrzad18.treningtracker.ui.common.daysText
import io.github.albertrzad18.treningtracker.ui.common.labelRes
import io.github.albertrzad18.treningtracker.ui.common.periodsText
import io.github.albertrzad18.treningtracker.ui.common.ttClickable
import io.github.albertrzad18.treningtracker.ui.theme.LocalTtColors
import io.github.albertrzad18.treningtracker.ui.theme.TtShape
import io.github.albertrzad18.treningtracker.ui.theme.TtSpace
import io.github.albertrzad18.treningtracker.ui.theme.TtType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** Cel razem z postępem i historią ostatnich okresów. */
data class GoalRow(
    val progress: GoalProgress,
    val history: List<Pair<DateRange, Boolean>>,
    val streak: Int,
)

class GoalsViewModel(app: Application) : AndroidViewModel(app) {
    private val treningApp = app as TreningApp
    private val goalDao = treningApp.database.goalDao()
    private val workoutDao = treningApp.database.workoutDao()

    /** `null` = jeszcze się wczytuje. */
    val rows: StateFlow<List<GoalRow>?> = combine(goalDao.observeAll(), workoutDao.observeAll()) { goals, entities ->
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val workouts = entities.map { it.toDomain() }
        goals.map { it.toDomain() }.map { goal ->
            val history = goalHistory(goal, workouts, today, zone)
            GoalRow(goalProgress(goal, workouts, today, zone), history, goalStreak(history))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun add(discipline: Discipline?, period: GoalPeriod, metric: GoalMetric, target: Double) {
        viewModelScope.launch {
            goalDao.insert(
                GoalEntity(
                    discipline = discipline,
                    period = period,
                    metric = metric,
                    target = target,
                    createdAtMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun delete(goal: Goal) {
        viewModelScope.launch { goalDao.deleteById(goal.id) }
    }
}

@Composable
fun GoalsScreen(onBack: () -> Unit, vm: GoalsViewModel = viewModel()) {
    val rows by vm.rows.collectAsStateWithLifecycle()
    GoalsContent(
        rows = rows,
        onBack = onBack,
        onAdd = vm::add,
        onDelete = vm::delete,
    )
}

/** Sam widok celów: bez ViewModelu, więc da się go pokazać w `@Preview`. */
@Composable
internal fun GoalsContent(
    rows: List<GoalRow>?,
    onBack: () -> Unit,
    onAdd: (Discipline?, GoalPeriod, GoalMetric, Double) -> Unit,
    onDelete: (Goal) -> Unit,
) {
    var addOpen by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<GoalRow?>(null) }
    val colors = LocalTtColors.current

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TtTopBar(stringResource(R.string.goals_title), onBack)
            val list = rows
            when {
                list == null -> Column(
                    Modifier.padding(horizontal = TtSpace.screenMargin),
                    verticalArrangement = Arrangement.spacedBy(TtSpace.panelGap),
                ) {
                    repeat(3) { SkeletonBlock(height = 150.dp, shape = TtShape.panel) }
                }
                list.isEmpty() -> Text(
                    stringResource(R.string.goals_empty),
                    style = TtType.body,
                    color = colors.ink2,
                    modifier = Modifier.padding(horizontal = TtSpace.screenMargin).padding(top = 56.dp),
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(
                        start = TtSpace.screenMargin,
                        end = TtSpace.screenMargin,
                        top = TtSpace.x1,
                        bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(TtSpace.panelGap),
                ) {
                    items(list, key = { it.progress.goal.id }) { row ->
                        Panel(modifier = Modifier.clip(TtShape.panel).ttClickable { detail = row }) {
                            GoalProgressBlock(row)
                        }
                    }
                }
            }
        }
        // ExtendedFAB „Dodaj cel”: zawsze w prawym dolnym rogu, nad listą.
        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(TtSpace.x4),
        ) {
            Row(
                Modifier
                    .height(56.dp)
                    .clip(TtShape.full)
                    .background(colors.accent)
                    .ttClickable { addOpen = true }
                    .padding(start = 18.dp, end = TtSpace.x6),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(TtIcons.Add, contentDescription = null, tint = colors.onAccent)
                Text(
                    stringResource(R.string.goals_add),
                    style = TtType.title,
                    color = colors.onAccent,
                )
            }
        }
    }

    if (addOpen) {
        AddGoalSheet(
            onDismiss = { addOpen = false },
            onSave = { discipline, period, metric, target ->
                onAdd(discipline, period, metric, target)
                addOpen = false
            },
        )
    }
    detail?.let { row ->
        GoalDetailSheet(
            row = row,
            onDismiss = { detail = null },
            onDelete = {
                onDelete(row.progress.goal)
                detail = null
            },
        )
    }
}

/** Arkusz z uchwytem i górnymi rogami 28 dp — wspólny dla dodawania celu i jego szczegółów. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun TtSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
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
                .padding(start = TtSpace.screenMargin, end = TtSpace.screenMargin, top = TtSpace.x4)
                .padding(bottom = TtSpace.x8)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(TtSpace.x4),
        ) {
            content()
        }
    }
}


/** Szczegóły celu: to samo co na liście plus „Usuń cel”. */
@Composable
private fun GoalDetailSheet(row: GoalRow, onDismiss: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalTtColors.current
    TtSheet(onDismiss) {
        GoalProgressBlock(row)
        Text(
            buildString {
                append(stringResource(R.string.goals_days_left, daysText(row.progress.daysLeft)))
                if (row.streak > 0) append(" · " + stringResource(R.string.goals_streak, periodsText(row.streak)))
            },
            style = TtType.bodyS,
            color = colors.ink2,
        )
        SecondaryButton(
            text = stringResource(R.string.goals_delete),
            contentColor = colors.error,
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddGoalSheet(
    onDismiss: () -> Unit,
    onSave: (Discipline?, GoalPeriod, GoalMetric, Double) -> Unit,
) {
    val colors = LocalTtColors.current
    var discipline by remember { mutableStateOf<Discipline?>(null) }
    var period by remember { mutableStateOf(GoalPeriod.MONTH) }
    var metric by remember { mutableStateOf(GoalMetric.DISTANCE) }
    var target by remember { mutableStateOf("") }
    val value = target.replace(',', '.').toDoubleOrNull()

    TtSheet(onDismiss) {
        Text(stringResource(R.string.goals_new), style = TtType.title, color = colors.ink)

        ChipGroup(stringResource(R.string.goals_group_discipline)) {
            val options = listOf<Discipline?>(null) + Discipline.entries
            options.forEach { option ->
                val selected = option == discipline
                TtChip(
                    label = option?.let { stringResource(it.labelRes()) }
                        ?: stringResource(R.string.goals_all_disciplines),
                    onClick = { discipline = option },
                    selected = selected,
                    background = if (selected) colors.accent else Color.Transparent,
                    border = if (selected) colors.accent else colors.line,
                    textColor = if (selected) colors.onAccent else colors.ink,
                )
            }
        }
        ChipGroup(stringResource(R.string.goals_group_period)) {
            GoalPeriod.entries.forEach { option ->
                val selected = option == period
                TtChip(
                    label = stringResource(option.labelRes()),
                    onClick = { period = option },
                    selected = selected,
                    background = if (selected) colors.accent else Color.Transparent,
                    border = if (selected) colors.accent else colors.line,
                    textColor = if (selected) colors.onAccent else colors.ink,
                )
            }
        }
        ChipGroup(stringResource(R.string.goals_group_metric)) {
            GoalMetric.entries.forEach { option ->
                val selected = option == metric
                TtChip(
                    label = stringResource(option.labelRes()),
                    onClick = { metric = option },
                    selected = selected,
                    background = if (selected) colors.accent else Color.Transparent,
                    border = if (selected) colors.accent else colors.line,
                    textColor = if (selected) colors.onAccent else colors.ink,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(TtSpace.x2)) {
            LabelText(stringResource(R.string.goals_target))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .border(1.dp, colors.line, TtShape.field)
                    .padding(horizontal = TtSpace.x4),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BasicTextField(
                    value = target,
                    onValueChange = { text -> target = text.filter { it.isDigit() || it == ',' || it == '.' }.take(7) },
                    modifier = Modifier.weight(1f),
                    textStyle = TtType.value.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.ink),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                Text(metric.unitLabel(), style = TtType.bodyS, color = colors.ink2)
            }
        }

        PrimaryButton(
            text = stringResource(R.string.goals_save),
            onClick = { if (value != null && value > 0) onSave(discipline, period, metric, metric.toStored(value)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(TtSpace.x2)) {
        LabelText(title)
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TtSpace.x2),
            verticalArrangement = Arrangement.spacedBy(TtSpace.x2),
        ) {
            content()
        }
    }
}
