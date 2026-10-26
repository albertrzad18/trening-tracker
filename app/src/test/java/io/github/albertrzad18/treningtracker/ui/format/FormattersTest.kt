package io.github.albertrzad18.treningtracker.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth

class FormattersTest {
    private val nbsp = " "

    @Test
    fun distance() {
        assertEquals("1${nbsp}234,5${nbsp}km", formatDistanceKm(1_234_500.0))
        assertEquals("1,5${nbsp}km", formatDistanceKm(1500.0))
        assertEquals("0,0${nbsp}km", formatDistanceKm(0.0))
    }

    @Test
    fun distanceRoundsHalfUp() {
        // 1250 m to 1,25 km: HALF_EVEN dawało 1,2, a zegarek i zdrowy rozsądek mówią 1,3.
        assertEquals("1,3${nbsp}km", formatDistanceKm(1250.0))
    }

    @Test
    fun poolWorkoutDistanceIsInMeters() {
        val pool = io.github.albertrzad18.treningtracker.domain.Discipline.POOL
        val road = io.github.albertrzad18.treningtracker.domain.Discipline.ROAD
        assertEquals("1${nbsp}250${nbsp}m", formatWorkoutDistance(pool, 1250.0))
        assertEquals("25,0${nbsp}km", formatWorkoutDistance(road, 25_000.0))
        assertEquals("1${nbsp}250", workoutDistanceParts(pool, 1250.0).value)
        assertEquals("m", workoutDistanceParts(pool, 1250.0).unit)
    }

    @Test
    fun duration() {
        assertEquals("1:23:45", formatDuration(Duration.ofSeconds(5025)))
        assertEquals("0:05:07", formatDuration(Duration.ofSeconds(307)))
        assertEquals("0:00:00", formatDuration(Duration.ZERO))
    }

    @Test
    fun date() {
        assertEquals("24 paź 2026", formatDate(LocalDate.of(2026, 10, 24)))
        assertEquals("3 sty 2026", formatDate(LocalDate.of(2026, 1, 3)))
    }

    @Test
    fun month() {
        assertEquals("październik 2026", formatMonth(YearMonth.of(2026, 10)))
    }

    @Test
    fun pace() {
        assertEquals("1:45${nbsp}/100${nbsp}m", formatPacePer100m(105.0))
        assertEquals("2:00${nbsp}/100${nbsp}m", formatPacePer100m(119.6))
    }

    @Test
    fun speedCaloriesHeartRate() {
        assertEquals("25,3${nbsp}km/h", formatSpeedKmh(25.26))
        assertEquals("1${nbsp}234${nbsp}kcal", formatCalories(1234.4))
        assertEquals("142${nbsp}bpm", formatHeartRate(142))
    }

    @Test
    fun percentChange() {
        assertEquals("+12%", formatPercentChange(12.0))
        assertEquals("-5%", formatPercentChange(-5.0))
        assertEquals("0%", formatPercentChange(0.2))
    }

    @Test
    fun dayOfWeekShortIsShortEnoughForNarrowColumns() {
        assertEquals("nd", formatDayOfWeekShort(LocalDate.of(2026, 10, 25)))
        assertEquals("czw", formatDayOfWeekShort(LocalDate.of(2026, 10, 22)))
    }

    @Test
    fun timeAndDateTime() {
        assertEquals("07:05", formatTime(java.time.LocalTime.of(7, 5)))
        assertEquals(
            "24 paź 2026, 14:30",
            formatDateTime(java.time.LocalDateTime.of(2026, 10, 24, 14, 30)),
        )
    }
}
