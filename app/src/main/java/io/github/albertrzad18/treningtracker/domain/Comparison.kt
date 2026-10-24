package io.github.albertrzad18.treningtracker.domain

/** Porównanie treningu ze średnią z innych treningów tej samej dyscypliny; wartości w %, `null` gdy nie ma z czym porównać. */
data class AverageComparison(val durationChangePercent: Double?, val distanceChangePercent: Double?)

fun compareToAverage(workout: Workout, all: List<Workout>): AverageComparison? {
    val others = all.filter { it.discipline == workout.discipline && it.id != workout.id }
    if (others.isEmpty()) return null
    val avgDuration = others.map { it.duration.seconds.toDouble() }.average()
    val avgDistance = others.mapNotNull { it.distanceM }.takeIf { it.isNotEmpty() }?.average()
    return AverageComparison(
        durationChangePercent = percentChange(workout.duration.seconds.toDouble(), avgDuration),
        distanceChangePercent = workout.distanceM?.let { percentChange(it, avgDistance) },
    )
}
