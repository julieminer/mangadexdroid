package com.melonhead.mangadexfollower.work_manager

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.melonhead.lib_app_events.AppEventsRepository
import com.melonhead.lib_app_events.events.UserEvent
import com.melonhead.lib_logging.Clog
import kotlinx.coroutines.future.await
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class RefreshWorker(
    val appContext: Context,
    workerParams: WorkerParameters
): CoroutineWorker(appContext, workerParams), KoinComponent {
    private val appEventsRepository: AppEventsRepository by inject()

    override suspend fun doWork(): Result {
        val refreshCompletion = CompletableFuture<Unit>()
        Clog.i("Refresh: refreshworker start")
        appEventsRepository.postEvent(UserEvent.RefreshManga(refreshCompletion, skipNetworkCheck = true))
        Clog.i("Refresh: refreshworker waiting")
        refreshCompletion.await()
        Clog.i("Refresh: refreshworker complete")

        scheduleDebugTask(appContext)

        return Result.success()
    }

    companion object {
        fun scheduleDebugTask(context: Context) {
            Clog.i("Refresh: Scheduling debug task")
            val debugWorkRequest = OneTimeWorkRequestBuilder<RefreshWorker>()
                .setInitialDelay(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueue(debugWorkRequest)
        }

        fun scheduleRepeatingTask(context: Context) {
            Clog.i("Refresh: Scheduling repeating task")
            val refreshWorkRequest = PeriodicWorkRequestBuilder<RefreshWorker>(
                repeatInterval = 30.minutes.toJavaDuration(),
                flexTimeInterval = 15.minutes.toJavaDuration()
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork("refresh-task", ExistingPeriodicWorkPolicy.UPDATE, refreshWorkRequest)
        }
    }
}
