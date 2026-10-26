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
) {
    /** Średnia prędkość ważona czasem (dystans przez czas), `null` bez dystansu. */
    val avgSpeedKmh: Double?
        get() = distanceM?.takeIf { it > 0 && duration.seconds > 0 }?.let { it / 1000.0 / (duration.seconds / 3600.0) }

    /** Średnie tempo na 100 m (czas przez dystans), `null` bez dystansu. */
    val pacePer100mSec: Double?
        get() = distanceM?.takeIf { it > 0 && duration.seconds > 0 }?.let { duration.seconds / (it / 100.0) }

    /** Średni czas jednego treningu; `null` bez treningów. */
    val averageDuration: Duration?
        get() = if (count == 0) null else Duration.ofSeconds(duration.seconds / count)

    /** Średni dystans jednego treningu; `null` bez treningów albo bez dystansu. */
    val averageDistanceM: Double?
        get() = if (count == 0) null else distanceM?.let { it / count }
}

fun List<Workout>.totals(): Totals = Totals(
    count = size,
    duration = fold(Duration.ZERO) { acc, w -> acc + w.duration },
    distanceM = mapNotNull { it.distanceM }.takeIf { it.isNotEmpty() }?.sum(),
    caloriesKcal = mapNotNull { it.caloriesKcal }.takeIf { it.isNotEmpty() }?.sum(),
)

/** Sumy jednej dyscypliny: używane na kafelkach Pulpitu i w podsumowaniach okresów. */
data class DisciplineTotals(val discipline: Discipline, val totals: Totals)

/** Sumy dla każdej z trzech dyscyplin, zawsze w tej samej kolejności (także zerowe). */
fun List<Workout>.perDiscipline(): List<DisciplineTotals> =
    Discipline.entries.map { d -> DisciplineTotals(d, filter { it.discipline == d }.totals()) }

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
