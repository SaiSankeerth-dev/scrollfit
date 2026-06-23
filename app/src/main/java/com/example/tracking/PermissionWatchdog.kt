package com.example.tracking

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** P0: detect disabled permissions every launch so the app never silently fails. */
data class PermissionState(
    val usageAccess: Boolean,
    val overlay: Boolean,
    val notifications: Boolean,
    val camera: Boolean,
    val batteryUnrestricted: Boolean
) {
    /** Tracking cannot function without these two. */
    val criticalMissing: Boolean get() = !usageAccess || !overlay
    val anyMissing: Boolean get() = !usageAccess || !overlay || !notifications || !batteryUnrestricted
}

object PermissionWatchdog {
    fun check(ctx: Context): PermissionState = PermissionState(
        usageAccess = UsageTracker.hasUsageAccess(ctx),
        overlay = TrackingController.hasOverlay(ctx),
        notifications = TrackingController.hasNotifications(ctx),
        camera = ContextCompat.checkSelfPermission(
            ctx, android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED,
        batteryUnrestricted = BatteryOptimizationHelper.isIgnoring(ctx)
    )

    /** Human-readable list of what to enable, most important first. */
    fun missingActions(s: PermissionState): List<String> = buildList {
        if (!s.usageAccess) add("Enable Usage Access (tracking is paused)")
        if (!s.overlay) add("Allow display over other apps (counter/lock disabled)")
        if (!s.batteryUnrestricted) add("Disable battery optimization (service may be killed)")
        if (!s.notifications) add("Allow notifications (reminders disabled)")
    }
}
