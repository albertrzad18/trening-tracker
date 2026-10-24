package io.github.albertrzad18.treningtracker.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Rodzaj przebiegu w czasie, który zapisujemy dla treningu. */
enum class SampleType { HEART_RATE, SPEED }

/**
 * Jedna próbka z przebiegu treningu (tętno albo prędkość).
 * Próbek bywa dużo, więc przy zapisie je przerzedzamy, a usunięcie treningu kasuje je razem z nim.
 */
@Entity(
    tableName = "samples",
    indices = [Index("workoutId", "type")],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: String,
    val type: SampleType,
    val timeMillis: Long,
    /** Tętno w uderzeniach na minutę albo prędkość w metrach na sekundę. */
    val value: Double,
)

/** Punkt trasy GPS: tylko rower na dworze. */
@Entity(
    tableName = "route_points",
    indices = [Index("workoutId")],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class RoutePointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: String,
    val timeMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val altitudeM: Double?,
)
