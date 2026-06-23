package com.example.tracking

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/** Accelerometer-based rep counter (squats/pushups) — honest motion detection, no camera. */
class RepCounter(
    ctx: Context,
    private val target: Int,
    private val onRep: (Int) -> Unit,
    private val onDone: () -> Unit
) : SensorEventListener {

    private val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accel: Sensor? = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var count = 0
    private var armed = true
    private var lastRepTs = 0L

    fun start() { accel?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) } }
    fun stop() { sm.unregisterListener(this) }

    override fun onSensorChanged(event: SensorEvent) {
        val (x, y, z) = Triple(event.values[0], event.values[1], event.values[2])
        val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
        val now = System.currentTimeMillis()
        // Peak above ~1.4g = a strong movement; debounce 600ms; require return below 1.1g to re-arm.
        if (armed && g > 1.4f && now - lastRepTs > 600) {
            count++; lastRepTs = now; armed = false
            onRep(count)
            if (count >= target) { stop(); onDone() }
        } else if (!armed && g < 1.1f) {
            armed = true
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
