package com.example.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Restore tracking + scheduled jobs after reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            DailyEvalWorker.schedule(context)
            if (UsageTracker.hasUsageAccess(context)) {
                runCatching {
                    ContextCompat.startForegroundService(
                        context, Intent(context, TrackingService::class.java)
                    )
                }
            }
        }
    }
}
