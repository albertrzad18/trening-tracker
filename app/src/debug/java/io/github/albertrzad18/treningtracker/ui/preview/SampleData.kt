package io.github.albertrzad18.treningtracker.ui.preview

import io.github.albertrzad18.treningtracker.domain.DashboardData
import io.github.albertrzad18.treningtracker.domain.Discipline
import io.github.albertrzad18.treningtracker.domain.Goal
import io.github.albertrzad18.treningtracker.domain.GoalMetric
import io.github.albertrzad18.treningtracker.domain.GoalPeriod
import io.github.albertrzad18.treningtracker.domain.PeriodFilter
import io.github.albertrzad18.treningtracker.domain.RecordKind
import io.github.albertrzad18.treningtracker.domain.RouteTrack
import io.github.albertrzad18.treningtracker.domain.SortColumn
import io.github.albertrzad18.treningtracker.domain.StatsMetric
import io.github.albertrzad18.treningtracker.domain.SummaryGranularity
import io.github.albertrzad18.treningtracker.domain.Workout
import io.github.albertrzad18.treningtracker.domain.activityHeatmap
import io.github.albertrzad18.treningtracker.domain.bestPeriod
import io.github.albertrzad18.treningtracker.domain.bucketize
import io.github.albertrzad18.treningtracker.domain.buildDashboard
import io.github.albertrzad18.treningtracker.domain.buildWorkoutList
import io.github.albertrzad18.treningtracker.domain.byDayOfWeek
import io.github.albertrzad18.treningtracker.domain.byTimeOfDay
import io.github.albertrzad18.treningtracker.domain.compareToAverage
import io.github.albertrzad18.treningtracker.domain.cumulativeDistance
import io.github.albertrzad18.treningtracker.domain.disciplineShares
import io.github.albertrzad18.treningtracker.domain.goalHistory
import io.github.albertrzad18.treningtracker.domain.goalProgress
import io.github.albertrzad18.treningtracker.domain.goalStreak
import io.github.albertrzad18.treningtracker.domain.granularityFor
import io.github.albertrzad18.treningtracker.domain.heartRateZones
import io.github.albertrzad18.treningtracker.domain.recordsFor
import io.github.albertrzad18.treningtracker.domain.statsRange
import io.github.albertrzad18.treningtracker.domain.summaries
import io.github.albertrzad18.treningtracker.domain.timeInZones
import io.github.albertrzad18.treningtracker.domain.trend
import io.github.albertrzad18.treningtracker.domain.TrendMetric
import io.github.albertrzad18.treningtracker.domain.Granularity
import io.github.albertrzad18.treningtracker.domain.SamplePoint
import io.github.albertrzad18.treningtracker.domain.inRange
import io.github.albertrzad18.treningtracker.ui.detail.DetailState
import io.github.albertrzad18.treningtracker.ui.goals.GoalRow
import io.github.albertrzad18.treningtracker.ui.records.RecordsData
import io.github.albertrzad18.treningtracker.ui.stats.StatsData
import io.github.albertrzad18.treningtracker.ui.stats.StatsFilters
import io.github.albertrzad18.treningtracker.ui.workouts.WorkoutFilters
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sztuczne treningi tylko do podglądów `@Preview`. Nie trafiają na żaden ekran w apce,
 * ale dzięki nim widać wszystkie stany (dane, ładowanie, pusto) bez emulatora i bez zegarka.
 */
internal object Sample {
    val zone: ZoneId = ZoneId.of("Europe/Warsaw")
    val today: LocalDate = LocalDate.of(2026, 10, 24)

    private fun workout(
        id: String,
        date: LocalDateTime,
        discipline: Discipline,
        minutes: Long,
        distanceM: Double? = null,
        kcal: Double? = null,
        avgHr: Long? = null,
        maxHr: Long? = null,
        speedMps: Double? = null,
        maxSpeedMps: Double? = null,
        elevation: Double? = null,
    ) = Workout(
        id = id,
        discipline = discipline,
        start = date.toInstant(ZoneOffset.ofHours(2)),
        zoneOffset = ZoneOffset.ofHours(2),
        duration = Duration.ofMinutes(minutes),
        title = null,
        distanceM = distanceM,
        caloriesKcal = kcal,
        avgHeartRate = avgHr,
        maxHeartRate = maxHr,
        avgSpeedMps = speedMps,
        maxSpeedMps = maxSpeedMps,
        elevationGainM = elevation,
    )

    /** Osiemnaście miesięcy treningów: basen cały rok, rower latem, trenażer zimą. */
    val workouts: List<Workout> = buildList {
        val random = Random(7)
        var date = LocalDate.of(2025, 5, 1)
        var index = 0
        while (!date.isAfter(today)) {
            val summer = date.monthValue in 4..9
            if (date.dayOfWeek.value % 3 == 0) {
                val minutes = 30L + random.nextInt(30)
                add(
                    workout(
                        "p$index",
                        date.atTime(18, 54),
                        Discipline.POOL,
                        minutes,
                        distanceM = 1500.0 + random.nextInt(1800),
                        kcal = 260.0 + random.nextInt(400),
                        avgHr = 125L + random.nextInt(15),
                        maxHr = 150L + random.nextInt(10),
                    ),
                )
            }
            if (summer && date.dayOfWeek.value == 6) {
                val minutes = 60L + random.nextInt(120)
                add(
                    workout(
                        "r$index",
                        date.atTime(9, 43),
                        Discipline.ROAD,
                        minutes,
                        distanceM = minutes * 450.0,
                        kcal = minutes * 11.0,
                        avgHr = 132L + random.nextInt(10),
                        maxHr = 150L + random.nextInt(12),
                        speedMps = 7.0 + random.nextDouble(),
                        maxSpeedMps = 10.0 + random.nextDouble(2.0),
                        elevation = 120.0 + random.nextInt(800),
                    ),
                )
            }
            if (!summer && date.dayOfWeek.value == 2) {
                val minutes = 45L + random.nextInt(70)
                add(
                    workout(
                        "t$index",
                        date.atTime(19, 7),
                        Discipline.TRAINER,
                        minutes,
                        kcal = minutes * 9.0,
                        avgHr = 138L + random.nextInt(12),
                        maxHr = 155L + random.nextInt(8),
                    ),
                )
            }
            date = date.plusDays(1)
            index++
        }
        // Ostatni tydzień zawsze z treningami, żeby Pulpit miał co pokazać.
        add(workout("last-pool", today.minusDays(2).atTime(18, 54), Discipline.POOL, 38, 2000.0, 304.0, 128, 141))
        add(workout("last-trainer", today.minusDays(4).atTime(19, 7), Discipline.TRAINER, 51, null, 459.0, 141, 158))
        add(workout("last-pool-2", today.minusDays(5).atTime(18, 56), Discipline.POOL, 33, 1700.0, 264.0, 132, 145))
    }.sortedBy { it.start }

    val dashboard: DashboardData = buildDashboard(workouts, today, zone)

    val workoutList = buildWorkoutList(
        all = workouts,
        disciplines = emptySet(),
        period = PeriodFilter.ALL,
        sort = SortColumn.DATE,
        ascending = false,
        today = today,
        zone = zone,
    )

    val workoutFilters = WorkoutFilters()

    val statsFilters = StatsFilters(period = PeriodFilter.YEAR, metric = StatsMetric.TIME)

    val stats: StatsData = run {
        val range = statsRange(workouts, statsFilters.period, today, zone)
        val inPeriod = workouts.inRange(range, zone)
        StatsData(
            isEmpty = false,
            buckets = inPeriod.bucketize(range, granularityFor(statsFilters.period), zone),
            granularity = granularityFor(statsFilters.period),
            shownDisciplines = Discipline.entries.toList(),
            heatmap = workouts.activityHeatmap(today, zone),
            shares = inPeriod.disciplineShares(),
            byDayOfWeek = inPeriod.byDayOfWeek(zone),
            byTimeOfDay = inPeriod.byTimeOfDay(zone),
            paceTrends = Discipline.entries.associateWith { workouts.trend(it, TrendMetric.SPEED_OR_PACE, zone) },
            efficiencyTrends = Discipline.entries.associateWith { workouts.trend(it, TrendMetric.EFFICIENCY, zone) },
            thisYear = today.year,
            cumulativeThisYear = workouts.cumulativeDistance(today.year, zone, today),
            cumulativePreviousYear = workouts.cumulativeDistance(today.year - 1, zone, today),
        )
    }

    val emptyStats: StatsData = stats.copy(
        isEmpty = true,
        buckets = emptyList(),
        shares = emptyList(),
    )

    val summaries = summaries(workouts, SummaryGranularity.WEEK, today, zone)

    val records = RecordsData(
        perDiscipline = Discipline.entries.associateWith { workouts.recordsFor(it, zone) },
        bestWeekDistance = workouts.bestPeriod(Granularity.WEEK, zone, byDistance = true),
        bestWeekDuration = workouts.bestPeriod(Granularity.WEEK, zone, byDistance = false),
        bestMonthDistance = workouts.bestPeriod(Granularity.MONTH, zone, byDistance = true),
        bestMonthDuration = workouts.bestPeriod(Granularity.MONTH, zone, byDistance = false),
        isEmpty = false,
    )

    val goalRows: List<GoalRow> = listOf(
        Goal(1, Discipline.ROAD, GoalPeriod.MONTH, GoalMetric.DISTANCE, 300_000.0),
        Goal(2, null, GoalPeriod.WEEK, GoalMetric.COUNT, 3.0),
    ).map { goal ->
        val history = goalHistory(goal, workouts, today, zone)
        GoalRow(goalProgress(goal, workouts, today, zone), history, goalStreak(history))
    }

    /** Rower z pełnymi danymi: przebieg tętna, prędkości i strefy. */
    val detail: DetailState.Found = run {
        val workout = workouts.last { it.discipline == Discipline.ROAD }
        val start = workout.start.toEpochMilli()
        val heartRate = (0..120).map { minute ->
            SamplePoint(start + minute * 60_000L, 128.0 + 14 * sin(minute / 9.0) + minute * 0.05)
        }
        val speed = (0..120).map { minute ->
            SamplePoint(start + minute * 60_000L, 7.2 + 1.6 * sin(minute / 6.0))
        }
        val zones = heartRateZones(190)
        DetailState.Found(
            workout = workout,
            comparison = compareToAverage(workout, workouts),
            heartRate = heartRate,
            speed = speed,
            zones = zones,
            timeInZones = timeInZones(heartRate, zones),
            route = RouteTrack(emptyList()),
        )
    }

    val recordKinds = RecordKind.entries
}
