package io.github.albertrzad18.treningtracker.domain

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/** Trening w modelu domenowym: bez Androida, Room i Health Connect. Pola z `?` bywają puste. */
data class Workout(
    val id: String,
    val discipline: Discipline,
    val start: Instant,
    val zoneOffset: ZoneOffset?,
    val duration: Duration,
    val title: String?,
    val distanceM: Double?,
    val caloriesKcal: Double?,
    val avgHeartRate: Long?,
    val maxHeartRate: Long?,
    val avgSpeedMps: Double?,
    val maxSpeedMps: Double?,
    val elevationGainM: Double?,
) {
    val end: Instant get() = start + duration

    /** Czas lokalny początku treningu: w strefie, w której trening się odbył, a jak jej brak, w strefie telefonu. */
    fun startLocal(fallback: ZoneId): ZonedDateTime = start.atZone(zoneOffset ?: fallback)

    /** Średnia prędkość w km/h: z zapisanej prędkości albo z dystansu i czasu. `null`, gdy nie ma z czego policzyć. */
    val avgSpeedKmh: Double?
        get() = avgSpeedMps?.let { it * 3.6 }
            ?: distanceM?.takeIf { it > 0 && duration.seconds > 0 }?.let { it / duration.seconds * 3.6 }

    /** Tempo w sekundach na 100 m (do basenu). `null`, gdy brak dystansu. */
    val pacePer100mSec: Double?
        get() = distanceM?.takeIf { it > 0 && duration.seconds > 0 }?.let { duration.seconds / (it / 100.0) }
}
