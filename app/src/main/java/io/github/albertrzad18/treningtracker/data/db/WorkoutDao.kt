package io.github.albertrzad18.treningtracker.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY startMillis DESC")
    fun observeAll(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE id = :id")
    fun observeById(id: String): Flow<WorkoutEntity?>

    /** Wstawia nowe treningi, a istniejące (to samo `id`) nadpisuje. */
    @Upsert
    suspend fun upsertAll(workouts: List<WorkoutEntity>)

    @Query("DELETE FROM workouts WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT id FROM workouts WHERE startMillis >= :sinceMillis")
    suspend fun idsSince(sinceMillis: Long): List<String>

    /** Używane przez dane demo w buildzie debug: usuwa wszystko z jednego źródła. */
    @Query("DELETE FROM workouts WHERE dataOrigin = :origin")
    suspend fun deleteByOrigin(origin: String)
}
