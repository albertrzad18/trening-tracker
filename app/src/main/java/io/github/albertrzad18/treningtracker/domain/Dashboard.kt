package io.github.albertrzad18.treningtracker.domain

import java.time.LocalDate
import java.time.ZoneId

data class DisciplineWeek(val discipline: Discipline, val totals: Totals)

/** Dane Pulpitu: bieżący tydzień na tle poprzedniego, podział na dyscypliny i ostatnie treningi. */
data class DashboardData(
    val isEmpty: Boolean,
    val week: Totals,
    val previousWeek: Totals,
    val durationChangePercent: Double?,
    val perDiscipline: List<DisciplineWeek>,
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
        perDiscipline = Discipline.entries.map { d -> DisciplineWeek(d, thisWeek.filter { it.discipline == d }.totals()) },
        latest = workouts.sortedByDescending { it.start }.take(latestCount),
    )
}
