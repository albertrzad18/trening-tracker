package io.github.albertrzad18.treningtracker.domain

import java.time.LocalDate
import java.time.ZoneId

/** Jak długi jest jeden okres na ekranie Podsumowania. */
enum class SummaryGranularity { DAY, WEEK, MONTH, YEAR }

/**
 * Podsumowanie jednego okresu: tygodnia, miesiąca albo roku.
 * Zmiany w procentach są liczone względem poprzedniego okresu tej samej długości; `null`, gdy nie ma z czym porównać.
 */
data class PeriodSummary(
    val granularity: SummaryGranularity,
    val range: DateRange,
    val count: Int,
    val activeDays: Int,
    val totals: Totals,
    val durationChangePercent: Double?,
    val distanceChangePercent: Double?,
    val caloriesChangePercent: Double?,
    val perDiscipline: List<DisciplineTotals>,
    val best: Workout?,
    val longestStreakDays: Int,
    /** Kroki i kalorie spalane naturalnie w okresie; `null`, gdy brak danych dziennych. */
    val passive: PassiveTotals? = null,
)

/** Zakres okresu, w którym leży [day]. */
fun SummaryGranularity.rangeOf(day: LocalDate): DateRange = when (this) {
    SummaryGranularity.DAY -> DateRange(day, day.plusDays(1))
    SummaryGranularity.WEEK -> weekOf(day)
    SummaryGranularity.MONTH -> day.withDayOfMonth(1).let { DateRange(it, it.plusMonths(1)) }
    SummaryGranularity.YEAR -> day.withDayOfYear(1).let { DateRange(it, it.plusYears(1)) }
}

/** Poprzedni okres tej samej długości (poprzedni tydzień, miesiąc albo rok). */
fun SummaryGranularity.previous(range: DateRange): DateRange = when (this) {
    SummaryGranularity.DAY -> DateRange(range.start.minusDays(1), range.start)
    SummaryGranularity.WEEK -> DateRange(range.start.minusWeeks(1), range.start)
    SummaryGranularity.MONTH -> DateRange(range.start.minusMonths(1), range.start)
    SummaryGranularity.YEAR -> DateRange(range.start.minusYears(1), range.start)
}

/** Ile dni z treningiem pod rząd, licząc tylko dni z [range]. */
fun List<Workout>.longestStreak(zone: ZoneId): Int {
    val days = map { it.startLocal(zone).toLocalDate() }.distinct().sorted()
    if (days.isEmpty()) return 0
    var best = 1
    var current = 1
    for (i in 1 until days.size) {
        current = if (days[i - 1].plusDays(1) == days[i]) current + 1 else 1
        if (current > best) best = current
    }
    return best
}

/** Najlepszy trening okresu: najdłuższy czasem, a przy równym czasie dłuższy dystansem. */
fun List<Workout>.bestWorkout(): Workout? =
    maxWithOrNull(compareBy({ it.duration.seconds }, { it.distanceM ?: 0.0 }))

private fun summaryOf(
    all: List<Workout>,
    granularity: SummaryGranularity,
    range: DateRange,
    zone: ZoneId,
): PeriodSummary {
    val workouts = all.inRange(range, zone)
    val previous = all.inRange(granularity.previous(range), zone).totals()
    val totals = workouts.totals()
    return PeriodSummary(
        granularity = granularity,
        range = range,
        count = workouts.size,
        activeDays = workouts.map { it.startLocal(zone).toLocalDate() }.distinct().size,
        totals = totals,
        durationChangePercent = percentChange(totals.duration.seconds.toDouble(), previous.duration.seconds.toDouble()),
        distanceChangePercent = totals.distanceM?.let { percentChange(it, previous.distanceM) },
        caloriesChangePercent = totals.caloriesKcal?.let { percentChange(it, previous.caloriesKcal) },
        perDiscipline = workouts.perDiscipline(),
        best = workouts.bestWorkout(),
        longestStreakDays = workouts.longestStreak(zone),
    )
}

/**
 * Podsumowania kolejnych okresów od najnowszego, od [today] w przeszłość.
 * Okresy bez treningów są pomijane, poza bieżącym (żeby zawsze było widać, jak idzie teraz).
 */
fun summaries(
    all: List<Workout>,
    granularity: SummaryGranularity,
    today: LocalDate,
    zone: ZoneId,
    limit: Int = 24,
): List<PeriodSummary> {
    val oldest = all.minByOrNull { it.start }?.startLocal(zone)?.toLocalDate() ?: today
    val result = mutableListOf<PeriodSummary>()
    var range = granularity.rangeOf(today)
    while (result.size < limit && !range.endExclusive.isBefore(oldest)) {
        val summary = summaryOf(all, granularity, range, zone)
        if (summary.count > 0 || result.isEmpty()) result += summary
        range = granularity.previous(range)
    }
    return result
}
