package io.github.albertrzad18.treningtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class RecordsTest {
    private val zone = ZoneId.of("Europe/Warsaw")

    private fun w(
        id: String,
        start: String,
        discipline: Discipline,
        minutes: Long,
        distanceM: Double? = null,
        kcal: Double? = null,
        elevation: Double? = null,
    ) = Workout(
        id, discipline, Instant.parse(start), null, Duration.ofMinutes(minutes), null,
        distanceM, kcal, null, null, null, null, elevation,
    )

    private val rides = listOf(
        w("a", "2026-05-02T08:00:00Z", Discipline.ROAD, 120, 50_000.0, 1200.0, 400.0),
        w("b", "2026-06-02T08:00:00Z", Discipline.ROAD, 90, 45_000.0, 900.0, 800.0),
        // Krótki, ale bardzo szybki przejazd: poniżej progu, więc nie bije rekordu prędkości.
        w("c", "2026-07-02T08:00:00Z", Discipline.ROAD, 20, 15_000.0, 300.0, null),
    )

    @Test
    fun longestDistanceAndDurationComeFromTheRightWorkout() {
        val records = rides.recordsFor(Discipline.ROAD, zone)
        val distance = records.single { it.kind == RecordKind.LONGEST_DISTANCE }
        assertEquals("a", distance.workoutId)
        assertEquals(50_000.0, distance.value, 0.1)
        assertEquals(LocalDate.of(2026, 5, 2), distance.date)
        assertEquals("a", records.single { it.kind == RecordKind.LONGEST_DURATION }.workoutId)
    }

    @Test
    fun speedRecordIgnoresShortRides() {
        // c ma 45 km/h, ale tylko 15 km, więc rekordem zostaje b (30 km/h).
        val speed = rides.recordsFor(Discipline.ROAD, zone).single { it.kind == RecordKind.BEST_AVG_SPEED }
        assertEquals("b", speed.workoutId)
        assertEquals(30.0, speed.value, 0.01)
    }

    @Test
    fun poolUsesPaceAndIgnoresShortSwims() {
        val swims = listOf(
            w("s1", "2026-05-02T08:00:00Z", Discipline.POOL, 30, 1500.0),
            w("s2", "2026-05-09T08:00:00Z", Discipline.POOL, 10, 600.0), // szybkie, ale za krótkie
        )
        val records = swims.recordsFor(Discipline.POOL, zone)
        val pace = records.single { it.kind == RecordKind.BEST_PACE }
        assertEquals("s1", pace.workoutId)
        assertTrue(records.none { it.kind == RecordKind.BEST_AVG_SPEED })
    }

    @Test
    fun missingDataMeansNoRecord() {
        val trainer = listOf(w("t", "2026-01-02T08:00:00Z", Discipline.TRAINER, 60))
        val kinds = trainer.recordsFor(Discipline.TRAINER, zone).map { it.kind }
        assertEquals(listOf(RecordKind.LONGEST_DURATION), kinds)
        assertTrue(emptyList<Workout>().recordsFor(Discipline.POOL, zone).isEmpty())
    }

    @Test
    fun bestPeriodFindsStrongestWeek() {
        val best = rides.bestPeriod(Granularity.WEEK, zone, byDistance = true)!!
        assertEquals(LocalDate.of(2026, 4, 27), best.range.start)
        assertEquals(50_000.0, best.distanceM!!, 0.1)
        assertNull(emptyList<Workout>().bestPeriod(Granularity.MONTH, zone, byDistance = true))
    }

    @Test
    fun recordHistoryKeepsOnlyImprovements() {
        val list = listOf(
            w("1", "2026-01-02T08:00:00Z", Discipline.ROAD, 60, 30_000.0),
            w("2", "2026-02-02T08:00:00Z", Discipline.ROAD, 60, 20_000.0), // gorszy, nie wchodzi
            w("3", "2026-03-02T08:00:00Z", Discipline.ROAD, 60, 40_000.0),
        )
        val history = list.recordHistory(RecordKind.LONGEST_DISTANCE, Discipline.ROAD, zone)
        assertEquals(listOf("1", "3"), history.map { it.workoutId })
    }
}
