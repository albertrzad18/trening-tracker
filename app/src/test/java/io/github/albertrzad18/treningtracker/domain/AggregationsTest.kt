package io.github.albertrzad18.treningtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class AggregationsTest {
    private val warsaw = ZoneId.of("Europe/Warsaw")

    private fun workout(
        start: String,
        discipline: Discipline = Discipline.POOL,
        minutes: Long = 60,
        distanceM: Double? = null,
        kcal: Double? = null,
        offset: ZoneOffset? = null,
        id: String = start,
    ) = Workout(
        id = id,
        discipline = discipline,
        start = Instant.parse(start),
        zoneOffset = offset,
        duration = Duration.ofMinutes(minutes),
        title = null,
        distanceM = distanceM,
        caloriesKcal = kcal,
        avgHeartRate = null,
        maxHeartRate = null,
        avgSpeedMps = null,
        maxSpeedMps = null,
        elevationGainM = null,
    )

    @Test
    fun totalsSumDurationDistanceAndCalories() {
        val t = listOf(
            workout("2026-10-19T10:00:00Z", distanceM = 1000.0, kcal = 300.0),
            workout("2026-10-20T10:00:00Z", minutes = 30, distanceM = 500.0, kcal = 150.0),
        ).totals()
        assertEquals(2, t.count)
        assertEquals(Duration.ofMinutes(90), t.duration)
        assertEquals(1500.0, t.distanceM!!, 0.001)
        assertEquals(450.0, t.caloriesKcal!!, 0.001)
    }

    @Test
    fun totalsDistanceIsNullWhenNoWorkoutHasIt() {
        val t = listOf(workout("2026-10-19T10:00:00Z", Discipline.TRAINER, kcal = 300.0)).totals()
        assertNull(t.distanceM)
    }

    @Test
    fun totalsOfEmptyListHaveNoDistanceOrCalories() {
        val t = emptyList<Workout>().totals()
        assertEquals(0, t.count)
        assertEquals(Duration.ZERO, t.duration)
        assertNull(t.distanceM)
        assertNull(t.caloriesKcal)
    }

    @Test
    fun weekStartsOnMonday() {
        // 2026-10-24 to sobota
        val week = weekOf(LocalDate.of(2026, 10, 24))
        assertEquals(LocalDate.of(2026, 10, 19), week.start)
        assertEquals(LocalDate.of(2026, 10, 26), week.endExclusive)
    }

    @Test
    fun sundayBelongsToTheWeekEndingThatDay() {
        val week = weekOf(LocalDate.of(2026, 10, 25))
        assertEquals(LocalDate.of(2026, 10, 19), week.start)
    }

    @Test
    fun previousRangeHasSameLengthAndEndsWhereCurrentStarts() {
        val prev = weekOf(LocalDate.of(2026, 10, 24)).previous()
        assertEquals(LocalDate.of(2026, 10, 12), prev.start)
        assertEquals(LocalDate.of(2026, 10, 19), prev.endExclusive)
    }

    @Test
    fun workoutPastMidnightCountsToStartDay() {
        // start 23:30 w niedzielę 25.10 (czas lokalny Warszawa, CET po zmianie czasu = UTC+1), trwa 60 min
        val w = workout("2026-10-25T22:30:00Z", minutes = 60)
        val sunday = DateRange(LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 26))
        val monday = DateRange(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 10, 27))
        assertEquals(1, listOf(w).inRange(sunday, warsaw).size)
        assertEquals(0, listOf(w).inRange(monday, warsaw).size)
    }

    @Test
    fun usesWorkoutOffsetInsteadOfPhoneZone() {
        // 23:30 UTC w niedzielę; trening w strefie +09:00 to już poniedziałek 08:30
        val w = workout("2026-10-25T23:30:00Z", offset = ZoneOffset.ofHours(9))
        val monday = DateRange(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 10, 27))
        assertEquals(1, listOf(w).inRange(monday, warsaw).size)
    }

    @Test
    fun percentChange() {
        assertEquals(12.0, percentChange(112.0, 100.0)!!, 0.001)
        assertEquals(-50.0, percentChange(50.0, 100.0)!!, 0.001)
    }

    @Test
    fun percentChangeIsNullWithoutUsablePreviousValue() {
        assertNull(percentChange(10.0, 0.0))
        assertNull(percentChange(10.0, null))
    }

    @Test
    fun pacePer100mFromDistanceAndDuration() {
        // 2000 m w 60 min = 3600 s -> 180 s na 100 m
        val w = workout("2026-10-19T10:00:00Z", distanceM = 2000.0)
        assertEquals(180.0, w.pacePer100mSec!!, 0.001)
    }

    @Test
    fun paceAndSpeedAreNullWithoutDistance() {
        val w = workout("2026-10-19T10:00:00Z", Discipline.TRAINER)
        assertNull(w.pacePer100mSec)
        assertNull(w.avgSpeedKmh)
    }

    @Test
    fun averageDurationOnlyCountsGivenDiscipline() {
        val list = listOf(
            workout("2026-10-19T10:00:00Z", Discipline.POOL, minutes = 60),
            workout("2026-10-20T10:00:00Z", Discipline.POOL, minutes = 30),
            workout("2026-10-21T10:00:00Z", Discipline.TRAINER, minutes = 120),
        )
        assertEquals(Duration.ofMinutes(45), list.averageDuration(Discipline.POOL))
        assertNull(list.averageDuration(Discipline.ROAD))
    }
}
