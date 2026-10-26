package io.github.albertrzad18.treningtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

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
    fun missingDataStaysNullInsteadOfZero() {
        val totals = listOf(DailyStats(LocalDate.of(2026, 10, 20), null, null)).passiveTotals(range)
        assertNull(totals.steps)
        assertNull(totals.kcal)
        assertNull(totals.maxSteps)
        assertNull(totals.basalKcal)
        assertTrue(totals.isEmpty)
    }
}
