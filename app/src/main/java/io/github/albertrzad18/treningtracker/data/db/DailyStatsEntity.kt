package io.github.albertrzad18.treningtracker.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import io.github.albertrzad18.treningtracker.domain.DailyStats
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Dane z całego dnia, niezwiązane z żadnym treningiem: kroki i kalorie spalane naturalnie (podstawowa przemiana materii).
 * Brak wartości (`null`) znaczy, że Health Connect jej nie podał, więc w apce jest ukryta, a nie zerowa.
 */
@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    /** Dzień w formacie ISO, np. `2026-10-24`. */
    @PrimaryKey val date: String,
    val steps: Long?,
    val basalKcal: Double?,
    val dataOrigin: String,
)

fun DailyStatsEntity.toDomain() = DailyStats(LocalDate.parse(date), steps, basalKcal)

@Dao
interface DailyStatsDao {
    @Query("SELECT * FROM daily_stats ORDER BY date")
    fun observeAll(): Flow<List<DailyStatsEntity>>

    @Upsert
    suspend fun upsertAll(days: List<DailyStatsEntity>)

    /** Używane przez dane demo w buildzie debug: usuwa wszystko z jednego źródła. */
    @Query("DELETE FROM daily_stats WHERE dataOrigin = :origin")
    suspend fun deleteByOrigin(origin: String)
}
