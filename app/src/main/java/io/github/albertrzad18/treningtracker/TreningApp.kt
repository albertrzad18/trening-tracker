package io.github.albertrzad18.treningtracker

import android.app.Application
import io.github.albertrzad18.treningtracker.data.db.AppDatabase
import io.github.albertrzad18.treningtracker.data.healthconnect.HealthConnectGateway
import io.github.albertrzad18.treningtracker.data.sync.SyncPrefs
import io.github.albertrzad18.treningtracker.data.sync.SyncRepository
import io.github.albertrzad18.treningtracker.data.sync.SyncScheduler

/** Klasa aplikacji: tworzy współdzielone obiekty (baza, Health Connect, synchronizacja) i planuje synchronizację. */
class TreningApp : Application() {
    val database by lazy { AppDatabase.get(this) }
    val gateway by lazy { HealthConnectGateway(this) }
    val syncPrefs by lazy { SyncPrefs(this) }
    val syncRepository by lazy { SyncRepository(gateway, database.workoutDao(), syncPrefs) }

    override fun onCreate() {
        super.onCreate()
        SyncScheduler.schedulePeriodic(this)
    }
}
