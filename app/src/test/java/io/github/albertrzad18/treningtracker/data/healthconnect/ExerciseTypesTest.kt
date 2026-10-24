package io.github.albertrzad18.treningtracker.data.healthconnect

import androidx.health.connect.client.records.ExerciseSessionRecord
import io.github.albertrzad18.treningtracker.domain.Discipline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseTypesTest {
    @Test
    fun mapsSupportedTypes() {
        assertEquals(Discipline.POOL, disciplineOf(ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL))
        assertEquals(Discipline.ROAD, disciplineOf(ExerciseSessionRecord.EXERCISE_TYPE_BIKING))
        assertEquals(Discipline.TRAINER, disciplineOf(ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY))
    }

    @Test
    fun ignoresOtherTypes() {
        assertNull(disciplineOf(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING))
        assertNull(disciplineOf(ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER))
    }
}
