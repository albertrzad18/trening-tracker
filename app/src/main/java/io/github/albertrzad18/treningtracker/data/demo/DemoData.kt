package io.github.albertrzad18.treningtracker.data.demo

import io.github.albertrzad18.treningtracker.data.db.RoutePointEntity
import io.github.albertrzad18.treningtracker.data.db.SampleEntity
import io.github.albertrzad18.treningtracker.data.db.SampleType
import io.github.albertrzad18.treningtracker.data.db.WorkoutEntity
import io.github.albertrzad18.treningtracker.domain.Discipline
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.random.Random

/** Treningi demo mają takie `dataOrigin`, dzięki czemu da się je potem usunąć jednym zapytaniem. */
const val DEMO_ORIGIN = "demo"

/**
 * Sztuczne treningi do sprawdzenia wykresów i tabeli bez zegarka: 18 miesięcy pływania,
 * trenażera (zimą) i roweru na dworze (latem), z lekką poprawą formy w czasie.
 * Ziarno losowania jest stałe, więc dane są zawsze takie same.
 */
fun demoWorkouts(today: LocalDate, zone: ZoneId, months: Long = 18, seed: Int = 2026): List<WorkoutEntity> {
    val random = Random(seed)
    val start = today.minusMonths(months).withDayOfMonth(1)
    val now = System.currentTimeMillis()
    val result = mutableListOf<WorkoutEntity>()
    var index = 0

    var day = start
    while (!day.isAfter(today)) {
        // Jak daleko jesteśmy od początku (0..1): im dalej, tym lepsza forma.
        val progress = java.time.temporal.ChronoUnit.DAYS.between(start, day).toDouble() /
            java.time.temporal.ChronoUnit.DAYS.between(start, today).coerceAtLeast(1)
        val summer = day.monthValue in 4..9

        fun add(discipline: Discipline, hour: Int, build: (Random) -> DemoMetrics) {
            val metrics = build(random)
            val startInstant = day.atTime(LocalTime.of(hour, random.nextInt(0, 60))).atZone(zone).toInstant()
            result += WorkoutEntity(
                id = "demo-${index++}",
                discipline = discipline,
                startMillis = startInstant.toEpochMilli(),
                zoneOffsetSec = zone.rules.getOffset(startInstant).totalSeconds,
                durationSec = metrics.minutes * 60L,
                title = null,
                distanceM = metrics.distanceM,
                caloriesKcal = metrics.kcal,
                avgHeartRate = metrics.avgHr,
                maxHeartRate = metrics.avgHr + random.nextInt(8, 25),
                avgSpeedMps = metrics.distanceM?.let { it / (metrics.minutes * 60.0) },
                maxSpeedMps = metrics.distanceM?.let { it / (metrics.minutes * 60.0) * 1.4 },
                elevationGainM = metrics.elevationM,
                dataOrigin = DEMO_ORIGIN,
                syncedAt = now,
            )
        }

        when (day.dayOfWeek.value) {
            // Poniedziałek i czwartek: basen.
            1, 4 -> if (random.nextInt(10) < 8) add(Discipline.POOL, 18) { r ->
                val minutes = r.nextInt(30, 61).toLong()
                // Tempo poprawia się z 2:20 do około 1:55 na 100 m.
                val pacePer100 = 140 - 25 * progress + r.nextInt(-6, 7)
                DemoMetrics(
                    minutes = minutes,
                    distanceM = (minutes * 60 / pacePer100 * 100).toInt().toDouble(),
                    kcal = (minutes * r.nextInt(8, 12)).toDouble(),
                    avgHr = r.nextInt(118, 142).toLong(),
                )
            }
            // Wtorek: zimą trenażer, latem krótszy rower.
            2 -> if (summer) {
                add(Discipline.ROAD, 17) { r -> roadMetrics(r, progress, r.nextInt(45, 76).toLong()) }
            } else {
                add(Discipline.TRAINER, 19) { r -> trainerMetrics(r, r.nextInt(45, 76).toLong()) }
            }
            // Sobota: latem długa trasa, zimą dłuższy trenażer.
            6 -> if (summer) {
                add(Discipline.ROAD, 9) { r -> roadMetrics(r, progress, r.nextInt(90, 211).toLong()) }
            } else if (random.nextInt(10) < 7) {
                add(Discipline.TRAINER, 10) { r -> trainerMetrics(r, r.nextInt(60, 121).toLong()) }
            }
        }
        day = day.plusDays(1)
    }
    return result
}

/**
 * Przebieg jednego treningu demo: tętno co pół minuty, prędkość tam, gdzie jest dystans,
 * i trasa w kształcie pętli dla roweru na dworze (okolice Krakowa).
 */
fun demoSeries(workout: WorkoutEntity, seed: Int = workout.id.hashCode()): DemoSeries {
    val random = Random(seed)
    val stepSec = 30L
    val steps = (workout.durationSec / stepSec).toInt().coerceIn(2, 400)
    val avgHr = workout.avgHeartRate?.toDouble() ?: 130.0
    val avgSpeed = workout.avgSpeedMps

    val heartRate = (0 until steps).map { i ->
        // Rozgrzewka na początku, mocniejsza końcówka, do tego drobne wahania.
        val progress = i.toDouble() / steps
        val shape = 0.85 + 0.25 * progress + 0.05 * kotlin.math.sin(i / 3.0)
        SampleEntity(
            workoutId = workout.id,
            type = SampleType.HEART_RATE,
            timeMillis = workout.startMillis + i * stepSec * 1000,
            value = (avgHr * shape + random.nextInt(-4, 5)).coerceIn(80.0, 200.0),
        )
    }
    val speed = if (avgSpeed == null) emptyList() else (0 until steps).map { i ->
        SampleEntity(
            workoutId = workout.id,
            type = SampleType.SPEED,
            timeMillis = workout.startMillis + i * stepSec * 1000,
            value = (avgSpeed * (0.8 + random.nextDouble() * 0.45)).coerceAtLeast(0.1),
        )
    }
    val route = if (workout.discipline != Discipline.ROAD) emptyList() else {
        // Pętla o obwodzie równym dystansowi: promień w km to dystans / 2π, a stopień to około 111 km.
        val radiusDeg = (workout.distanceM ?: 20000.0) / 1000 / (2 * Math.PI) / 111.0
        (0 until steps).map { i ->
            val angle = 2 * Math.PI * i / steps
            RoutePointEntity(
                workoutId = workout.id,
                timeMillis = workout.startMillis + i * stepSec * 1000,
                latitude = DEMO_LAT + radiusDeg * kotlin.math.sin(angle) + random.nextDouble(-0.0005, 0.0005),
                // Długość geograficzna „kurczy się” z szerokością, stąd mnożnik.
                longitude = DEMO_LON + radiusDeg * 1.55 * kotlin.math.cos(angle) + random.nextDouble(-0.0005, 0.0005),
                altitudeM = 220 + 80 * kotlin.math.sin(angle * 3) + random.nextDouble(-5.0, 5.0),
            )
        }
    }
    return DemoSeries(heartRate + speed, route)
}

data class DemoSeries(val samples: List<SampleEntity>, val route: List<RoutePointEntity>)

/** Środek demonstracyjnych tras: okolice Krakowa. */
private const val DEMO_LAT = 50.06
private const val DEMO_LON = 19.94

private data class DemoMetrics(
    val minutes: Long,
    val distanceM: Double?,
    val kcal: Double?,
    val avgHr: Long,
    val elevationM: Double? = null,
)

/** Rower na dworze: prędkość rośnie z formą, jest dystans i przewyższenie. */
private fun roadMetrics(r: Random, progress: Double, minutes: Long): DemoMetrics {
    val kmh = 24 + 4 * progress + r.nextInt(-3, 4)
    return DemoMetrics(
        minutes = minutes,
        distanceM = kmh * minutes / 60.0 * 1000,
        kcal = (minutes * r.nextInt(9, 13)).toDouble(),
        avgHr = r.nextInt(122, 148).toLong(),
        elevationM = (minutes * r.nextInt(2, 6)).toDouble(),
    )
}

/** Trenażer: tak jak u Alberta, tylko czas, tętno i kalorie (bez dystansu). */
private fun trainerMetrics(r: Random, minutes: Long): DemoMetrics = DemoMetrics(
    minutes = minutes,
    distanceM = null,
    kcal = (minutes * r.nextInt(8, 12)).toDouble(),
    avgHr = r.nextInt(128, 158).toLong(),
)
