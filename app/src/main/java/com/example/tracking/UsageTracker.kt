package com.example.tracking

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import java.util.Calendar

/** Real device usage tracking via UsageStatsManager (no Accessibility scraping). */
object UsageTracker {
    const val IG = "com.instagram.android"
    const val YT = "com.google.android.youtube"

    fun hasUsageAccess(ctx: Context): Boolean {
        val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings(ctx: Context) {
        ctx.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun startOfDay(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    /** Foreground minutes today: Pair(instagramMinutes, youtubeMinutes). */
    fun todayMinutes(ctx: Context): Pair<Int, Int> {
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay(), now)
            ?: return 0 to 0
        var igMs = 0L; var ytMs = 0L
        for (s in stats) {
            when (s.packageName) {
                IG -> igMs += s.totalTimeInForeground
                YT -> ytMs += s.totalTimeInForeground
            }
        }
        return (igMs / 60000L).toInt() to (ytMs / 60000L).toInt()
    }

    /** Most-recent foreground package within the lookback window (for live lock/pause triggers). */
    fun currentForeground(ctx: Context, lookbackMs: Long = 12000L): String? {
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - lookbackMs, now)
        val e = UsageEvents.Event()
        var last: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) last = e.packageName
        }
        return last
    }
}
