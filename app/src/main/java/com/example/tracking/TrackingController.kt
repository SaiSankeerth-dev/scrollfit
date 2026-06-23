package com.example.tracking

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Central place to check/request the real permissions and start the tracking service.
 * Called from MainActivity lifecycle. Keeps Compose UI untouched.
 */
object TrackingController {
    private const val PREFS = "scrollfit_perms"
    private const val ASKED_USAGE = "asked_usage"

    fun hasOverlay(ctx: Context): Boolean = Settings.canDrawOverlays(ctx)

    fun openOverlaySettings(ctx: Context) {
        ctx.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + ctx.packageName)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun hasNotifications(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            ctx, android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Call from MainActivity.onCreate. */
    fun requestRuntimePermissions(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifications(activity)) {
            ActivityCompat.requestPermissions(
                activity, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001
            )
        }
    }

    /** Call from MainActivity.onResume. Starts the service if usage access is granted. */
    fun sync(activity: Activity) {
        Notifications.createChannels(activity)
        if (UsageTracker.hasUsageAccess(activity)) {
            val intent = Intent(activity, TrackingService::class.java)
            ContextCompat.startForegroundService(activity, intent)
            DailyEvalWorker.schedule(activity)
            HealthWorker.schedule(activity)
        } else {
            // Prompt for Usage Access once, so we don't loop the user into Settings forever.
            val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(ASKED_USAGE, false)) {
                prefs.edit().putBoolean(ASKED_USAGE, true).apply()
                UsageTracker.openUsageAccessSettings(activity)
            }
        }
    }
}
