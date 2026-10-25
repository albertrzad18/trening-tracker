package io.github.albertrzad18.treningtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StatsTest {
    private val zone = ZoneId.of("Europe/Warsaw")
    private val today = LocalDate.of(2026, 10, 24) // sobota

    private fun w(
        id: String,
        start: String,
        discipline: Discipline = Discipline.POOL,
        minutes: Long = 60,
        distanceM: Double? = null,
        kcal: Double? = null,
        hr: Long? = null,
        speedMps: Double? = null,
    ) = Workout(
        id, discipline, Instant.parse(start), null, Duration.ofMinutes(minutes), null,
        distanceM, kcal, hr, null, speedMps, null, null,
    )

    @Test
    fun bucketizeKeepsEmptyDays() {
        val list = listOf(w("a", "2026-10-20T10:00:00Z", minutes = 30))
        val buckets = list.bucketize(DateRange(today.minusDays(6), today.plusDays(1)), Granularity.DAY, zone)
        assertEquals(7, buckets.size)
        assertEquals(today.minusDays(6), buckets.first().start)
        assertEquals(today, buckets.last().start)
        assertEquals(1, buckets.count { it.totals.count == 1 })
        assertEquals(30.0, buckets.first { it.totals.count == 1 }.totals.metricValue(StatsMetric.TIME), 0.001)
    }

    @Test
    fun bucketizeGroupsByMonth() {
        val list = listOf(
            w("a", "2026-09-05T10:00:00Z", minutes = 60),
            w("b", "2026-09-25T10:00:00Z", minutes = 30),
            w("c", "2026-10-01T10:00:00Z", minutes = 45),
        )
        val buckets = list.bucketize(DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 1)), Granularity.MONTH, zone)
        assertEquals(2, buckets.size)
        assertEquals(90, buckets[0].totals.duration.toMinutes())
        assertEquals(45, buckets[1].totals.duration.toMinutes())
    }

    @Test
    fun bucketSplitsDisciplines() {
        val list = listOf(
            w("a", "2026-10-20T10:00:00Z", Discipline.POOL, 30),
            w("b", "2026-10-20T18:00:00Z", Discipline.TRAINER, 60),
        )
        val bucket = list.bucketize(DateRange(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 21)), Granularity.DAY, zone).single()
        assertEquals(30, bucket.perDiscipline[Discipline.POOL]!!.duration.toMinutes())
        assertEquals(60, bucket.perDiscipline[Discipline.TRAINER]!!.duration.toMinutes())
        assertEquals(0, bucket.perDiscipline[Discipline.ROAD]!!.duration.toMinutes())
    }

    @Test
    fun metricValueUsesZeroForMissingData() {
        val trainer = listOf(w("a", "2026-10-20T10:00:00Z", Discipline.TRAINER, 60, distanceM = null)).totals()
        assertEquals(0.0, trainer.metricValue(StatsMetric.DISTANCE), 0.001)
        assertEquals(60.0, trainer.metricValue(StatsMetric.TIME), 0.001)
    }

    @Test
    fun statsRangeForAllStartsAtFirstWorkout() {
        val list = listOf(w("a", "2026-03-15T10:00:00Z"))
        val range = statsRange(list, PeriodFilter.ALL, today, zone)
        assertEquals(LocalDate.of(2026, 3, 15), range.start)
        assertEquals(today.plusDays(1), range.endExclusive)
    }

    @Test
    fun statsRangeForAllWithoutWorkoutsIsToday() {
        val range = statsRange(emptyList(), PeriodFilter.ALL, today, zone)
        assertEquals(today, range.start)
    }

    @Test
    fun heatmapHasFullWeeksAndCountsMinutes() {
        val list = listOf(w("a", "2026-10-20T10:00:00Z", minutes = 75))
        val data = list.activityHeatmap(today, zone, weeks = 4)
        assertEquals(4, data.weeks.size)
        assertTrue(data.weeks.all { it.size == 7 })
        assertEquals(DayOfWeek.MONDAY, data.weeks.first().first().date.dayOfWeek)
        assertEquals(75, data.maxMinutes)
        val tuesday = data.weeks.last()[1]
        assertEquals(LocalDate.of(2026, 10, 20), tuesday.date)
        assertEquals(75, tuesday.minutes)
        assertEquals(Discipline.POOL, tuesday.dominant)
        assertEquals(1, data.activeDays)
    }

    @Test
    fun heatmapIgnoresFutureDays() {
        val data = emptyList<Workout>().activityHeatmap(today, zone, weeks = 1)
        assertTrue(data.isEmpty)
        assertEquals(7, data.weeks.single().size)
    }

    @Test
    fun trendUsesPaceForPoolAndSpeedForBike() {
        val pool = listOf(w("a", "2026-10-20T10:00:00Z", Discipline.POOL, 30, distanceM = 1500.0))
        // 30 min na 1500 m = 120 s / 100 m
        assertEquals(120.0, pool.trend(Discipline.POOL, TrendMetric.SPEED_OR_PACE, zone).single().value, 0.01)

        val road = listOf(w("b", "2026-10-20T10:00:00Z", Discipline.ROAD, 60, distanceM = 30000.0))
        assertEquals(30.0, road.trend(Discipline.ROAD, TrendMetric.SPEED_OR_PACE, zone).single().value, 0.01)
    }

    @Test
    fun trendSkipsWorkoutsWithoutData() {
        val list = listOf(
            w("a", "2026-10-18T10:00:00Z", Discipline.TRAINER, 60, distanceM = 20000.0, hr = 100),
            w("b", "2026-10-20T10:00:00Z", Discipline.TRAINER, 60, distanceM = null, hr = 150),
        )
        assertEquals(1, list.trend(Discipline.TRAINER, TrendMetric.SPEED_OR_PACE, zone).size)
        // 20 km/h przy tętnie 100 = 20 km/h na 100 uderzeń
        assertEquals(20.0, list.trend(Discipline.TRAINER, TrendMetric.EFFICIENCY, zone).single().value, 0.01)
    }

    @Test
    fun movingAverageSmoothsValues() {
        assertEquals(listOf(10.0, 15.0, 25.0, 35.0), movingAverage(listOf(10.0, 20.0, 30.0, 40.0), 2))
        assertEquals(listOf(10.0, 15.0, 20.0), movingAverage(listOf(10.0, 20.0, 30.0), 5))
    }

    @Test
    fun cumulativeDistanceGrowsAndEndsToday() {
        val list = listOf(
            w("a", "2026-01-10T10:00:00Z", Discipline.ROAD, 60, distanceM = 20000.0),
            w("b", "2026-01-10T15:00:00Z", Discipline.ROAD, 60, distanceM = 10000.0),
            w("c", "2026-02-01T10:00:00Z", Discipline.ROAD, 60, distanceM = 5000.0),
            w("d", "2025-05-01T10:00:00Z", Discipline.ROAD, 60, distanceM = 99000.0),
        )
        val points = list.cumulativeDistance(2026, zone, today)
        assertEquals(1, points.first().dayOfYear)
        assertEquals(0.0, points.first().km, 0.001)
        assertEquals(30.0, points[1].km, 0.001)
        assertEquals(35.0, points[2].km, 0.001)
        assertEquals(today.dayOfYear, points.last().dayOfYear)
        assertEquals(35.0, points.last().km, 0.001)
    }

    @Test
    fun cumulativeDistanceIsEmptyWithoutDistance() {
        val list = listOf(w("a", "2026-01-10T10:00:00Z", Discipline.TRAINER, 60, distanceM = null))
        assertTrue(list.cumulativeDistance(2026, zone, today).isEmpty())
    }

    @Test
    fun disciplineSharesSumToOne() {
        val list = listOf(
            w("a", "2026-10-20T10:00:00Z", Discipline.POOL, 30),
            w("b", "2026-10-21T10:00:00Z", Discipline.TRAINER, 90),
        )
        val shares = list.disciplineShares()
        assertEquals(2, shares.size)
        assertEquals(0.25, shares.first { it.discipline == Discipline.POOL }.fraction, 0.001)
        assertEquals(1.0, shares.sumOf { it.fraction }, 0.001)
    }

    @Test
    fun byDayOfWeekHasAllDays() {
        val list = listOf(w("a", "2026-10-20T10:00:00Z", minutes = 60)) // wtorek w strefie Warszawy
        val byDay = list.byDayOfWeek(zone)
        assertEquals(7, byDay.size)
        assertEquals(1, byDay[DayOfWeek.TUESDAY]!!.count)
        assertEquals(0, byDay[DayOfWeek.MONDAY]!!.count)
    }

    @Test
    fun byTimeOfDayUsesLocalHour() {
        // 21:30 UTC to 23:30 w Warszawie (czas letni), więc przedział 20–24
        val list = listOf(w("a", "2026-07-20T21:30:00Z", minutes = 30))
        val bins = list.byTimeOfDay(zone)
        assertEquals(6, bins.size)
        assertEquals(20, bins.single { it.totals.count > 0 }.fromHour)
        assertEquals(24, bins.last().toHour)
    }
}
