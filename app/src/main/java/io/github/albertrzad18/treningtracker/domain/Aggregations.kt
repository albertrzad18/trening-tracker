package io.github.albertrzad18.treningtracker.domain

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Sumy dla grupy treningów. Dystans to `null`, gdy żaden trening nie miał dystansu (nie pokazujemy zer). */
data class Totals(
    val count: Int,
    val duration: Duration,
    val distanceM: Double?,
    val caloriesKcal: Double?,
)

fun List<Workout>.totals(): Totals = Totals(
    count = size,
    duration = fold(Duration.ZERO) { acc, w -> acc + w.duration },
    distanceM = mapNotNull { it.distanceM }.takeIf { it.isNotEmpty() }?.sum(),
    caloriesKcal = mapNotNull { it.caloriesKcal }.takeIf { it.isNotEmpty() }?.sum(),
)

/** Zakres dat `[start, end)`: początek włącznie, koniec wyłącznie. */
data class DateRange(val start: LocalDate, val endExclusive: LocalDate) {
    fun previous(): DateRange {
        val days = java.time.temporal.ChronoUnit.DAYS.between(start, endExclusive)
        return DateRange(start.minusDays(days), start)
    }
}

/** Tydzień od poniedziałku do niedzieli, w którym leży [day]. */
fun weekOf(day: LocalDate): DateRange {
    val monday = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return DateRange(monday, monday.plusDays(7))
}

/** Treningi, których lokalny dzień rozpoczęcia mieści się w [range]. Trening przez północ liczy się do dnia startu. */
fun List<Workout>.inRange(range: DateRange, fallbackZone: ZoneId): List<Workout> = filter {
    val day = it.startLocal(fallbackZone).toLocalDate()
    !day.isBefore(range.start) && day.isBefore(range.endExclusive)
}

/**
 * Zmiana w procentach względem poprzedniej wartości, np. 12.0 dla +12%.
 * `null`, gdy poprzednia wartość to 0 lub jej nie ma (nie da się policzyć sensownie).
 */
fun percentChange(current: Double, previous: Double?): Double? =
    if (previous == null || previous == 0.0) null else (current - previous) / previous * 100.0

/** Średnia czasu trwania z treningów danej dyscypliny; `null`, gdy brak treningów. */
fun List<Workout>.averageDuration(discipline: Discipline): Duration? {
    val group = filter { it.discipline == discipline }
    if (group.isEmpty()) return null
    return Duration.ofSeconds(group.sumOf { it.duration.seconds } / group.size)
}

/** Średni dystans (m) z treningów danej dyscypliny, które go mają; `null`, gdy nie ma żadnego. */
fun List<Workout>.averageDistanceM(discipline: Discipline): Double? =
    filter { it.discipline == discipline }.mapNotNull { it.distanceM }.takeIf { it.isNotEmpty() }?.average()

fun Instant.toLocalDate(zone: ZoneId): LocalDate = atZone(zone).toLocalDate()
