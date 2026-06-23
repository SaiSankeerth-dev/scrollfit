package com.example.tracking

import android.content.Context
import androidx.work.*
import com.example.data.ScrollFitRepository
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Midnight evaluation: update streak, award points, post morning summary. */
class DailyEvalWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val repo = ScrollFitRepository(applicationContext)
        runCatching {
            val (ig, yt) = UsageTracker.todayMinutes(applicationContext)
            repo.setTodayUsageAbsolute(ig, yt)
            repo.dailyEvaluation()
            Notifications.morningSummary(
                applicationContext,
                "Yesterday: ${ig + yt} min. New day — stay under your goal."
            )
        }
        return Result.success()
    }

    companion object {
        private const val NAME = "scrollfit_daily_eval"

        fun schedule(ctx: Context) {
            val now = Calendar.getInstance()
            val next = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 5)
                set(Calendar.SECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            val delay = next.timeInMillis - now.timeInMillis
            val req = PeriodicWorkRequestBuilder<DailyEvalWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                NAME, ExistingPeriodicWorkPolicy.UPDATE, req
            )
        }
    }
}
