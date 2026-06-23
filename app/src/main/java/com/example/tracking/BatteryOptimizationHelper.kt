package com.example.tracking

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/** P0: OEMs (Xiaomi/Vivo/Oppo/Realme) kill background services unless exempted. */
object BatteryOptimizationHelper {

    fun isIgnoring(ctx: Context): Boolean {
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(ctx.packageName)
    }

    /** Opens the system "ignore battery optimization" prompt for this app. */
    fun requestIgnore(ctx: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:" + ctx.packageName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ctx.startActivity(intent) }.onFailure {
            // Fallback to the general battery settings list
            runCatching {
                ctx.startActivity(
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    /** Device-specific autostart guidance (OEM settings live outside standard Android). */
    fun oemGuide(): String {
        val m = Build.MANUFACTURER.lowercase()
        return when {
            "xiaomi" in m || "redmi" in m || "poco" in m ->
                "Xiaomi/MIUI: Settings > Apps > ScrollFit > Autostart ON; Battery saver > No restrictions."
            "vivo" in m -> "Vivo: Settings > Battery > Background power consumption > allow ScrollFit; enable Autostart."
            "oppo" in m || "realme" in m ->
                "Oppo/Realme: Settings > Battery > App battery management > ScrollFit > Allow background + Autostart."
            "samsung" in m ->
                "Samsung: Settings > Battery > Background usage limits > remove ScrollFit from 'Sleeping apps'."
            else -> "Allow this app to run in the background and disable battery optimization for reliable tracking."
        }
    }
}
