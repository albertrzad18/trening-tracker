package io.github.albertrzad18.treningtracker.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Wersja 2: dochodzą przebiegi treningu (tętno, prędkość), punkty trasy i cele.
 * Tabela `workouts` zostaje bez zmian, więc zapisane treningi przeżywają aktualizację.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `samples` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`workoutId` TEXT NOT NULL, " +
                "`type` TEXT NOT NULL, " +
                "`timeMillis` INTEGER NOT NULL, " +
                "`value` REAL NOT NULL, " +
                "FOREIGN KEY(`workoutId`) REFERENCES `workouts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_samples_workoutId_type` ON `samples` (`workoutId`, `type`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `route_points` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`workoutId` TEXT NOT NULL, " +
                "`timeMillis` INTEGER NOT NULL, " +
                "`latitude` REAL NOT NULL, " +
                "`longitude` REAL NOT NULL, " +
                "`altitudeM` REAL, " +
                "FOREIGN KEY(`workoutId`) REFERENCES `workouts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_route_points_workoutId` ON `route_points` (`workoutId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `goals` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`discipline` TEXT, " +
                "`period` TEXT NOT NULL, " +
                "`metric` TEXT NOT NULL, " +
                "`target` REAL NOT NULL, " +
                "`createdAtMillis` INTEGER NOT NULL)",
        )
    }
}
