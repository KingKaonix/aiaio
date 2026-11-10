package io.github.kingkaonix.aiaio.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Background sync entry point. Phase 0 is a no-op that keeps the WorkManager wiring honest;
 * Phase 5 delegates to [SyncEngine] with the user's selected transport and records results for
 * the Sync Doctor screen.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // TODO(phase-5): await SyncEngine.sync(selectedTransport()) and map outcomes to
        // Result.retry() / Result.failure() with a persisted Sync Doctor entry.
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "aiaio-sync"

        fun schedule(context: Context, intervalMinutes: Long = 60) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                intervalMinutes.coerceAtLeast(15),
                TimeUnit.MINUTES,
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
