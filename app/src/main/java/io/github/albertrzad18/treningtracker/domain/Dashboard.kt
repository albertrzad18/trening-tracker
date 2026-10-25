package io.github.albertrzad18.treningtracker.domain

import java.time.LocalDate
import java.time.ZoneId

/**
 * Dane Pulpitu: ostatni tydzień na tle poprzedniego (łącznie i w podziale na dyscypliny)
 * oraz ostatnie treningi. Zmiany w procentach są `null`, gdy nie ma z czym porównać.
 */
data class DashboardData(
    val isEmpty: Boolean,
    val range: DateRange,
    val week: Totals,
    val previousWeek: Totals,
    val durationChangePercent: Double?,
    val caloriesChangePercent: Double?,
    val perDiscipline: List<DisciplineTotals>,
    val previousPerDiscipline: List<DisciplineTotals>,
    val latest: List<Workout>,
    /** Kroki i kalorie spalane naturalnie w okresie; `null`, gdy nie ma żadnych danych dziennych. */
    val passive: PassiveTotals? = null,
)

fun buildDashboard(
    workouts: List<Workout>,
    today: LocalDate,
    zone: ZoneId,
    latestCount: Int = 3,
    granularity: SummaryGranularity = SummaryGranularity.WEEK,
): DashboardData {
    // Okres to tydzień albo miesiąc: ten sam ekran w dwóch skalach.
    val currentRange = granularity.rangeOf(today)
    val thisWeek = workouts.inRange(currentRange, zone)
    val lastWeek = workouts.inRange(granularity.previous(currentRange), zone)
    val week = thisWeek.totals()
    val previous = lastWeek.totals()
    return DashboardData(
        isEmpty = workouts.isEmpty(),
        range = currentRange,
        week = week,
        previousWeek = previous,
        durationChangePercent = percentChange(week.duration.seconds.toDouble(), previous.duration.seconds.toDouble()),
        caloriesChangePercent = week.caloriesKcal?.let { percentChange(it, previous.caloriesKcal) },
        perDiscipline = thisWeek.perDiscipline(),
        previousPerDiscipline = lastWeek.perDiscipline(),
        latest = workouts.sortedByDescending { it.start }.take(latestCount),
    )
}
