package io.github.albertrzad18.treningtracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.albertrzad18.treningtracker.domain.Discipline
import io.github.albertrzad18.treningtracker.domain.GoalMetric
import io.github.albertrzad18.treningtracker.domain.GoalPeriod

/**
 * Cel ustawiony przez użytkownika, np. „100 km rowerem w miesiącu”.
 * `discipline = null` znaczy: wszystkie dyscypliny razem.
 */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val discipline: Discipline?,
    val period: GoalPeriod,
    val metric: GoalMetric,
    /** Wartość w jednostce metryki: metry, sekundy, kcal albo liczba treningów. */
    val target: Double,
    val createdAtMillis: Long,
)
