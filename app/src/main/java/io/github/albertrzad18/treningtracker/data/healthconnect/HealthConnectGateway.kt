package io.github.albertrzad18.treningtracker.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import io.github.albertrzad18.treningtracker.data.db.WorkoutEntity
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import io.github.albertrzad18.treningtracker.domain.DailyStats
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseRouteResult
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import io.github.albertrzad18.treningtracker.domain.Discipline
import io.github.albertrzad18.treningtracker.domain.SamplePoint
import io.github.albertrzad18.treningtracker.domain.downsample
import java.time.Instant
import kotlin.reflect.KClass

/** Rodzaje danych, których obecność sprawdzamy dla każdego treningu. */
enum class DataKind(val recordClass: KClass<out Record>) {
    HEART_RATE(HeartRateRecord::class),
    DISTANCE(DistanceRecord::class),
    TOTAL_CALORIES(TotalCaloriesBurnedRecord::class),
    ACTIVE_CALORIES(ActiveCaloriesBurnedRecord::class),
    SPEED(SpeedRecord::class),
    POWER(PowerRecord::class),
    CADENCE(CyclingPedalingCadenceRecord::class),
    ELEVATION(ElevationGainedRecord::class),
}

enum class RouteState { NONE, DATA, CONSENT_REQUIRED }

/** Co wiemy o jednym treningu: jakie dane do niego dotarły. */
data class SessionProbe(
    val exerciseType: Int,
    val discipline: Discipline?,
    val title: String?,
    val start: Instant,
    val durationSeconds: Long,
    val origin: String,
    val recordCounts: Map<DataKind, Int>,
    val segments: Int,
    val laps: Int,
    val route: RouteState,
)

/** Jeden punkt trasy GPS. */
data class RoutePoint(val timeMillis: Long, val latitude: Double, val longitude: Double, val altitudeM: Double?)

/** Przebieg treningu w czasie: tętno, prędkość i trasa. */
data class SessionSeries(
    val heartRate: List<SamplePoint>,
    val speed: List<SamplePoint>,
    val route: List<RoutePoint>,
) {
    val isEmpty: Boolean get() = heartRate.isEmpty() && speed.isEmpty() && route.isEmpty()
}

/** Porcja zmian z Changes API: nowe/zmienione sesje, usunięte id, kolejny token. */
data class ChangesBatch(
    val upserts: List<ExerciseSessionRecord>,
    val deletedIds: List<String>,
    val nextToken: String,
    val hasMore: Boolean,
    val expired: Boolean,
)

class HealthConnectGateway(private val context: Context) {

    fun sdkStatus(): Int = HealthConnectClient.getSdkStatus(context)

    private val client: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    /** Uprawnienia do odczytu treningów i danych dodatkowych. */
    val readPermissions: Set<String> =
        (listOf(ExerciseSessionRecord::class) + DataKind.entries.map { it.recordClass } + DAILY_KINDS)
            .map { HealthPermission.getReadPermission(it) }
            .toSet()

    /** Uprawnienia do danych dziennych (kroki, kalorie spoczynkowe i aktywne); bez nich apka działa, tylko bez tych liczb. */
    val dailyPermissions: Set<String> = DAILY_KINDS.map { HealthPermission.getReadPermission(it) }.toSet()

    /**
     * Kroki i kalorie spalane naturalnie dzień po dniu. Agregacja Health Connect sama łączy źródła
     * (telefon i zegarek) bez podwójnego liczenia. Metryki bez uprawnienia pomijamy.
     */
    suspend fun readDailyStats(from: java.time.LocalDate, toInclusive: java.time.LocalDate, granted: Set<String>): List<DailyStats> {
        val steps = HealthPermission.getReadPermission(StepsRecord::class) in granted
        val basal = HealthPermission.getReadPermission(BasalMetabolicRateRecord::class) in granted
        val active = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class) in granted
        if (!steps && !basal && !active) return emptyList()
        val metrics = buildSet {
            if (steps) add(StepsRecord.COUNT_TOTAL)
            if (basal) add(BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL)
            if (active) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
        }
        val result = client.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = metrics,
                timeRangeFilter = TimeRangeFilter.between(from.atStartOfDay(), toInclusive.plusDays(1).atStartOfDay()),
                timeRangeSlicer = java.time.Period.ofDays(1),
            ),
        )
        return result.map { day ->
            DailyStats(
                date = day.startTime.toLocalDate(),
                steps = day.result[StepsRecord.COUNT_TOTAL],
                basalKcal = day.result[BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL]?.inKilocalories,
                activeKcal = day.result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories,
            )
        }
    }

    /**
     * Diagnostyka dnia [date]: kroki i kalorie z agregatu Health Connect oraz surowe rekordy pogrupowane po źródle.
     * Jeśli suma surowych rekordów jest większa od agregatu albo źródeł jest kilka, liczby mogą się rozjeżdżać z zegarkiem.
     */
    suspend fun dayBreakdown(date: java.time.LocalDate, granted: Set<String>): String = buildString {
        val range = TimeRangeFilter.between(date.atStartOfDay(), date.plusDays(1).atStartOfDay())
        appendLine("Dzień $date")
        fun can(record: KClass<out Record>) = HealthPermission.getReadPermission(record) in granted

        if (can(StepsRecord::class)) {
            val total = client.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), range))[StepsRecord.COUNT_TOTAL]
            appendLine("Kroki (agregat): ${total ?: "brak"}")
            client.readRecords(ReadRecordsRequest(StepsRecord::class, range)).records
                .groupBy { it.metadata.dataOrigin.packageName }
                .forEach { (pkg, list) -> appendLine("  $pkg: ${list.sumOf { it.count }} (${list.size} rek.)") }
        }
        if (can(ActiveCaloriesBurnedRecord::class)) {
            val total = client.aggregate(AggregateRequest(setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL), range))[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
            appendLine("Kcal aktywne (agregat): ${total?.inKilocalories?.toInt() ?: "brak"}")
            client.readRecords(ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, range)).records
                .groupBy { it.metadata.dataOrigin.packageName }
                .forEach { (pkg, list) -> appendLine("  $pkg: ${list.sumOf { it.energy.inKilocalories }.toInt()} (${list.size} rek.)") }
        }
        if (can(BasalMetabolicRateRecord::class)) {
            val total = client.aggregate(AggregateRequest(setOf(BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL), range))[BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL]
            appendLine("Kcal spoczynkowe (agregat): ${total?.inKilocalories?.toInt() ?: "brak"}")
        }
        if (can(TotalCaloriesBurnedRecord::class)) {
            val total = client.aggregate(AggregateRequest(setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL), range))[TotalCaloriesBurnedRecord.ENERGY_TOTAL]
            appendLine("Kcal łącznie wg Health Connect: ${total?.inKilocalories?.toInt() ?: "brak"}")
        }
    }.trim()

    val historyPermission = "android.permission.health.READ_HEALTH_DATA_HISTORY"

    fun isHistoryAvailable(): Boolean =
        client.features.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY) ==
            HealthConnectFeatures.FEATURE_STATUS_AVAILABLE

    suspend fun grantedPermissions(): Set<String> = client.permissionController.getGrantedPermissions()

    suspend fun readSessions(from: Instant, to: Instant): List<ExerciseSessionRecord> {
        val result = mutableListOf<ExerciseSessionRecord>()
        var pageToken: String? = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(from, to),
                    pageToken = pageToken,
                ),
            )
            result += response.records
            pageToken = response.pageToken
        } while (pageToken != null)
        return result
    }

    val backgroundPermission = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"

    fun isBackgroundReadAvailable(): Boolean =
        client.features.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND) ==
            HealthConnectFeatures.FEATURE_STATUS_AVAILABLE

    /** Wszystkie uprawnienia, o które prosimy użytkownika (odczyt + opcjonalnie historia i tło). */
    fun wantedPermissions(): Set<String> = buildSet {
        addAll(readPermissions)
        if (isHistoryAvailable()) add(historyPermission)
        if (isBackgroundReadAvailable()) add(backgroundPermission)
    }

    /** Token do późniejszego pobierania tylko zmian w treningach (Changes API). */
    suspend fun newChangesToken(): String =
        client.getChangesToken(ChangesTokenRequest(recordTypes = setOf(ExerciseSessionRecord::class)))

    /** Jedna porcja zmian od [token]. `expired` = token wygasł i trzeba zrobić pełny odczyt. */
    suspend fun changesSince(token: String): ChangesBatch {
        val response = client.getChanges(token)
        if (response.changesTokenExpired) return ChangesBatch(emptyList(), emptyList(), token, false, expired = true)
        val upserts = response.changes.filterIsInstance<UpsertionChange>().mapNotNull { it.record as? ExerciseSessionRecord }
        val deletedIds = response.changes.filterIsInstance<DeletionChange>().map { it.recordId }
        return ChangesBatch(upserts, deletedIds, response.nextChangesToken, response.hasMore, expired = false)
    }

    /**
     * Zamienia sesję z Health Connect na wiersz bazy z podsumowaniem (dystans, kalorie, tętno, prędkość, przewyższenie).
     * Zwraca `null` dla dyscyplin, których nie obsługujemy. Puste wartości zostają `null`.
     */
    suspend fun toEntity(session: ExerciseSessionRecord, granted: Set<String>, nowMillis: Long): WorkoutEntity? {
        val discipline = disciplineOf(session.exerciseType) ?: return null
        val metrics = buildSet<AggregateMetric<*>> {
            fun allowed(kind: DataKind) = HealthPermission.getReadPermission(kind.recordClass) in granted
            if (allowed(DataKind.DISTANCE)) add(DistanceRecord.DISTANCE_TOTAL)
            if (allowed(DataKind.TOTAL_CALORIES)) add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
            if (allowed(DataKind.HEART_RATE)) {
                add(HeartRateRecord.BPM_AVG)
                add(HeartRateRecord.BPM_MAX)
            }
            if (allowed(DataKind.SPEED)) {
                add(SpeedRecord.SPEED_AVG)
                add(SpeedRecord.SPEED_MAX)
            }
            if (allowed(DataKind.ELEVATION)) add(ElevationGainedRecord.ELEVATION_GAINED_TOTAL)
        }
        val result = if (metrics.isEmpty()) null else {
            client.aggregate(AggregateRequest(metrics, TimeRangeFilter.between(session.startTime, session.endTime)))
        }
        return WorkoutEntity(
            id = session.metadata.id,
            discipline = discipline,
            startMillis = session.startTime.toEpochMilli(),
            zoneOffsetSec = session.startZoneOffset?.totalSeconds,
            durationSec = session.endTime.epochSecond - session.startTime.epochSecond,
            title = session.title?.takeIf { it.isNotBlank() },
            distanceM = result?.get(DistanceRecord.DISTANCE_TOTAL)?.inMeters?.takeIf { it > 0 },
            caloriesKcal = result?.get(TotalCaloriesBurnedRecord.ENERGY_TOTAL)?.inKilocalories?.takeIf { it > 0 },
            avgHeartRate = result?.get(HeartRateRecord.BPM_AVG),
            maxHeartRate = result?.get(HeartRateRecord.BPM_MAX),
            avgSpeedMps = result?.get(SpeedRecord.SPEED_AVG)?.inMetersPerSecond?.takeIf { it > 0 },
            maxSpeedMps = result?.get(SpeedRecord.SPEED_MAX)?.inMetersPerSecond?.takeIf { it > 0 },
            elevationGainM = result?.get(ElevationGainedRecord.ELEVATION_GAINED_TOTAL)?.inMeters?.takeIf { it > 0 },
            dataOrigin = session.metadata.dataOrigin.packageName,
            syncedAt = nowMillis,
        )
    }

    /**
     * Przebieg jednego treningu: próbki tętna i prędkości oraz punkty trasy.
     * Próbki są przerzedzone, żeby w bazie nie lądowały tysiące punktów z jednego treningu.
     */
    suspend fun readSeries(session: ExerciseSessionRecord, granted: Set<String>): SessionSeries {
        val range = TimeRangeFilter.between(session.startTime, session.endTime)
        fun allowed(kind: DataKind) = HealthPermission.getReadPermission(kind.recordClass) in granted

        val heartRate = if (!allowed(DataKind.HEART_RATE)) emptyList() else {
            client.readRecords(ReadRecordsRequest(HeartRateRecord::class, range)).records
                .flatMap { record -> record.samples.map { SamplePoint(it.time.toEpochMilli(), it.beatsPerMinute.toDouble()) } }
                .sortedBy { it.timeMillis }
        }
        val speed = if (!allowed(DataKind.SPEED)) emptyList() else {
            client.readRecords(ReadRecordsRequest(SpeedRecord::class, range)).records
                .flatMap { record -> record.samples.map { SamplePoint(it.time.toEpochMilli(), it.speed.inMetersPerSecond) } }
                .sortedBy { it.timeMillis }
        }
        // Trasa przychodzi razem z sesją; gdy Health Connect chce osobnej zgody, zostawiamy pustą listę.
        val route = (session.exerciseRouteResult as? ExerciseRouteResult.Data)?.exerciseRoute?.route.orEmpty()

        return SessionSeries(
            heartRate = downsample(heartRate),
            speed = downsample(speed),
            route = route.map {
                RoutePoint(it.time.toEpochMilli(), it.latitude, it.longitude, it.altitude?.inMeters)
            },
        )
    }

    /** Dla jednego treningu sprawdza, jakie rekordy leżą w jego zakresie czasu. */
    suspend fun probe(session: ExerciseSessionRecord, granted: Set<String>): SessionProbe {
        val range = TimeRangeFilter.between(session.startTime, session.endTime)
        val counts = DataKind.entries.mapNotNull { kind ->
            // bez uprawnienia do tego rodzaju danych nie pytamy (rzuciłoby SecurityException)
            if (HealthPermission.getReadPermission(kind.recordClass) !in granted) return@mapNotNull null
            kind to client.readRecords(ReadRecordsRequest(kind.recordClass, range)).records.size
        }.toMap()
        return SessionProbe(
            exerciseType = session.exerciseType,
            discipline = disciplineOf(session.exerciseType),
            title = session.title,
            start = session.startTime,
            durationSeconds = session.endTime.epochSecond - session.startTime.epochSecond,
            origin = session.metadata.dataOrigin.packageName,
            recordCounts = counts,
            segments = session.segments.size,
            laps = session.laps.size,
            route = when (session.exerciseRouteResult) {
                is ExerciseRouteResult.Data -> RouteState.DATA
                is ExerciseRouteResult.ConsentRequired -> RouteState.CONSENT_REQUIRED
                else -> RouteState.NONE
            },
        )
    }
}

/** Rekordy dzienne czytane osobno od treningów. */
private val DAILY_KINDS = listOf(StepsRecord::class, BasalMetabolicRateRecord::class, ActiveCaloriesBurnedRecord::class)
