package com.example.tracking

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity

/** Real system overlays via WindowManager. Floating counter + dismissible soft-lock. */
object OverlayManager {
    private var floatingView: View? = null
    private var lockView: View? = null

    private fun wm(ctx: Context) =
        ctx.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    fun showFloating(ctx: Context, text: String) {
        if (!Settings_canDraw(ctx)) return
        val app = ctx.applicationContext
        if (floatingView != null) {
            (floatingView as? TextView)?.text = text
            return
        }
        val tv = TextView(app).apply {
            this.text = text
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#CC0F172A"))
            setPadding(24, 14, 24, 14)
            textSize = 12f
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 24; y = 180
        }
        floatingView = tv
        runCatching { wm(app).addView(tv, params) }
    }

    fun hideFloating(ctx: Context) {
        floatingView?.let { runCatching { wm(ctx).removeView(it) } }
        floatingView = null
    }

    fun showLock(ctx: Context, platform: String, target: Int) {
        if (!Settings_canDraw(ctx) || lockView != null) return
        val app = ctx.applicationContext
        val root = LinearLayout(app).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F27F1D1D"))
            gravity = Gravity.CENTER
            setPadding(60, 120, 60, 120)
        }
        root.addView(TextView(app).apply {
            text = "${platform.replaceFirstChar { it.uppercase() }} limit reached"
            setTextColor(Color.WHITE); textSize = 22f; gravity = Gravity.CENTER
        })
        root.addView(TextView(app).apply {
            text = "Complete $target reps to dismiss — or snooze."
            setTextColor(Color.parseColor("#FECACA")); textSize = 14f
            gravity = Gravity.CENTER; setPadding(0, 16, 0, 32)
        })
        root.addView(Button(app).apply {
            text = "Start exercise"
            setOnClickListener {
                hideLock(app)
                app.startActivity(
                    Intent(app, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .putExtra("open_exercise", platform)
                )
            }
        })
        root.addView(Button(app).apply {
            text = "Snooze 5 min (costs Focus)"
            setOnClickListener {
                hideLock(app)
                TrackingService.requestSnooze(app)
            }
        })

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        lockView = root
        runCatching { wm(app).addView(root, params) }
    }

    fun hideLock(ctx: Context) {
        lockView?.let { runCatching { wm(ctx).removeView(it) } }
        lockView = null
    }

    fun hideAll(ctx: Context) { hideFloating(ctx); hideLock(ctx) }

    private fun Settings_canDraw(ctx: Context): Boolean =
        android.provider.Settings.canDrawOverlays(ctx)
}
