package io.github.albertrzad18.treningtracker.ui.common

import io.github.albertrzad18.treningtracker.domain.Discipline
import io.github.albertrzad18.treningtracker.domain.Totals
import io.github.albertrzad18.treningtracker.domain.Workout
import io.github.albertrzad18.treningtracker.ui.format.formatCalories
import io.github.albertrzad18.treningtracker.ui.format.formatDistanceKm
import io.github.albertrzad18.treningtracker.ui.format.formatDuration
import io.github.albertrzad18.treningtracker.ui.format.formatHeartRate
import io.github.albertrzad18.treningtracker.ui.format.formatPacePer100m
import io.github.albertrzad18.treningtracker.ui.format.formatSpeedKmh
import java.time.LocalDateTime
import java.time.ZoneId

/** Tempo /100 m dla basenu, prędkość km/h dla roweru i trenażera; `null` (pole ukryte), gdy nie ma z czego policzyć. */
fun Workout.paceOrSpeedText(): String? =
    if (discipline == Discipline.POOL) pacePer100mSec?.let(::formatPacePer100m) else avgSpeedKmh?.let(::formatSpeedKmh)

fun Workout.distanceText(): String? = distanceM?.let(::formatDistanceKm)
fun Workout.caloriesText(): String? = caloriesKcal?.let(::formatCalories)
fun Workout.avgHeartRateText(): String? = avgHeartRate?.let(::formatHeartRate)
fun Workout.durationText(): String = formatDuration(duration)

fun Workout.localDateTime(zone: ZoneId): LocalDateTime = startLocal(zone).toLocalDateTime()

/** Jedna linia podsumowania: czas, a dystans i kalorie tylko jeśli są. */
fun Totals.summaryText(): String = buildList {
    add(formatDuration(duration))
    distanceM?.let { add(formatDistanceKm(it)) }
    caloriesKcal?.let { add(formatCalories(it)) }
}.joinToString(" · ")
