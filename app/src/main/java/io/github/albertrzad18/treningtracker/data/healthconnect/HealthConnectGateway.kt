package io.github.albertrzad18.treningtracker.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
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

class HealthConnectGateway(private val context: Context) {

    fun sdkStatus(): Int = HealthConnectClient.getSdkStatus(context)

    private val client: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    /** Uprawnienia do odczytu treningów i danych dodatkowych. */
    val readPermissions: Set<String> =
        (listOf(ExerciseSessionRecord::class) + DataKind.entries.map { it.recordClass })
            .map { HealthPermission.getReadPermission(it) }
            .toSet()

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
