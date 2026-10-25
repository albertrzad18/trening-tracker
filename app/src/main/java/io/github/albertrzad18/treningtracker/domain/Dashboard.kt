package io.github.albertrzad18.treningtracker.domain

import java.time.LocalDate
import java.time.ZoneId

/**
 * Dane Pulpitu: ostatni tydzień na tle poprzedniego (łącznie i w podziale na dyscypliny)
 * oraz ostatnie treningi. Zmiany w procentach są `null`, gdy nie ma z czym porównać.
 */
data class DashboardData(
    val isEmpty: Boolean,
    val weekRange: DateRange,
    val week: Totals,
    val previousWeek: Totals,
    val durationChangePercent: Double?,
    val caloriesChangePercent: Double?,
    val perDiscipline: List<DisciplineTotals>,
    val previousPerDiscipline: List<DisciplineTotals>,
    val latest: List<Workout>,
)

fun buildDashboard(workouts: List<Workout>, today: LocalDate, zone: ZoneId, latestCount: Int = 3): DashboardData {
    val currentRange = weekOf(today)
    val thisWeek = workouts.inRange(currentRange, zone)
    val lastWeek = workouts.inRange(currentRange.previous(), zone)
    val week = thisWeek.totals()
    val previous = lastWeek.totals()
    return DashboardData(
        isEmpty = workouts.isEmpty(),
        weekRange = currentRange,
        week = week,
        previousWeek = previous,
        durationChangePercent = percentChange(week.duration.seconds.toDouble(), previous.duration.seconds.toDouble()),
        caloriesChangePercent = week.caloriesKcal?.let { percentChange(it, previous.caloriesKcal) },
        perDiscipline = thisWeek.perDiscipline(),
        previousPerDiscipline = lastWeek.perDiscipline(),
        latest = workouts.sortedByDescending { it.start }.take(latestCount),
    )
}
