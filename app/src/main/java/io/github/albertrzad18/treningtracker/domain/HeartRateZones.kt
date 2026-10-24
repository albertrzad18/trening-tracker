package io.github.albertrzad18.treningtracker.domain

import java.time.Duration

/** Punkt przebiegu: czas (ms) i wartość (tętno w bpm albo prędkość w m/s). */
data class SamplePoint(val timeMillis: Long, val value: Double)

/** Strefa tętna: numer 1–5 i zakres uderzeń na minutę. [toBpm] ostatniej strefy to maksymalne tętno. */
data class HeartRateZone(val index: Int, val fromBpm: Int, val toBpm: Int)

/** Domyślne maksymalne tętno, gdy użytkownik nie poda swojego. */
const val DEFAULT_MAX_HEART_RATE = 190

/** Pięć stref po 10% maksymalnego tętna, od 50% w górę. */
fun heartRateZones(maxHeartRate: Int): List<HeartRateZone> = (1..5).map { i ->
    HeartRateZone(
        index = i,
        fromBpm = (maxHeartRate * (0.4 + i * 0.1)).toInt(),
        toBpm = if (i == 5) maxHeartRate else (maxHeartRate * (0.5 + i * 0.1)).toInt(),
    )
}

/** Numer strefy (1–5) dla tętna; poniżej pierwszej strefy zwraca `null` (rozgrzewka, przerwa). */
fun zoneOf(bpm: Double, zones: List<HeartRateZone>): Int? =
    zones.lastOrNull { bpm >= it.fromBpm }?.index

/**
 * Ile czasu trening spędził w każdej strefie (indeks 0 to strefa 1).
 * Każda próbka „trzyma” czas do następnej; ostatnia dostaje typowy odstęp z reszty.
 */
fun timeInZones(samples: List<SamplePoint>, zones: List<HeartRateZone>): List<Duration> {
    val result = MutableList(zones.size) { 0L }
    if (samples.size < 2) return result.map { Duration.ofMillis(it) }
    val sorted = samples.sortedBy { it.timeMillis }
    val gaps = sorted.zipWithNext { a, b -> b.timeMillis - a.timeMillis }
    val typicalGap = gaps.sorted()[gaps.size / 2]
    sorted.forEachIndexed { i, sample ->
        val gap = if (i < sorted.size - 1) sorted[i + 1].timeMillis - sorted[i].timeMillis else typicalGap
        zoneOf(sample.value, zones)?.let { zone -> result[zone - 1] += gap }
    }
    return result.map { Duration.ofMillis(it) }
}

/**
 * Przerzedza przebieg do najwyżej [maxPoints] punktów, uśredniając wartości w równych koszykach.
 * Dzięki temu w bazie nie ląduje kilka tysięcy próbek z jednego treningu.
 */
fun downsample(samples: List<SamplePoint>, maxPoints: Int = 300): List<SamplePoint> {
    if (samples.size <= maxPoints) return samples
    val bucketSize = (samples.size + maxPoints - 1) / maxPoints
    return samples.chunked(bucketSize) { chunk ->
        SamplePoint(chunk.first().timeMillis, chunk.sumOf { it.value } / chunk.size)
    }
}
