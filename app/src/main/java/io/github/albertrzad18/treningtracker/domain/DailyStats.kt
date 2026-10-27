package io.github.albertrzad18.treningtracker.domain

import java.time.LocalDate
import java.time.ZoneId

/** Kroki i kalorie z jednego dnia (spoczynkowe + aktywne); `null` = brak danych. */
data class DailyStats(
    val date: LocalDate,
    val steps: Long?,
    val basalKcal: Double?,
    val activeKcal: Double? = null,
) {
    /** Kalorie całego dnia jak na zegarku; `null`, gdy nie ma żadnej z dwóch części. */
    val totalKcal: Double? get() = if (basalKcal == null && activeKcal == null) null else (basalKcal ?: 0.0) + (activeKcal ?: 0.0)
}

/**
 * Sumy z dni okresu. Pola są `null`, gdy w okresie nie ma żadnej wartości (nie pokazujemy zer).
 * [averageSteps] liczymy tylko z dni, które mają kroki, a [averageKcal] tylko z dni z kaloriami.
 * [maxSteps] to najlepszy dzień okresu, [basalKcal] same kalorie spoczynkowe (bez ruchu),
 * a [activeKcal] same aktywne (to, co Huawei Health nazywa „kalorie w ruchu”).
 */
data class PassiveTotals(
    val steps: Long?,
    val averageSteps: Long?,
    val kcal: Double?,
    val averageKcal: Double? = null,
    val maxSteps: Long? = null,
    val basalKcal: Double? = null,
    val activeKcal: Double? = null,
    val averageActiveKcal: Double? = null,
    val maxActiveKcal: Double? = null,
) {
    val isEmpty: Boolean get() = steps == null && kcal == null && activeKcal == null
}

/**
 * Dolicza kalorie treningów do kalorii aktywnych dnia. Zegarek Huawei wlicza trening do „aktywnych”,
 * ale Health Sync zapisuje kalorie treningu tylko przy samym treningu, a w kaloriach aktywnych dnia ich brakuje.
 */
fun List<DailyStats>.withWorkoutCalories(workouts: List<Workout>, zone: ZoneId): List<DailyStats> {
    val kcalByDay = workouts
        .mapNotNull { w -> w.caloriesKcal?.let { w.startLocal(zone).toLocalDate() to it } }
        .groupBy({ it.first }, { it.second })
        .mapValues { it.value.sum() }
    return map { day ->
        val extra = kcalByDay[day.date] ?: return@map day
        day.copy(activeKcal = (day.activeKcal ?: 0.0) + extra)
    }
}

fun List<DailyStats>.passiveTotals(range: DateRange): PassiveTotals {
    val days = filter { !it.date.isBefore(range.start) && it.date.isBefore(range.endExclusive) }
    val stepDays = days.mapNotNull { it.steps }
    val kcal = days.mapNotNull { it.totalKcal }
    val basal = days.mapNotNull { it.basalKcal }
    val active = days.mapNotNull { it.activeKcal }
    return PassiveTotals(
        steps = stepDays.takeIf { it.isNotEmpty() }?.sum(),
        averageSteps = stepDays.takeIf { it.isNotEmpty() }?.let { it.sum() / it.size },
        kcal = kcal.takeIf { it.isNotEmpty() }?.sum(),
        averageKcal = kcal.takeIf { it.isNotEmpty() }?.average(),
        maxSteps = stepDays.maxOrNull(),
        basalKcal = basal.takeIf { it.isNotEmpty() }?.sum(),
        activeKcal = active.takeIf { it.isNotEmpty() }?.sum(),
        averageActiveKcal = active.takeIf { it.isNotEmpty() }?.average(),
        maxActiveKcal = active.maxOrNull(),
    )
}
