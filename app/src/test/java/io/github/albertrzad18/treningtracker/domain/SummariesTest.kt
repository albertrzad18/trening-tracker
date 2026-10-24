package io.github.albertrzad18.treningtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SummariesTest {
    private val zone = ZoneId.of("Europe/Warsaw")
    private val today = LocalDate.of(2026, 10, 24) // sobota

    private fun w(
        id: String,
        start: String,
        discipline: Discipline = Discipline.POOL,
        minutes: Long = 60,
        distanceM: Double? = null,
        kcal: Double? = null,
    ) = Workout(
        id, discipline, Instant.parse(start), null, Duration.ofMinutes(minutes), null,
        distanceM, kcal, null, null, null, null, null,
    )

    @Test
    fun weekSummaryComparesWithPreviousWeek() {
        val list = listOf(
            w("a", "2026-10-20T10:00:00Z", minutes = 60, distanceM = 2000.0, kcal = 500.0),
            w("b", "2026-10-22T10:00:00Z", minutes = 60, distanceM = 1000.0, kcal = 300.0),
            w("c", "2026-10-14T10:00:00Z", minutes = 60, distanceM = 1500.0, kcal = 400.0),
        )
        val week = summaries(list, SummaryGranularity.WEEK, today, zone).first()
        assertEquals(LocalDate.of(2026, 10, 19), week.range.start)
        assertEquals(2, week.count)
        assertEquals(2, week.activeDays)
        assertEquals(100.0, week.durationChangePercent!!, 0.001) // 120 min vs 60 min
        assertEquals(100.0, week.distanceChangePercent!!, 0.001) // 3000 m vs 1500 m
        assertEquals(100.0, week.caloriesChangePercent!!, 0.001)
    }

    @Test
    fun summariesStartWithCurrentPeriodEvenWhenEmpty() {
        val list = listOf(w("old", "2026-08-10T10:00:00Z"))
        val weeks = summaries(list, SummaryGranularity.WEEK, today, zone)
        assertEquals(LocalDate.of(2026, 10, 19), weeks.first().range.start)
        assertEquals(0, weeks.first().count)
        // Puste tygodnie po bieżącym są pomijane, zostaje tylko ten z treningiem.
        assertEquals(2, weeks.size)
        assertEquals(1, weeks[1].count)
    }

    @Test
    fun summariesAreNewestFirstAndLimited() {
        val list = (1..10).map { w("w$it", "2026-%02d-10T10:00:00Z".format(it)) }
        val months = summaries(list, SummaryGranularity.MONTH, today, zone, limit = 3)
        assertEquals(3, months.size)
        assertEquals(LocalDate.of(2026, 10, 1), months[0].range.start)
        assertEquals(LocalDate.of(2026, 9, 1), months[1].range.start)
        assertEquals(LocalDate.of(2026, 8, 1), months[2].range.start)
    }

    @Test
    fun yearSummarySplitsDisciplines() {
        val list = listOf(
            w("a", "2026-03-10T10:00:00Z", Discipline.POOL, 30),
            w("b", "2026-04-10T10:00:00Z", Discipline.TRAINER, 90),
        )
        val year = summaries(list, SummaryGranularity.YEAR, today, zone).first()
        assertEquals(LocalDate.of(2026, 1, 1), year.range.start)
        assertEquals(3, year.perDiscipline.size)
        assertEquals(30, year.perDiscipline.first { it.discipline == Discipline.POOL }.totals.duration.toMinutes())
        assertEquals(0, year.perDiscipline.first { it.discipline == Discipline.ROAD }.totals.count)
    }

    @Test
    fun longestStreakCountsConsecutiveDays() {
        val list = listOf(
            w("a", "2026-10-19T10:00:00Z"),
            w("b", "2026-10-20T10:00:00Z"),
            w("c", "2026-10-20T18:00:00Z"), // ten sam dzień, nie wydłuża serii
            w("d", "2026-10-21T10:00:00Z"),
            w("e", "2026-10-23T10:00:00Z"),
        )
        assertEquals(3, list.longestStreak(zone))
        assertEquals(0, emptyList<Workout>().longestStreak(zone))
    }

    @Test
    fun bestWorkoutIsLongest() {
        val list = listOf(
            w("a", "2026-10-19T10:00:00Z", minutes = 30),
            w("b", "2026-10-20T10:00:00Z", minutes = 90),
        )
        assertEquals("b", list.bestWorkout()!!.id)
        assertNull(emptyList<Workout>().bestWorkout())
    }

    @Test
    fun changeIsNullWithoutPreviousData() {
        val list = listOf(w("a", "2026-10-20T10:00:00Z", distanceM = 1000.0))
        val week = summaries(list, SummaryGranularity.WEEK, today, zone).first()
        assertNull(week.durationChangePercent)
        assertNull(week.distanceChangePercent)
        assertTrue(week.totals.count == 1)
    }
}
