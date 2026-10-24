package io.github.albertrzad18.treningtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class ScreensLogicTest {
    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 10, 24) // sobota

    private fun w(
        id: String,
        start: String,
        discipline: Discipline = Discipline.POOL,
        minutes: Long = 60,
        distanceM: Double? = null,
        kcal: Double? = null,
        hr: Long? = null,
    ) = Workout(
        id, discipline, Instant.parse(start), null, Duration.ofMinutes(minutes), null,
        distanceM, kcal, hr, null, null, null, null,
    )

    private val sample = listOf(
        w("a", "2026-10-20T10:00:00Z", Discipline.POOL, 60, 2000.0, 500.0, 120),
        w("b", "2026-10-22T10:00:00Z", Discipline.TRAINER, 45, null, 400.0, 140),
        w("c", "2026-10-14T10:00:00Z", Discipline.POOL, 30, 1000.0, 250.0, 130),
        w("d", "2026-09-30T10:00:00Z", Discipline.TRAINER, 90, null, 800.0, null),
    )

    @Test
    fun dashboardSplitsCurrentAndPreviousWeek() {
        val d = buildDashboard(sample, today, zone)
        assertEquals(2, d.week.count)
        assertEquals(1, d.previousWeek.count)
        // 105 min vs 30 min = +250%
        assertEquals(250.0, d.durationChangePercent!!, 0.001)
        assertEquals(listOf("b", "a", "c"), d.latest.map { it.id })
    }

    @Test
    fun dashboardOnEmptyData() {
        val d = buildDashboard(emptyList(), today, zone)
        assertTrue(d.isEmpty)
        assertNull(d.durationChangePercent)
        assertEquals(3, d.perDiscipline.size)
    }

    @Test
    fun dashboardPerDisciplineHidesMissingDistance() {
        val trainer = buildDashboard(sample, today, zone).perDiscipline.first { it.discipline == Discipline.TRAINER }
        assertEquals(1, trainer.totals.count)
        assertNull(trainer.totals.distanceM)
    }

    @Test
    fun listFiltersByDisciplineAndPeriod() {
        val r = buildWorkoutList(sample, setOf(Discipline.POOL), PeriodFilter.MONTH, SortColumn.DATE, false, today, zone)
        assertEquals(listOf("a", "c"), r.workouts.map { it.id })
        val week = buildWorkoutList(sample, emptySet(), PeriodFilter.WEEK, SortColumn.DATE, false, today, zone)
        assertEquals(listOf("b", "a"), week.workouts.map { it.id })
    }

    @Test
    fun listGroupsByMonthOnlyWhenSortedByDate() {
        val byDate = buildWorkoutList(sample, emptySet(), PeriodFilter.ALL, SortColumn.DATE, false, today, zone)
        assertEquals(listOf(YearMonth.of(2026, 10), YearMonth.of(2026, 9)), byDate.groups.map { it.month })
        assertEquals(3, byDate.groups[0].totals.count) // 3 treningi w październiku
        val byDuration = buildWorkoutList(sample, emptySet(), PeriodFilter.ALL, SortColumn.DURATION, false, today, zone)
        assertTrue(byDuration.groups.isEmpty())
        assertEquals(listOf("d", "a", "b", "c"), byDuration.workouts.map { it.id })
    }

    @Test
    fun sortPutsMissingValuesLastInBothDirections() {
        val desc = buildWorkoutList(sample, emptySet(), PeriodFilter.ALL, SortColumn.HEART_RATE, false, today, zone)
        assertEquals("d", desc.workouts.last().id)
        val asc = buildWorkoutList(sample, emptySet(), PeriodFilter.ALL, SortColumn.HEART_RATE, true, today, zone)
        assertEquals("d", asc.workouts.last().id)
        assertEquals("a", asc.workouts.first().id)
    }

    @Test
    fun listSummaryHasTotalsAndAverage() {
        val r = buildWorkoutList(sample, emptySet(), PeriodFilter.ALL, SortColumn.DATE, false, today, zone)
        assertEquals(4, r.totals.count)
        assertEquals(Duration.ofSeconds(3375), r.averageDuration) // 225 min / 4 = 56 min 15 s
        assertEquals(3000.0, r.totals.distanceM!!, 0.001)
    }

    @Test
    fun emptyListHasNoAverage() {
        val r = buildWorkoutList(emptyList(), emptySet(), PeriodFilter.ALL, SortColumn.DATE, false, today, zone)
        assertNull(r.averageDuration)
        assertEquals(0, r.totals.count)
    }

    @Test
    fun comparisonUsesOtherWorkoutsOfSameDiscipline() {
        val c = compareToAverage(sample[0], sample)!!
        // inne baseny: tylko "c" (30 min, 1000 m); a: 60 min, 2000 m
        assertEquals(100.0, c.durationChangePercent!!, 0.001)
        assertEquals(100.0, c.distanceChangePercent!!, 0.001)
    }

    @Test
    fun comparisonIsNullWithoutOtherWorkouts() {
        assertNull(compareToAverage(sample[0], listOf(sample[0])))
        assertNotNull(compareToAverage(sample[1], sample))
        assertNull(compareToAverage(sample[1], sample)!!.distanceChangePercent)
    }
}
