package io.github.albertrzad18.treningtracker.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.albertrzad18.treningtracker.TreningApp
import java.util.concurrent.TimeUnit

/** Zadanie w tle: synchronizuje Health Connect → Room. Błędy zapisuje w [SyncPrefs], nie wymusza ponawiania. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        (applicationContext as TreningApp).syncRepository.sync()
        return Result.success()
    }
}

object SyncScheduler {
    private const val PERIODIC = "sync_periodic"
    private const val NOW = "sync_now"

    /** Cykliczna synchronizacja co 6 godzin (minimum WorkManagera to 15 minut). */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Jednorazowa synchronizacja od razu (start apki, przycisk „Synchronizuj teraz”). */
    fun syncNow(context: Context) {
        WorkManager.getInstance(context)
            .enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<SyncWorker>().build())
    }
}
