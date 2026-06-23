package com.example.tracking

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.example.data.ScrollFitRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/** Foreground service: polls real usage every 60s, updates DB, drives floating counter + lock. */
class TrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repo: ScrollFitRepository

    override fun onCreate() {
        super.onCreate()
        repo = ScrollFitRepository(applicationContext)
        Notifications.createChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                Notifications.TRACKING_ID,
                Notifications.trackingNotification(this),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(Notifications.TRACKING_ID, Notifications.trackingNotification(this))
        }
        loop()
        return START_STICKY
    }

    private fun loop() {
        scope.launch {
            while (isActive) {
                runCatching { tick() }
                delay(60_000L)
            }
        }
    }

    private suspend fun tick() {
        ServiceHealthManager.heartbeat(this)
        if (!UsageTracker.hasUsageAccess(this)) return
        val (ig, yt) = UsageTracker.todayMinutes(this)
        repo.setTodayUsageAbsolute(ig, yt)

        val settings = repo.dao.getSettings() ?: return
        val limits = repo.dao.getLimitsFlow().first()
        val igLimit = limits.find { it.platform == "instagram" }?.dailyLimitMinutes ?: 30
        val ytLimit = limits.find { it.platform == "youtube" }?.dailyLimitMinutes ?: 30

        val fg = UsageTracker.currentForeground(this)

        // Floating counter
        if (settings.floatingEnabled && fg == UsageTracker.IG) {
            OverlayManager.showFloating(this, "IG $ig/$igLimit min")
        } else if (settings.floatingEnabled && fg == UsageTracker.YT) {
            OverlayManager.showFloating(this, "YT $yt/$ytLimit min")
        } else {
            OverlayManager.hideFloating(this)
        }

        // Hardcore soft-lock when over limit AND the target app is in foreground
        if (settings.mode == "hardcore") {
            if (fg == UsageTracker.IG && ig >= igLimit) {
                OverlayManager.showLock(this, "instagram", 17)
            } else if (fg == UsageTracker.YT && yt >= ytLimit) {
                OverlayManager.showLock(this, "youtube", 17)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        OverlayManager.hideAll(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        /** Called by the lock overlay's Snooze button. */
        fun requestSnooze(ctx: Context) {
            val repo = ScrollFitRepository(ctx.applicationContext)
            CoroutineScope(Dispatchers.IO).launch { repo.applySnoozePenalty() }
        }
    }
}
