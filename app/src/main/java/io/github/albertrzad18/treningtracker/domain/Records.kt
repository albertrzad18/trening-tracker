package io.github.albertrzad18.treningtracker.domain

import java.time.LocalDate
import java.time.ZoneId

/** Rodzaj rekordu osobistego. Każdy liczy się osobno dla każdej dyscypliny. */
enum class RecordKind {
    LONGEST_DISTANCE,
    LONGEST_DURATION,
    BEST_AVG_SPEED,
    BEST_PACE,
    MOST_CALORIES,
    BIGGEST_ELEVATION,
}

/** Jeden rekord: wartość, trening, w którym padł, i jego data. */
data class PersonalRecord(
    val kind: RecordKind,
    val discipline: Discipline,
    val value: Double,
    val workoutId: String,
    val date: LocalDate,
)

/** Rekord okresu: największy dystans albo czas w jednym tygodniu lub miesiącu. */
data class PeriodRecord(val range: DateRange, val distanceM: Double?, val durationSec: Long)

/** Minimalny dystans, od którego liczymy rekord średniej prędkości: krótkie zrywy nie mają być rekordem. */
const val MIN_SPEED_RECORD_DISTANCE_M = 20_000.0

/** Minimalny dystans basenowy dla rekordu tempa. */
const val MIN_PACE_RECORD_DISTANCE_M = 1_000.0

/** Wszystkie rekordy jednej dyscypliny; rodzaje bez danych są pomijane. */
fun List<Workout>.recordsFor(discipline: Discipline, zone: ZoneId): List<PersonalRecord> {
    val group = filter { it.discipline == discipline }
    if (group.isEmpty()) return emptyList()

    fun record(kind: RecordKind, workout: Workout?, value: Double?): PersonalRecord? {
        if (workout == null || value == null) return null
        return PersonalRecord(kind, discipline, value, workout.id, workout.startLocal(zone).toLocalDate())
    }

    fun best(selector: (Workout) -> Double?, highest: Boolean, filter: (Workout) -> Boolean = { true }): Pair<Workout, Double>? {
        val candidates = group.filter(filter).mapNotNull { w -> selector(w)?.let { w to it } }
        return if (highest) candidates.maxByOrNull { it.second } else candidates.minByOrNull { it.second }
    }

    val distance = best({ it.distanceM }, highest = true)
    val duration = best({ it.duration.seconds.toDouble() }, highest = true)
    val speed = best({ it.avgSpeedKmh }, highest = true) { (it.distanceM ?: 0.0) >= MIN_SPEED_RECORD_DISTANCE_M }
    val pace = best({ it.pacePer100mSec }, highest = false) { (it.distanceM ?: 0.0) >= MIN_PACE_RECORD_DISTANCE_M }
    val calories = best({ it.caloriesKcal }, highest = true)
    val elevation = best({ it.elevationGainM }, highest = true)

    return listOfNotNull(
        record(RecordKind.LONGEST_DISTANCE, distance?.first, distance?.second),
        record(RecordKind.LONGEST_DURATION, duration?.first, duration?.second),
        // Basen mierzymy tempem, rower i trenażer prędkością.
        if (discipline == Discipline.POOL) {
            record(RecordKind.BEST_PACE, pace?.first, pace?.second)
        } else {
            record(RecordKind.BEST_AVG_SPEED, speed?.first, speed?.second)
        },
        record(RecordKind.MOST_CALORIES, calories?.first, calories?.second),
        record(RecordKind.BIGGEST_ELEVATION, elevation?.first, elevation?.second),
    )
}

/** Najlepszy tydzień albo miesiąc pod względem dystansu i czasu. */
fun List<Workout>.bestPeriod(granularity: Granularity, zone: ZoneId, byDistance: Boolean): PeriodRecord? {
    if (isEmpty()) return null
    val groups = groupBy { granularity.startOf(it.startLocal(zone).toLocalDate()) }
    val best = groups.entries
        .mapNotNull { (start, list) ->
            val totals = list.totals()
            val value = if (byDistance) totals.distanceM else totals.duration.seconds.toDouble()
            value?.takeIf { it > 0 }?.let { Triple(start, totals, it) }
        }
        .maxByOrNull { it.third } ?: return null
    return PeriodRecord(
        range = DateRange(best.first, granularity.next(best.first)),
        distanceM = best.second.distanceM,
        durationSec = best.second.duration.seconds,
    )
}

/**
 * Jak rekord danego rodzaju poprawiał się w czasie: kolejne treningi, które pobiły poprzednią wartość.
 * Dla tempa „lepiej” znaczy mniej.
 */
fun List<Workout>.recordHistory(kind: RecordKind, discipline: Discipline, zone: ZoneId): List<PersonalRecord> {
    val lower = kind == RecordKind.BEST_PACE
    val values = filter { it.discipline == discipline }
        .sortedBy { it.start }
        .mapNotNull { w -> valueOf(w, kind)?.let { w to it } }
        .filter { (w, _) -> qualifies(w, kind) }

    val result = mutableListOf<PersonalRecord>()
    var best: Double? = null
    values.forEach { (workout, value) ->
        val current = best
        val better = current == null || if (lower) value < current else value > current
        if (better) {
            best = value
            result += PersonalRecord(kind, discipline, value, workout.id, workout.startLocal(zone).toLocalDate())
        }
    }
    return result
}

private fun valueOf(w: Workout, kind: RecordKind): Double? = when (kind) {
    RecordKind.LONGEST_DISTANCE -> w.distanceM
    RecordKind.LONGEST_DURATION -> w.duration.seconds.toDouble()
    RecordKind.BEST_AVG_SPEED -> w.avgSpeedKmh
    RecordKind.BEST_PACE -> w.pacePer100mSec
    RecordKind.MOST_CALORIES -> w.caloriesKcal
    RecordKind.BIGGEST_ELEVATION -> w.elevationGainM
}

private fun qualifies(w: Workout, kind: RecordKind): Boolean = when (kind) {
    RecordKind.BEST_AVG_SPEED -> (w.distanceM ?: 0.0) >= MIN_SPEED_RECORD_DISTANCE_M
    RecordKind.BEST_PACE -> (w.distanceM ?: 0.0) >= MIN_PACE_RECORD_DISTANCE_M
    else -> true
}
