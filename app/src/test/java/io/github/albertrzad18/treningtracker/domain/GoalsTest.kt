package io.github.albertrzad18.treningtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class GoalsTest {
    private val zone = ZoneId.of("Europe/Warsaw")
    private val today = LocalDate.of(2026, 10, 10) // 10 z 31 dni października

    private fun w(id: String, start: String, discipline: Discipline, minutes: Long, distanceM: Double? = null) =
        Workout(id, discipline, Instant.parse(start), null, Duration.ofMinutes(minutes), null, distanceM, null, null, null, null, null, null)

    private val goal = Goal(1, Discipline.ROAD, GoalPeriod.MONTH, GoalMetric.DISTANCE, 100_000.0)

    @Test
    fun progressCountsOnlyThisMonthAndDiscipline() {
        val list = listOf(
            w("a", "2026-10-02T08:00:00Z", Discipline.ROAD, 60, 30_000.0),
            w("b", "2026-09-28T08:00:00Z", Discipline.ROAD, 60, 50_000.0), // poprzedni miesiąc
            w("c", "2026-10-03T08:00:00Z", Discipline.POOL, 60, 2_000.0), // inna dyscyplina
        )
        val progress = goalProgress(goal, list, today, zone)
        assertEquals(30_000.0, progress.current, 0.1)
        assertEquals(0.3, progress.fraction, 0.001)
        assertFalse(progress.done)
        assertEquals(21, progress.daysLeft)
    }

    @Test
    fun projectionScalesCurrentPace() {
        val list = listOf(w("a", "2026-10-02T08:00:00Z", Discipline.ROAD, 60, 30_000.0))
        // 30 km przez 10 dni to 3 km dziennie, czyli 93 km w 31 dni.
        assertEquals(93_000.0, goalProgress(goal, list, today, zone).projected!!, 1.0)
    }

    @Test
    fun emptyPeriodHasNoProjection() {
        val progress = goalProgress(goal, emptyList(), today, zone)
        assertNull(progress.projected)
        assertEquals(0.0, progress.fraction, 0.001)
    }

    @Test
    fun goalWithoutDisciplineCountsEverything() {
        val all = Goal(2, null, GoalPeriod.WEEK, GoalMetric.COUNT, 3.0)
        val list = listOf(
            w("a", "2026-10-05T08:00:00Z", Discipline.ROAD, 60),
            w("b", "2026-10-06T08:00:00Z", Discipline.POOL, 30),
            w("c", "2026-10-07T08:00:00Z", Discipline.TRAINER, 45),
        )
        val progress = goalProgress(all, list, today, zone)
        assertEquals(3.0, progress.current, 0.001)
        assertTrue(progress.done)
    }

    @Test
    fun durationGoalCountsSeconds() {
        val timeGoal = Goal(3, null, GoalPeriod.WEEK, GoalMetric.DURATION, 2 * 3600.0)
        val list = listOf(w("a", "2026-10-05T08:00:00Z", Discipline.POOL, 90))
        assertEquals(5400.0, goalProgress(timeGoal, list, today, zone).current, 0.1)
    }

    @Test
    fun historyAndStreakLookAtClosedPeriods() {
        val list = listOf(
            w("sep", "2026-09-10T08:00:00Z", Discipline.ROAD, 60, 120_000.0),
            w("aug", "2026-08-10T08:00:00Z", Discipline.ROAD, 60, 120_000.0),
            w("jul", "2026-07-10T08:00:00Z", Discipline.ROAD, 60, 10_000.0), // za mało
        )
        val history = goalHistory(goal, list, today, zone, count = 3)
        assertEquals(listOf(true, true, false), history.map { it.second })
        assertEquals(2, goalStreak(history))
    }
}
