package io.github.albertrzad18.treningtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

class DailyStatsTest {
    private val range = DateRange(LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 26))

    @Test
    fun sumsOnlyDaysInsideRangeAndAveragesDaysWithSteps() {
        val days = listOf(
            DailyStats(LocalDate.of(2026, 10, 18), 99_999, 1_000.0),
            DailyStats(LocalDate.of(2026, 10, 19), 6_000, 1_700.0),
            DailyStats(LocalDate.of(2026, 10, 20), 10_000, 1_700.0),
            DailyStats(LocalDate.of(2026, 10, 21), null, 1_700.0),
        )
        val totals = days.passiveTotals(range)
        assertEquals(16_000L, totals.steps)
        assertEquals(8_000L, totals.averageSteps)
        assertEquals(5_100.0, totals.kcal!!, 0.001)
    }

    @Test
    fun caloriesAreRestingPlusActive() {
        val days = listOf(
            DailyStats(LocalDate.of(2026, 10, 20), null, 1_564.0, 2_036.0),
            DailyStats(LocalDate.of(2026, 10, 21), null, 1_600.0, null),
            DailyStats(LocalDate.of(2026, 10, 22), null, null, 500.0),
        )
        val totals = days.passiveTotals(range)
        assertEquals(3_600.0, days[0].totalKcal!!, 0.001)
        assertEquals(1_600.0, days[1].totalKcal!!, 0.001)
        assertEquals(5_700.0, totals.kcal!!, 0.001)
        assertEquals(1_900.0, totals.averageKcal!!, 0.001)
    }

    @Test
    fun bestStepsDayAndRestingCalories() {
        val days = listOf(
            DailyStats(LocalDate.of(2026, 10, 19), 6_000, 1_600.0, 400.0),
            DailyStats(LocalDate.of(2026, 10, 20), 14_500, 1_650.0, 900.0),
            DailyStats(LocalDate.of(2026, 10, 21), 9_000, null, 300.0),
        )
        val totals = days.passiveTotals(range)
        assertEquals(14_500L, totals.maxSteps)
        assertEquals(3_250.0, totals.basalKcal!!, 0.001)
        assertEquals(1_600.0, totals.activeKcal!!, 0.001)
        assertEquals(533.333, totals.averageActiveKcal!!, 0.001)
        assertEquals(900.0, totals.maxActiveKcal!!, 0.001)
    }

    @Test
    fun workoutCaloriesAreAddedToActiveOfTheirDay() {
        val zone = ZoneId.of("Europe/Warsaw")
        fun workout(day: Int, kcal: Double?) = Workout(
            id = "w$day$kcal", discipline = Discipline.POOL,
            start = LocalDate.of(2026, 10, day).atTime(18, 0).atZone(zone).toInstant(), zoneOffset = null,
            duration = Duration.ofHours(1), title = null, distanceM = null, caloriesKcal = kcal,
            avgHeartRate = null, maxHeartRate = null, avgSpeedMps = null, maxSpeedMps = null, elevationGainM = null,
        )
        val days = listOf(
            DailyStats(LocalDate.of(2026, 10, 26), 5_000, 1_564.0, 300.0),
            DailyStats(LocalDate.of(2026, 10, 27), 6_571, 1_564.0, 214.0),
            DailyStats(LocalDate.of(2026, 10, 28), null, 1_564.0, null),
        )
        val result = days.withWorkoutCalories(
            listOf(workout(27, 409.0), workout(27, null), workout(28, 250.0)),
            zone,
        )
        assertEquals(300.0, result[0].activeKcal!!, 0.001)
        assertEquals(623.0, result[1].activeKcal!!, 0.001)
        assertEquals(250.0, result[2].activeKcal!!, 0.001)
    }

    @Test
    fun missingDataStaysNullInsteadOfZero() {
        val totals = listOf(DailyStats(LocalDate.of(2026, 10, 20), null, null)).passiveTotals(range)
        assertNull(totals.steps)
        assertNull(totals.kcal)
        assertNull(totals.maxSteps)
        assertNull(totals.basalKcal)
        assertTrue(totals.isEmpty)
    }
}
