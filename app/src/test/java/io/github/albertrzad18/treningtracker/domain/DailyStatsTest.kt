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
        assertEquals(5_100.0, totals.basalKcal!!, 0.001)
    }

    @Test
    fun missingDataStaysNullInsteadOfZero() {
        val totals = listOf(DailyStats(LocalDate.of(2026, 10, 20), null, null)).passiveTotals(range)
        assertNull(totals.steps)
        assertNull(totals.basalKcal)
        assertTrue(totals.isEmpty)
    }
}
