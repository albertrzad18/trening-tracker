package io.github.albertrzad18.treningtracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WorkoutEntity::class, SampleEntity::class, RoutePointEntity::class, GoalEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun sampleDao(): SampleDao
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "trening.db")
                .addMigrations(MIGRATION_1_2)
                // Klucze obce kasują próbki i trasę razem z treningiem.
                .build()
                .also { instance = it }
        }
    }
}
