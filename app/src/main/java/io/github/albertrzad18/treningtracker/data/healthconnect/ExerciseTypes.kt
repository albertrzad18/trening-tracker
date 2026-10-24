package io.github.albertrzad18.treningtracker.data.healthconnect

import androidx.health.connect.client.records.ExerciseSessionRecord
import io.github.albertrzad18.treningtracker.domain.Discipline

/** Zamienia typ ćwiczenia z Health Connect na naszą dyscyplinę; `null` = nie interesuje nas. */
fun disciplineOf(exerciseType: Int): Discipline? = when (exerciseType) {
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> Discipline.POOL
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> Discipline.ROAD
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> Discipline.TRAINER
    else -> null
}
