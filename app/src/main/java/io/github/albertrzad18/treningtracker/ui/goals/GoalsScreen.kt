package io.github.albertrzad18.treningtracker.ui.goals

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
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
import io.github.albertrzad18.treningtracker.ui.common.DisciplineDot
import io.github.albertrzad18.treningtracker.ui.common.daysText
import io.github.albertrzad18.treningtracker.ui.common.labelRes
import io.github.albertrzad18.treningtracker.ui.common.periodsText
import io.github.albertrzad18.treningtracker.ui.format.formatCalories
import io.github.albertrzad18.treningtracker.ui.format.formatDistanceKm
import io.github.albertrzad18.treningtracker.ui.format.formatHoursAndMinutes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
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
    var showDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.goals_title), style = MaterialTheme.typography.titleLarge)
        }
        val list = rows
        when {
            list == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            else -> LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (list.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.goals_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(list, key = { it.progress.goal.id }) { row ->
                    GoalCard(row, onDelete = { vm.delete(row.progress.goal) })
                }
            }
        }
        Button(onClick = { showDialog = true }, modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.goals_add))
        }
    }

    if (showDialog) {
        NewGoalDialog(
            onDismiss = { showDialog = false },
            onSave = { discipline, period, metric, target ->
                vm.add(discipline, period, metric, target)
                showDialog = false
            },
        )
    }
}

@Composable
fun GoalCard(row: GoalRow, onDelete: (() -> Unit)? = null) {
    val progress = row.progress
    val goal = progress.goal
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                goal.discipline?.let {
                    DisciplineDot(it, size = 10.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(goal.title(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                onDelete?.let {
                    TextButton(onClick = it) { Text(stringResource(R.string.goals_delete)) }
                }
            }
            Text(
                stringResource(
                    R.string.goals_progress,
                    goal.metric.format(progress.current),
                    goal.metric.format(goal.target),
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            LinearProgressIndicator(
                progress = { progress.fraction.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            if (progress.done) {
                Text(stringResource(R.string.goals_done), style = MaterialTheme.typography.bodyMedium)
            } else {
                progress.projected?.let {
                    Text(
                        stringResource(R.string.goals_projection, goal.metric.format(it)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    stringResource(R.string.goals_days_left, daysText(progress.daysLeft)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (row.history.isNotEmpty()) {
                Text(
                    stringResource(R.string.goals_history, row.history.joinToString(" ") { if (it.second) "✔" else "✘" }),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (row.streak > 0) {
                Text(
                    stringResource(R.string.goals_streak, periodsText(row.streak)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** Nazwa celu, np. „Rower: dystans, miesięcznie”. */
@Composable
private fun Goal.title(): String {
    val who = discipline?.let { stringResource(it.labelRes()) } ?: stringResource(R.string.goals_all_disciplines)
    return "$who · ${stringResource(metric.labelRes())} · ${stringResource(period.labelRes())}"
}

private fun GoalMetric.labelRes() = when (this) {
    GoalMetric.DISTANCE -> R.string.goals_metric_distance
    GoalMetric.DURATION -> R.string.goals_metric_duration
    GoalMetric.CALORIES -> R.string.goals_metric_calories
    GoalMetric.COUNT -> R.string.goals_metric_count
}

private fun GoalPeriod.labelRes() = when (this) {
    GoalPeriod.WEEK -> R.string.goals_period_week
    GoalPeriod.MONTH -> R.string.goals_period_month
    GoalPeriod.YEAR -> R.string.goals_period_year
}

/** Wartość celu w jednostkach, w jakich ją rozumiemy: metry, sekundy, kcal, sztuki. */
private fun GoalMetric.format(value: Double): String = when (this) {
    GoalMetric.DISTANCE -> formatDistanceKm(value)
    GoalMetric.DURATION -> formatHoursAndMinutes(Duration.ofSeconds(value.toLong()))
    GoalMetric.CALORIES -> formatCalories(value)
    GoalMetric.COUNT -> value.toInt().toString()
}

/** Przelicza to, co wpisał użytkownik (km, godziny), na jednostki bazy (metry, sekundy). */
private fun GoalMetric.toStored(input: Double): Double = when (this) {
    GoalMetric.DISTANCE -> input * 1000
    GoalMetric.DURATION -> input * 3600
    else -> input
}

@Composable
private fun NewGoalDialog(
    onDismiss: () -> Unit,
    onSave: (Discipline?, GoalPeriod, GoalMetric, Double) -> Unit,
) {
    var discipline by remember { mutableStateOf<Discipline?>(null) }
    var period by remember { mutableStateOf(GoalPeriod.MONTH) }
    var metric by remember { mutableStateOf(GoalMetric.DISTANCE) }
    var target by remember { mutableStateOf("") }
    val value = target.replace(',', '.').toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.goals_new)) },
        confirmButton = {
            TextButton(
                enabled = value != null && value > 0,
                onClick = { onSave(discipline, period, metric, metric.toStored(value!!)) },
            ) { Text(stringResource(R.string.goals_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.goals_cancel)) } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipRow(
                    options = listOf<Discipline?>(null) + Discipline.entries,
                    selected = discipline,
                    label = { d -> d?.let { stringResource(it.labelRes()) } ?: stringResource(R.string.goals_all_disciplines) },
                    onSelect = { discipline = it },
                )
                ChipRow(
                    options = GoalPeriod.entries,
                    selected = period,
                    label = { stringResource(it.labelRes()) },
                    onSelect = { period = it },
                )
                ChipRow(
                    options = GoalMetric.entries,
                    selected = metric,
                    label = { stringResource(it.labelRes()) },
                    onSelect = { metric = it },
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text(stringResource(R.string.goals_target)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
        },
    )
}

@Composable
private fun <T> ChipRow(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    // FlowRow zawija chipy do kolejnej linii, więc w wąskim oknie dialogu nic nie ucieka poza ekran.
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}
