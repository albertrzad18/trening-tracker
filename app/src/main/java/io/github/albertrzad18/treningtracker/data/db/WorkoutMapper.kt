package io.github.albertrzad18.treningtracker.data.db

import io.github.albertrzad18.treningtracker.domain.Workout
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

fun WorkoutEntity.toDomain() = Workout(
    id = id,
    discipline = discipline,
    start = Instant.ofEpochMilli(startMillis),
    zoneOffset = zoneOffsetSec?.let { ZoneOffset.ofTotalSeconds(it) },
    duration = Duration.ofSeconds(durationSec),
    title = title,
    distanceM = distanceM,
    caloriesKcal = caloriesKcal,
    avgHeartRate = avgHeartRate,
    maxHeartRate = maxHeartRate,
    avgSpeedMps = avgSpeedMps,
    maxSpeedMps = maxSpeedMps,
    elevationGainM = elevationGainM,
)
