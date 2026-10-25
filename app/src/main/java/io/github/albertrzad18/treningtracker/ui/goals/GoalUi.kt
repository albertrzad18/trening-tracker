package io.github.albertrzad18.treningtracker.ui.goals

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.domain.DateRange
import io.github.albertrzad18.treningtracker.domain.Goal
import io.github.albertrzad18.treningtracker.domain.GoalMetric
import io.github.albertrzad18.treningtracker.domain.GoalPeriod
import io.github.albertrzad18.treningtracker.ui.common.DisciplineDot
import io.github.albertrzad18.treningtracker.ui.common.LabelText
import io.github.albertrzad18.treningtracker.ui.common.TtIcons
import io.github.albertrzad18.treningtracker.ui.common.daysText
import io.github.albertrzad18.treningtracker.ui.common.labelRes
import io.github.albertrzad18.treningtracker.ui.common.periodsText
import io.github.albertrzad18.treningtracker.ui.format.formatCalories
import io.github.albertrzad18.treningtracker.ui.format.formatDistanceKm
import io.github.albertrzad18.treningtracker.ui.format.formatHoursAndMinutes
import io.github.albertrzad18.treningtracker.ui.format.pluralPl
import io.github.albertrzad18.treningtracker.ui.theme.LocalTtColors
import io.github.albertrzad18.treningtracker.ui.theme.TtMotion
import io.github.albertrzad18.treningtracker.ui.theme.TtShape
import io.github.albertrzad18.treningtracker.ui.theme.TtSpace
import io.github.albertrzad18.treningtracker.ui.theme.TtType
import io.github.albertrzad18.treningtracker.ui.theme.ttDuration
import java.time.Duration

/** Nazwa celu: `Rower · Dystans (km) · Miesięczny`. */
@Composable
fun goalTitle(goal: Goal): String {
    val who = goal.discipline?.let { stringResource(it.labelRes()) } ?: stringResource(R.string.goals_all_disciplines)
    return "$who · ${stringResource(goal.metric.labelRes())} · ${stringResource(goal.period.labelRes())}"
}

@Composable
fun GoalMetric.labelRes() = when (this) {
    GoalMetric.DISTANCE -> R.string.goals_metric_distance
    GoalMetric.DURATION -> R.string.goals_metric_duration
    GoalMetric.CALORIES -> R.string.goals_metric_calories
    GoalMetric.COUNT -> R.string.goals_metric_count
}

@Composable
fun GoalPeriod.labelRes() = when (this) {
    GoalPeriod.WEEK -> R.string.goals_period_week
    GoalPeriod.MONTH -> R.string.goals_period_month
    GoalPeriod.YEAR -> R.string.goals_period_year
}

/** Wartość celu w jednostkach, w jakich ją rozumiemy: kilometry, godziny, kcal, liczba treningów. */
@Composable
fun GoalMetric.format(value: Double): String = when (this) {
    GoalMetric.DISTANCE -> formatDistanceKm(value)
    GoalMetric.DURATION -> formatHoursAndMinutes(Duration.ofSeconds(value.toLong()))
    GoalMetric.CALORIES -> formatCalories(value)
    GoalMetric.COUNT -> pluralPl(
        value.toInt(),
        stringResource(R.string.unit_workout_one),
        stringResource(R.string.unit_workout_few),
        stringResource(R.string.unit_workout_many),
    )
}

/** Przelicza to, co wpisał użytkownik (km, godziny), na jednostki bazy (metry, sekundy). */
fun GoalMetric.toStored(input: Double): Double = when (this) {
    GoalMetric.DISTANCE -> input * 1000
    GoalMetric.DURATION -> input * 3600
    else -> input
}

/** Jednostka w polu „Wartość celu”. */
@Composable
fun GoalMetric.unitLabel(): String = when (this) {
    GoalMetric.DISTANCE -> "km"
    GoalMetric.DURATION -> "h"
    GoalMetric.CALORIES -> "kcal"
    GoalMetric.COUNT -> stringResource(R.string.unit_workout_many)
}

/**
 * Jeden cel: nazwa, zrobione z założonego, pasek postępu, prognoza, ile dni zostało,
 * historia ostatnich okresów (kwadraty) i seria. Pulpit pokazuje wersję skróconą.
 */
@Composable
fun GoalProgressBlock(
    row: GoalRow,
    modifier: Modifier = Modifier,
    showForecast: Boolean = true,
    showStreak: Boolean = true,
) {
    val colors = LocalTtColors.current
    val goal = row.progress.goal
    val color = goal.discipline?.let { colors.of(it) } ?: colors.ink
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TtSpace.x2)) {
            goal.discipline?.let { DisciplineDot(it) }
            LabelText(goalTitle(goal))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TtSpace.x2)) {
            Text(
                goal.metric.format(row.progress.current),
                style = TtType.hero.copy(fontSize = 34.sp, lineHeight = 38.sp),
                color = colors.ink,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                stringResource(R.string.goals_of, goal.metric.format(goal.target)),
                style = TtType.bodyS,
                color = colors.ink2,
                modifier = Modifier.alignByBaseline(),
            )
            if (row.progress.done) {
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(TtSpace.x1, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(TtIcons.Done, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.goals_done), style = TtType.labelPlain, color = color)
                }
            }
        }
        GoalBar(fraction = row.progress.fraction.toFloat(), color = color)
        if (showForecast && !row.progress.done) {
            row.progress.projected?.let {
                Text(
                    stringResource(R.string.goals_projection, goal.metric.format(it)),
                    style = TtType.bodyS,
                    color = colors.ink,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.goals_days_left, daysText(row.progress.daysLeft)),
                style = TtType.bodyS,
                color = colors.ink2,
                modifier = Modifier.weight(1f),
            )
            GoalHistory(row.history, color)
        }
        if (showStreak && row.streak > 0) {
            Text(
                stringResource(R.string.goals_streak, periodsText(row.streak)),
                style = TtType.bodyS,
                color = colors.ink2,
            )
        }
    }
}

/** Pasek postępu 6 dp: tor `track`, wypełnienie w kolorze dyscypliny (dla „Wszystko” ink). */
@Composable
fun GoalBar(fraction: Float, color: Color, modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 6.dp) {
    val colors = LocalTtColors.current
    val target by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(ttDuration(TtMotion.RING), easing = FastOutSlowInEasing),
        label = "goal",
    )
    Box(modifier.fillMaxWidth().height(height).clip(TtShape.full).background(colors.track)) {
        if (target > 0f) {
            Box(Modifier.fillMaxWidth(target).fillMaxHeight().clip(TtShape.full).background(color))
        }
    }
}

/** Historia celu: 6 kwadratów 10 dp. Okres dowieziony jest wypełniony, niedowieziony ma tylko obrys. */
@Composable
private fun GoalHistory(history: List<Pair<DateRange, Boolean>>, color: Color) {
    if (history.isEmpty()) return
    val colors = LocalTtColors.current
    val shape = RoundedCornerShape(2.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(TtSpace.x1)) {
        // Najstarszy okres po lewej, najnowszy po prawej.
        history.reversed().forEach { (_, done) ->
            Box(
                Modifier
                    .size(10.dp)
                    .background(if (done) color else Color.Transparent, shape)
                    .border(1.dp, if (done) color else colors.line, shape),
            )
        }
    }
}

/** Nazwa celu w jednej linii do podtytułu na liście (np. w „Więcej”). */
@Composable
fun goalShortTitle(goal: Goal): String = buildString {
    append(goal.discipline?.let { stringResource(it.labelRes()) } ?: stringResource(R.string.goals_all_disciplines))
    append(" · ")
    append(stringResource(goal.metric.labelRes()))
}

/** Pogrubiona nazwa sekcji w arkuszu dodawania celu. */
@Composable
internal fun SheetGroupTitle(text: String) {
    Text(text.uppercase(), style = TtType.label, color = LocalTtColors.current.ink2, fontWeight = FontWeight.Medium)
}
