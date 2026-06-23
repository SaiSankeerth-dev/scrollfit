package com.example.tracking

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

/** P0: heartbeat + watchdog that restarts the tracking service if it dies. */
object ServiceHealthManager {
    private const val PREF = "scrollfit_health"
    private const val KEY_TICK = "last_tick"
    private const val STALE_MS = 3 * 60 * 1000L // 3 minutes

    fun heartbeat(ctx: Context) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putLong(KEY_TICK, System.currentTimeMillis()).apply()
    }

    fun isStale(ctx: Context): Boolean {
        val last = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong(KEY_TICK, 0L)
        return last != 0L && System.currentTimeMillis() - last > STALE_MS
    }
}

class HealthWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        if (UsageTracker.hasUsageAccess(applicationContext) && ServiceHealthManager.isStale(applicationContext)) {
            runCatching {
                ContextCompat.startForegroundService(
                    applicationContext, Intent(applicationContext, TrackingService::class.java)
                )
            }
        }
        return Result.success()
    }

    companion object {
        private const val NAME = "scrollfit_health_watchdog"
        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<HealthWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                NAME, ExistingPeriodicWorkPolicy.UPDATE, req
            )
        }
    }
}
