package com.example.tracking

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object Notifications {
    const val CH_TRACKING = "scrollfit_tracking"
    const val CH_SUMMARY = "scrollfit_summary"
    const val TRACKING_ID = 42

    fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_TRACKING, "Tracking", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "ScrollFit is measuring your usage" }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_SUMMARY, "Daily summary", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Morning recap and goal" }
        )
    }

    private fun openAppIntent(ctx: Context): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            ctx, 0, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun trackingNotification(ctx: Context): android.app.Notification =
        NotificationCompat.Builder(ctx, CH_TRACKING)
            .setContentTitle("ScrollFit is on")
            .setContentText("Measuring your short-form usage")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(openAppIntent(ctx))
            .build()

    fun morningSummary(ctx: Context, body: String) {
        val n = NotificationCompat.Builder(ctx, CH_SUMMARY)
            .setContentTitle("Good morning")
            .setContentText(body)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(ctx))
            .build()
        if (TrackingController.hasNotifications(ctx)) {
            androidx.core.app.NotificationManagerCompat.from(ctx).notify(100, n)
        }
    }
}
