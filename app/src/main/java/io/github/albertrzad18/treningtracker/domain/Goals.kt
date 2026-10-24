package io.github.albertrzad18.treningtracker.domain

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Na jaki okres jest cel. */
enum class GoalPeriod { WEEK, MONTH, YEAR }

/** Co cel mierzy. */
enum class GoalMetric { DISTANCE, DURATION, CALORIES, COUNT }

/** Cel w modelu domenowym (bez Room). */
data class Goal(
    val id: Long,
    val discipline: Discipline?,
    val period: GoalPeriod,
    val metric: GoalMetric,
    val target: Double,
)

/**
 * Jak idzie realizacja celu w bieżącym okresie.
 * [projected] to prognoza na koniec okresu przy obecnym tempie; `null`, gdy okres dopiero się zaczął.
 */
data class GoalProgress(
    val goal: Goal,
    val current: Double,
    val projected: Double?,
    val daysLeft: Int,
) {
    val fraction: Double get() = if (goal.target <= 0) 0.0 else (current / goal.target).coerceIn(0.0, 1.0)
    val done: Boolean get() = current >= goal.target
}

fun GoalPeriod.rangeOf(day: LocalDate): DateRange = when (this) {
    GoalPeriod.WEEK -> weekOf(day)
    GoalPeriod.MONTH -> day.withDayOfMonth(1).let { DateRange(it, it.plusMonths(1)) }
    GoalPeriod.YEAR -> day.withDayOfYear(1).let { DateRange(it, it.plusYears(1)) }
}

/** Ile cel „zalicza” z jednego zbioru treningów: metry, sekundy, kcal albo sztuki. */
fun GoalMetric.valueOf(workouts: List<Workout>): Double = when (this) {
    GoalMetric.DISTANCE -> workouts.sumOf { it.distanceM ?: 0.0 }
    GoalMetric.DURATION -> workouts.sumOf { it.duration.seconds }.toDouble()
    GoalMetric.CALORIES -> workouts.sumOf { it.caloriesKcal ?: 0.0 }
    GoalMetric.COUNT -> workouts.size.toDouble()
}

/**
 * Postęp celu w okresie, w którym leży [today].
 * Prognoza liczy się z tempa dotychczasowych dni: zrobione / dni minione * dni w okresie.
 */
fun goalProgress(goal: Goal, all: List<Workout>, today: LocalDate, zone: ZoneId): GoalProgress {
    val range = goal.period.rangeOf(today)
    val workouts = all.inRange(range, zone).let { list ->
        if (goal.discipline == null) list else list.filter { it.discipline == goal.discipline }
    }
    val current = goal.metric.valueOf(workouts)
    val totalDays = ChronoUnit.DAYS.between(range.start, range.endExclusive).toInt()
    val daysPassed = (ChronoUnit.DAYS.between(range.start, today) + 1).toInt().coerceIn(1, totalDays)
    val daysLeft = (totalDays - daysPassed).coerceAtLeast(0)
    return GoalProgress(
        goal = goal,
        current = current,
        projected = if (current <= 0.0) null else current / daysPassed * totalDays,
        daysLeft = daysLeft,
    )
}

/** Czy cel był zrealizowany w okresie [range] (do historii celów). */
fun goalDone(goal: Goal, all: List<Workout>, range: DateRange, zone: ZoneId): Boolean {
    val workouts = all.inRange(range, zone).let { list ->
        if (goal.discipline == null) list else list.filter { it.discipline == goal.discipline }
    }
    return goal.metric.valueOf(workouts) >= goal.target
}

/**
 * Historia celu: ostatnie [count] zakończonych okresów od najnowszego, z informacją, czy cel był dowieziony.
 * Bieżący okres pomijamy, bo jeszcze trwa.
 */
fun goalHistory(
    goal: Goal,
    all: List<Workout>,
    today: LocalDate,
    zone: ZoneId,
    count: Int = 6,
): List<Pair<DateRange, Boolean>> {
    var range = goal.period.rangeOf(today)
    return (1..count).map {
        range = when (goal.period) {
            GoalPeriod.WEEK -> DateRange(range.start.minusWeeks(1), range.start)
            GoalPeriod.MONTH -> DateRange(range.start.minusMonths(1), range.start)
            GoalPeriod.YEAR -> DateRange(range.start.minusYears(1), range.start)
        }
        range to goalDone(goal, all, range, zone)
    }
}

/** Najdłuższa seria kolejnych okresów z celem zrealizowanym, licząc od ostatniego zakończonego wstecz. */
fun goalStreak(history: List<Pair<DateRange, Boolean>>): Int = history.takeWhile { it.second }.size
