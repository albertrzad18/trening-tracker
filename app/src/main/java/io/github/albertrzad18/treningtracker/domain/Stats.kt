package io.github.albertrzad18.treningtracker.domain

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

/** Co mierzymy na wykresie objętości: czas (minuty), dystans (km) albo kalorie. */
enum class StatsMetric { TIME, DISTANCE, CALORIES }

/** Szerokość jednego słupka na wykresie. */
enum class Granularity { DAY, WEEK, MONTH }

/** Jaka szerokość słupka pasuje do wybranego okresu. */
fun granularityFor(period: PeriodFilter): Granularity = when (period) {
    PeriodFilter.WEEK -> Granularity.DAY
    PeriodFilter.MONTH -> Granularity.DAY
    PeriodFilter.YEAR -> Granularity.MONTH
    PeriodFilter.ALL -> Granularity.MONTH
}

/** Jeden słupek: przedział czasu z sumami, także w podziale na dyscypliny. Pusty przedział też tu jest (dziura na wykresie). */
data class Bucket(
    val start: LocalDate,
    val granularity: Granularity,
    val totals: Totals,
    val perDiscipline: Map<Discipline, Totals>,
)

/** Wartość słupka w jednostce wykresu: minuty, kilometry albo kcal. Brak danych to 0. */
fun Totals.metricValue(metric: StatsMetric): Double = when (metric) {
    StatsMetric.TIME -> duration.seconds / 60.0
    StatsMetric.DISTANCE -> (distanceM ?: 0.0) / 1000.0
    StatsMetric.CALORIES -> caloriesKcal ?: 0.0
}

/** Początek przedziału, w którym leży [day]: ten dzień, poniedziałek tygodnia albo pierwszy dzień miesiąca. */
fun Granularity.startOf(day: LocalDate): LocalDate = when (this) {
    Granularity.DAY -> day
    Granularity.WEEK -> weekOf(day).start
    Granularity.MONTH -> day.withDayOfMonth(1)
}

fun Granularity.next(start: LocalDate): LocalDate = when (this) {
    Granularity.DAY -> start.plusDays(1)
    Granularity.WEEK -> start.plusWeeks(1)
    Granularity.MONTH -> start.plusMonths(1)
}

/**
 * Zakres do wykresów dla wybranego okresu. Dla [PeriodFilter.ALL] zaczynamy od pierwszego treningu
 * (albo od dziś, gdy nie ma żadnego) i kończymy na dniu po dziś, żeby bieżący dzień też się policzył.
 */
fun statsRange(workouts: List<Workout>, period: PeriodFilter, today: LocalDate, zone: ZoneId): DateRange {
    periodRange(period, today)?.let { return it }
    val first = workouts.minByOrNull { it.start }?.startLocal(zone)?.toLocalDate() ?: today
    return DateRange(first, today.plusDays(1))
}

/**
 * Dzieli treningi na kolejne przedziały pokrywające cały [range], także te bez treningów.
 * Przedziały są ułożone od najstarszego, więc indeks na osi X rośnie w prawo.
 */
fun List<Workout>.bucketize(range: DateRange, granularity: Granularity, zone: ZoneId): List<Bucket> {
    val byStart = groupBy { granularity.startOf(it.startLocal(zone).toLocalDate()) }
    val buckets = mutableListOf<Bucket>()
    var cursor = granularity.startOf(range.start)
    while (cursor.isBefore(range.endExclusive)) {
        val group = byStart[cursor].orEmpty()
        buckets += Bucket(
            start = cursor,
            granularity = granularity,
            totals = group.totals(),
            perDiscipline = Discipline.entries.associateWith { d -> group.filter { it.discipline == d }.totals() },
        )
        cursor = granularity.next(cursor)
    }
    return buckets
}

// --- Kalendarz aktywności (heatmapa) ---

/**
 * Jeden dzień heatmapy: ile minut treningu i która dyscyplina go zdominowała.
 * Przy dwóch treningach w jednym dniu kolor bierzemy z tego dłuższego.
 */
data class HeatmapDay(val date: LocalDate, val minutes: Int, val dominant: Discipline?)

/** Kolumny heatmapy to tygodnie (poniedziałek–niedziela), wiersze to dni tygodnia. */
data class HeatmapData(val weeks: List<List<HeatmapDay>>, val maxMinutes: Int) {
    val isEmpty: Boolean get() = maxMinutes == 0

    /** Ile dni z treningiem w całej siatce: kluczowa liczba nad kalendarzem. */
    val activeDays: Int get() = weeks.sumOf { week -> week.count { it.minutes > 0 } }
}

/** Ostatnie [weeks] tygodni, zawsze pełne kolumny od poniedziałku: siatka jak na GitHubie. */
fun List<Workout>.activityHeatmap(today: LocalDate, zone: ZoneId, weeks: Int = 26): HeatmapData {
    val byDay = groupBy { it.startLocal(zone).toLocalDate() }
    val lastMonday = weekOf(today).start
    val firstMonday = lastMonday.minusWeeks((weeks - 1).toLong())
    val columns = (0 until weeks).map { w ->
        val monday = firstMonday.plusWeeks(w.toLong())
        (0..6).map { d ->
            val date = monday.plusDays(d.toLong())
            val group = byDay[date].orEmpty()
            HeatmapDay(
                date = date,
                minutes = if (date.isAfter(today)) 0 else (group.sumOf { it.duration.seconds } / 60).toInt(),
                // Kolor dnia bierzemy z dyscypliny, która zajęła najwięcej czasu.
                dominant = group
                    .groupBy { it.discipline }
                    .maxByOrNull { (_, list) -> list.sumOf { it.duration.seconds } }
                    ?.key,
            )
        }
    }
    return HeatmapData(columns, columns.flatten().maxOfOrNull { it.minutes } ?: 0)
}

// --- Trend formy ---

/** Co pokazuje wykres trendu. */
enum class TrendMetric {
    /** Tempo /100 m dla basenu, prędkość km/h dla roweru i trenażera. */
    SPEED_OR_PACE,

    /** Km/h na 100 uderzeń tętna: rośnie, gdy przy tym samym tętnie jedziesz szybciej. */
    EFFICIENCY,
}

/** Punkt trendu: pojedynczy trening i średnia krocząca liczona z ostatnich kilku treningów. */
data class TrendPoint(val date: LocalDate, val value: Double, val average: Double)

/** Średnia krocząca z ostatnich [window] wartości (na początku listy liczona z tego, co już jest). */
fun movingAverage(values: List<Double>, window: Int): List<Double> = values.indices.map { i ->
    val from = (i - window + 1).coerceAtLeast(0)
    values.subList(from, i + 1).average()
}

/** Wartość trendu dla jednego treningu; `null`, gdy brakuje danych (trening wypada z wykresu). */
private fun Workout.trendValue(metric: TrendMetric): Double? = when (metric) {
    TrendMetric.SPEED_OR_PACE -> if (discipline == Discipline.POOL) pacePer100mSec else avgSpeedKmh
    TrendMetric.EFFICIENCY -> {
        val hr = avgHeartRate
        val speed = avgSpeedKmh
        if (hr != null && hr > 0 && speed != null) speed / hr * 100.0 else null
    }
}

/** Treningi jednej dyscypliny od najstarszego, z wartością metryki i średnią kroczącą. */
fun List<Workout>.trend(
    discipline: Discipline,
    metric: TrendMetric,
    zone: ZoneId,
    window: Int = 5,
): List<TrendPoint> {
    val points = filter { it.discipline == discipline }
        .sortedBy { it.start }
        .mapNotNull { w -> w.trendValue(metric)?.let { w.startLocal(zone).toLocalDate() to it } }
    val averages = movingAverage(points.map { it.second }, window)
    return points.mapIndexed { i, (date, value) -> TrendPoint(date, value, averages[i]) }
}

// --- Dystans narastająco w roku ---

/** Punkt wykresu narastającego: numer dnia w roku i suma kilometrów od 1 stycznia. */
data class CumulativePoint(val dayOfYear: Int, val km: Double)

/**
 * Dystans narastająco w roku [year]: punkt na każdy dzień z treningiem, plus 1 stycznia (0 km).
 * Dla bieżącego roku linia kończy się na [today], żeby nie udawać, że rok już minął.
 */
fun List<Workout>.cumulativeDistance(year: Int, zone: ZoneId, today: LocalDate): List<CumulativePoint> {
    val days = mapNotNull { w -> w.distanceM?.let { w.startLocal(zone).toLocalDate() to it } }
        .filter { (date, _) -> date.year == year }
        .groupBy({ it.first }, { it.second })
        .toSortedMap()
    if (days.isEmpty()) return emptyList()
    val points = mutableListOf(CumulativePoint(1, 0.0))
    var sum = 0.0
    days.forEach { (date, meters) ->
        sum += meters.sum()
        points += CumulativePoint(date.dayOfYear, sum / 1000.0)
    }
    val lastDay = if (year == today.year) today.dayOfYear else LocalDate.of(year, 12, 31).dayOfYear
    if (points.last().dayOfYear < lastDay) points += CumulativePoint(lastDay, sum / 1000.0)
    return points
}

// --- Udział dyscyplin ---

/** Udział dyscypliny w łącznym czasie treningów; [fraction] z zakresu 0..1. */
data class DisciplineShare(val discipline: Discipline, val duration: Duration, val fraction: Double)

/** Tylko dyscypliny, w których coś było; puste, gdy nie ma żadnego treningu. */
fun List<Workout>.disciplineShares(): List<DisciplineShare> {
    val total = sumOf { it.duration.seconds }
    if (total == 0L) return emptyList()
    return Discipline.entries.mapNotNull { d ->
        val seconds = filter { it.discipline == d }.sumOf { it.duration.seconds }
        if (seconds == 0L) null else DisciplineShare(d, Duration.ofSeconds(seconds), seconds.toDouble() / total)
    }
}

// --- Kiedy trenuję ---

/** Sumy dla każdego dnia tygodnia, od poniedziałku; dni bez treningów też są (zerowy słupek). */
fun List<Workout>.byDayOfWeek(zone: ZoneId): Map<DayOfWeek, Totals> =
    DayOfWeek.entries.associateWith { day ->
        filter { it.startLocal(zone).dayOfWeek == day }.totals()
    }

/** Przedział godzinowy, np. 6:00–10:00. */
data class TimeOfDayBin(val fromHour: Int, val toHour: Int, val totals: Totals)

/** Doba podzielona na przedziały po [binHours] godzin, liczone po godzinie rozpoczęcia treningu. */
fun List<Workout>.byTimeOfDay(zone: ZoneId, binHours: Int = 4): List<TimeOfDayBin> =
    (0 until 24 step binHours).map { from ->
        val to = from + binHours
        TimeOfDayBin(from, to, filter { it.startLocal(zone).hour in from until to }.totals())
    }
