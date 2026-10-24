package io.github.albertrzad18.treningtracker.domain

import java.time.LocalDate
import java.time.ZoneId

/** Dane Pulpitu: bieżący tydzień na tle poprzedniego, podział na dyscypliny, mini-wykres 7 dni i ostatnie treningi. */
data class DashboardData(
    val isEmpty: Boolean,
    val week: Totals,
    val previousWeek: Totals,
    val durationChangePercent: Double?,
    val perDiscipline: List<DisciplineTotals>,
    val lastSevenDays: List<Bucket>,
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
        week = week,
        previousWeek = previous,
        durationChangePercent = percentChange(week.duration.seconds.toDouble(), previous.duration.seconds.toDouble()),
        perDiscipline = thisWeek.perDiscipline(),
        // Mini-wykres: 7 ostatnich dni włącznie z dziś, niezależnie od granic tygodnia.
        lastSevenDays = workouts.bucketize(DateRange(today.minusDays(6), today.plusDays(1)), Granularity.DAY, zone),
        latest = workouts.sortedByDescending { it.start }.take(latestCount),
    )
}
