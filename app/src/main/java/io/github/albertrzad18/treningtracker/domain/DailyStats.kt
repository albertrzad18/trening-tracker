package io.github.albertrzad18.treningtracker.domain

import java.time.LocalDate

/** Kroki i kalorie spalane naturalnie w jednym dniu; `null` = brak danych. */
data class DailyStats(val date: LocalDate, val steps: Long?, val basalKcal: Double?)

/**
 * Sumy z dni okresu. Pola są `null`, gdy w okresie nie ma żadnej wartości (nie pokazujemy zer).
 * [averageSteps] liczymy tylko z dni, które mają kroki.
 */
data class PassiveTotals(
    val steps: Long?,
    val averageSteps: Long?,
    val basalKcal: Double?,
    val averageBasalKcal: Double? = null,
) {
    val isEmpty: Boolean get() = steps == null && basalKcal == null
}

fun List<DailyStats>.passiveTotals(range: DateRange): PassiveTotals {
    val days = filter { !it.date.isBefore(range.start) && it.date.isBefore(range.endExclusive) }
    val stepDays = days.mapNotNull { it.steps }
    val basal = days.mapNotNull { it.basalKcal }
    return PassiveTotals(
        steps = stepDays.takeIf { it.isNotEmpty() }?.sum(),
        averageSteps = stepDays.takeIf { it.isNotEmpty() }?.let { it.sum() / it.size },
        basalKcal = basal.takeIf { it.isNotEmpty() }?.sum(),
        averageBasalKcal = basal.takeIf { it.isNotEmpty() }?.average(),
    )
}
