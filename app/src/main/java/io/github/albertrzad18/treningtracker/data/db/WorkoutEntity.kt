package io.github.albertrzad18.treningtracker.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.albertrzad18.treningtracker.domain.Discipline

/**
 * Jeden trening zapisany lokalnie. Pola z `?` mogą być puste (np. dystans na trenażerze),
 * wtedy w UI je ukrywamy.
 */
@Entity(
    tableName = "workouts",
    indices = [Index("startMillis"), Index("discipline")],
)
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val discipline: Discipline,
    val startMillis: Long,
    val zoneOffsetSec: Int?,
    val durationSec: Long,
    val title: String?,
    val distanceM: Double?,
    val caloriesKcal: Double?,
    val avgHeartRate: Long?,
    val maxHeartRate: Long?,
    val avgSpeedMps: Double?,
    val maxSpeedMps: Double?,
    val elevationGainM: Double?,
    val dataOrigin: String,
    val syncedAt: Long,
)
