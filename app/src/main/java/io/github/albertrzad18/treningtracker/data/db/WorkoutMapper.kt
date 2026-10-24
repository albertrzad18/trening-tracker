package io.github.albertrzad18.treningtracker.data.db

import io.github.albertrzad18.treningtracker.domain.Goal
import io.github.albertrzad18.treningtracker.domain.RoutePointDomain
import io.github.albertrzad18.treningtracker.domain.RouteTrack
import io.github.albertrzad18.treningtracker.domain.SamplePoint
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

fun SampleEntity.toDomain() = SamplePoint(timeMillis = timeMillis, value = value)

fun List<RoutePointEntity>.toTrack() = RouteTrack(
    map { RoutePointDomain(it.timeMillis, it.latitude, it.longitude, it.altitudeM) },
)

fun GoalEntity.toDomain() = Goal(
    id = id,
    discipline = discipline,
    period = period,
    metric = metric,
    target = target,
)

fun Goal.toEntity(createdAtMillis: Long) = GoalEntity(
    id = id,
    discipline = discipline,
    period = period,
    metric = metric,
    target = target,
    createdAtMillis = createdAtMillis,
)
