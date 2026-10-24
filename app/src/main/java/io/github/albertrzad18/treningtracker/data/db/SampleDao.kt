package io.github.albertrzad18.treningtracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SampleDao {
    @Query("SELECT * FROM samples WHERE workoutId = :workoutId AND type = :type ORDER BY timeMillis")
    fun observeSamples(workoutId: String, type: SampleType): Flow<List<SampleEntity>>

    @Query("SELECT * FROM route_points WHERE workoutId = :workoutId ORDER BY timeMillis")
    fun observeRoute(workoutId: String): Flow<List<RoutePointEntity>>

    @Insert
    suspend fun insertSamples(samples: List<SampleEntity>)

    @Insert
    suspend fun insertRoute(points: List<RoutePointEntity>)

    @Query("DELETE FROM samples WHERE workoutId = :workoutId")
    suspend fun deleteSamples(workoutId: String)

    @Query("DELETE FROM route_points WHERE workoutId = :workoutId")
    suspend fun deleteRoute(workoutId: String)

    /** Podmienia cały przebieg treningu: najpierw kasujemy stare próbki, potem wstawiamy nowe. */
    @Transaction
    suspend fun replaceSamples(workoutId: String, samples: List<SampleEntity>, route: List<RoutePointEntity>) {
        deleteSamples(workoutId)
        deleteRoute(workoutId)
        if (samples.isNotEmpty()) insertSamples(samples)
        if (route.isNotEmpty()) insertRoute(route)
    }

    /** Id treningów, które mają już zapisany przebieg: nie pobieramy ich drugi raz. */
    @Query("SELECT DISTINCT workoutId FROM samples")
    suspend fun workoutIdsWithSamples(): List<String>
}
