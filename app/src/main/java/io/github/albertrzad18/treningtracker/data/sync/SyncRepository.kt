package io.github.albertrzad18.treningtracker.data.sync

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import io.github.albertrzad18.treningtracker.data.db.DailyStatsDao
import io.github.albertrzad18.treningtracker.data.db.DailyStatsEntity
import io.github.albertrzad18.treningtracker.data.db.RoutePointEntity
import io.github.albertrzad18.treningtracker.data.db.SampleDao
import io.github.albertrzad18.treningtracker.data.db.SampleEntity
import io.github.albertrzad18.treningtracker.data.db.SampleType
import io.github.albertrzad18.treningtracker.data.db.WorkoutDao
import io.github.albertrzad18.treningtracker.data.healthconnect.HealthConnectGateway
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.temporal.ChronoUnit

sealed interface SyncResult {
    data class Success(val changed: Int) : SyncResult
    data object NoPermission : SyncResult
    data object Unavailable : SyncResult
    data class Failed(val message: String) : SyncResult
}

/**
 * Przenosi treningi z Health Connect do Room.
 * Pierwszy raz (albo po wygaśnięciu tokenu) czytamy cały dostępny zakres, potem tylko zmiany (Changes API).
 * Treningi usunięte lokalnie z Health Connect kasujemy z Room tylko wtedy, gdy Health Connect o tym poinformuje
 * (DeletionChange). Pełny odczyt niczego nie kasuje, bo Room ma przechowywać historię, której Health Sync nie przenosi.
 */
class SyncRepository(
    private val gateway: HealthConnectGateway,
    private val dao: WorkoutDao,
    private val sampleDao: SampleDao,
    private val dailyDao: DailyStatsDao,
    private val prefs: SyncPrefs,
    private val clock: () -> Instant = Instant::now,
) {
    private val lock = Mutex()

    suspend fun sync(forceFull: Boolean = false): SyncResult = lock.withLock {
        try {
            doSync(forceFull).also { result ->
                when (result) {
                    is SyncResult.Success -> prefs.saveSuccess(clock().toEpochMilli())
                    is SyncResult.Failed -> prefs.saveError(result.message)
                    else -> Unit
                }
            }
        } catch (e: Exception) {
            val message = e.message ?: e::class.java.simpleName
            prefs.saveError(message)
            SyncResult.Failed(message)
        }
    }

    private suspend fun doSync(forceFull: Boolean): SyncResult {
        if (gateway.sdkStatus() != HealthConnectClient.SDK_AVAILABLE) return SyncResult.Unavailable
        val granted = gateway.grantedPermissions()
        if (HealthPermission.getReadPermission(ExerciseSessionRecord::class) !in granted) return SyncResult.NoPermission

        val now = clock()
        var changed = 0
        val token = prefs.changesToken
        val historyGranted = gateway.historyPermission in granted
        // Pełny odczyt także wtedy, gdy uprawnienie do historii doszło później: Changes API nie odda starszych danych.
        var needFullRead = token == null || forceFull || (historyGranted && !prefs.fullReadHadHistory)

        if (!needFullRead && token != null) {
            // Wyjątek (np. nieprawidłowy token) traktujemy tak samo jak wygaśnięcie: pełny odczyt.
            val applied = runCatching { applyChanges(token, granted, now.toEpochMilli()) }.getOrNull()
            if (applied == null || applied == EXPIRED) needFullRead = true else changed += applied
        }

        if (needFullRead) {
            // Token bierzemy PRZED odczytem, żeby zmiany w trakcie odczytu nie przepadły.
            val newToken = gateway.newChangesToken()
            val days = if (gateway.historyPermission in granted) FULL_HISTORY_DAYS else LIMITED_DAYS
            changed += upsertSessions(now.minus(days, ChronoUnit.DAYS), now, granted)
            prefs.changesToken = newToken
            prefs.fullReadHadHistory = historyGranted
        } else {
            // Tętno i kalorie mogą dojść po zakończeniu sesji, a Changes API śledzi tylko same sesje.
            changed += upsertSessions(now.minus(RECENT_DAYS, ChronoUnit.DAYS), now, granted)
        }
        // Kroki i kalorie podstawowe to osobne dane dzienne; ich brak lub błąd nie psuje synchronizacji treningów.
        runCatching { syncDaily(now, granted) }
        return SyncResult.Success(changed)
    }

    /** Dane dzienne z ostatnich dni (pełny zakres tylko z uprawnieniem do historii). */
    private suspend fun syncDaily(now: Instant, granted: Set<String>) {
        val days = if (gateway.historyPermission in granted) FULL_HISTORY_DAYS.coerceAtMost(DAILY_MAX_DAYS) else LIMITED_DAYS
        val zone = java.time.ZoneId.systemDefault()
        val today = now.atZone(zone).toLocalDate()
        val stats = gateway.readDailyStats(today.minusDays(days), today, granted)
        val entities = stats
            .filter { it.steps != null || it.totalKcal != null }
            .map { DailyStatsEntity(it.date.toString(), it.steps, it.basalKcal, "healthconnect", it.activeKcal) }
        if (entities.isNotEmpty()) dailyDao.upsertAll(entities)
    }

    /** Zwraca liczbę zmienionych treningów albo [EXPIRED], gdy trzeba zrobić pełny odczyt. */
    private suspend fun applyChanges(startToken: String, granted: Set<String>, nowMillis: Long): Int {
        var token = startToken
        var changed = 0
        do {
            val batch = gateway.changesSince(token)
            if (batch.expired) return EXPIRED
            val entities = batch.upserts.mapNotNull { gateway.toEntity(it, granted, nowMillis) }
            if (entities.isNotEmpty()) dao.upsertAll(entities)
            if (batch.deletedIds.isNotEmpty()) dao.deleteByIds(batch.deletedIds)
            changed += entities.size + batch.deletedIds.size
            token = batch.nextToken
            prefs.changesToken = token
        } while (batch.hasMore)
        return changed
    }

    private suspend fun upsertSessions(from: Instant, to: Instant, granted: Set<String>): Int {
        val sessions = gateway.readSessions(from, to)
        val entities = sessions.mapNotNull { gateway.toEntity(it, granted, clock().toEpochMilli()) }
        if (entities.isNotEmpty()) dao.upsertAll(entities)
        saveSeries(sessions, granted)
        return entities.size
    }

    /**
     * Zapisuje przebiegi (tętno, prędkość, trasa) dla treningów, które ich jeszcze nie mają.
     * Jedna synchronizacja bierze najwyżej [SERIES_PER_SYNC] treningów, od najnowszego,
     * żeby pierwszy odczyt całej historii nie trwał w nieskończoność. Reszta dojdzie przy kolejnych.
     */
    private suspend fun saveSeries(sessions: List<ExerciseSessionRecord>, granted: Set<String>) {
        val known = sampleDao.workoutIdsWithSamples().toSet()
        val todo = sessions
            .filter { it.metadata.id !in known }
            .sortedByDescending { it.startTime }
            .take(SERIES_PER_SYNC)
        todo.forEach { session ->
            val series = runCatching { gateway.readSeries(session, granted) }.getOrNull() ?: return@forEach
            if (series.isEmpty) return@forEach
            val id = session.metadata.id
            sampleDao.replaceSamples(
                workoutId = id,
                samples = series.heartRate.map { SampleEntity(workoutId = id, type = SampleType.HEART_RATE, timeMillis = it.timeMillis, value = it.value) } +
                    series.speed.map { SampleEntity(workoutId = id, type = SampleType.SPEED, timeMillis = it.timeMillis, value = it.value) },
                route = series.route.map {
                    RoutePointEntity(
                        workoutId = id,
                        timeMillis = it.timeMillis,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        altitudeM = it.altitudeM,
                    )
                },
            )
        }
    }

    private companion object {
        const val EXPIRED = -1
        const val RECENT_DAYS = 2L
        const val LIMITED_DAYS = 30L
        const val DAILY_MAX_DAYS = 800L
        const val FULL_HISTORY_DAYS = 365L * 5
        const val SERIES_PER_SYNC = 25
    }
}
