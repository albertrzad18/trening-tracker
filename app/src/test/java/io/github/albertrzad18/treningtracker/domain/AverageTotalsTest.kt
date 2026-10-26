package io.github.albertrzad18.treningtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration

class AverageTotalsTest {
    @Test
    fun averagesPerWorkout() {
        val totals = Totals(count = 4, duration = Duration.ofMinutes(200), distanceM = 8_700.0, caloriesKcal = null)
        assertEquals(Duration.ofMinutes(50), totals.averageDuration)
        assertEquals(2_175.0, totals.averageDistanceM!!, 0.001)
    }

    @Test
    fun noWorkoutsOrNoDistanceGiveNull() {
        assertNull(Totals(0, Duration.ZERO, null, null).averageDuration)
        assertNull(Totals(3, Duration.ofMinutes(90), null, null).averageDistanceM)
    }
}
