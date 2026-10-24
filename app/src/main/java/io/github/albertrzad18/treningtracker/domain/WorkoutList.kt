package io.github.albertrzad18.treningtracker.domain

import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class PeriodFilter { WEEK, MONTH, YEAR, ALL }

enum class SortColumn { DATE, DURATION, DISTANCE, CALORIES, SPEED, HEART_RATE }

data class MonthGroup(val month: YearMonth, val workouts: List<Workout>, val totals: Totals)

/**
 * Wynik dla ekranu Treningi. [groups] jest wypełnione tylko przy sortowaniu po dacie
 * (grupy miesięcy), w pozostałych przypadkach używamy płaskiej listy [workouts].
 */
data class WorkoutListResult(
    val workouts: List<Workout>,
    val groups: List<MonthGroup>,
    val totals: Totals,
    val averageDuration: Duration?,
)

fun periodRange(period: PeriodFilter, today: LocalDate): DateRange? = when (period) {
    PeriodFilter.WEEK -> weekOf(today)
    PeriodFilter.MONTH -> today.withDayOfMonth(1).let { DateRange(it, it.plusMonths(1)) }
    PeriodFilter.YEAR -> today.withDayOfYear(1).let { DateRange(it, it.plusYears(1)) }
    PeriodFilter.ALL -> null
}

fun buildWorkoutList(
    all: List<Workout>,
    disciplines: Set<Discipline>,
    period: PeriodFilter,
    sort: SortColumn,
    ascending: Boolean,
    today: LocalDate,
    zone: ZoneId,
): WorkoutListResult {
    val byDiscipline = if (disciplines.isEmpty()) all else all.filter { it.discipline in disciplines }
    val range = periodRange(period, today)
    val filtered = if (range == null) byDiscipline else byDiscipline.inRange(range, zone)
    val sorted = filtered.sortedWith(comparatorFor(sort, ascending))

    val groups = if (sort == SortColumn.DATE) {
        sorted.groupBy { YearMonth.from(it.startLocal(zone)) }
            .map { (month, list) -> MonthGroup(month, list, list.totals()) }
            // sorted już ma właściwą kolejność, a groupBy ją zachowuje; grupy ułożone wg pierwszego treningu
    } else emptyList()

    val average = if (sorted.isEmpty()) null else Duration.ofSeconds(sorted.sumOf { it.duration.seconds } / sorted.size)
    return WorkoutListResult(sorted, groups, sorted.totals(), average)
}

/** Brakujące wartości zawsze na końcu, niezależnie od kierunku sortowania. */
private fun comparatorFor(sort: SortColumn, ascending: Boolean): Comparator<Workout> {
    fun <T : Comparable<T>> by(key: (Workout) -> T?): Comparator<Workout> = Comparator { a, b ->
        val ka = key(a)
        val kb = key(b)
        when {
            ka == null && kb == null -> 0
            ka == null -> 1
            kb == null -> -1
            ascending -> ka.compareTo(kb)
            else -> kb.compareTo(ka)
        }
    }
    return when (sort) {
        SortColumn.DATE -> by<Long> { it.start.toEpochMilli() }
        SortColumn.DURATION -> by<Long> { it.duration.seconds }
        SortColumn.DISTANCE -> by<Double> { it.distanceM }
        SortColumn.CALORIES -> by<Double> { it.caloriesKcal }
        SortColumn.SPEED -> by<Double> { it.avgSpeedKmh }
        SortColumn.HEART_RATE -> by<Long> { it.avgHeartRate }
    }
}
