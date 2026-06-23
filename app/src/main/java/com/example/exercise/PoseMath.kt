package com.example.exercise

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

object PoseMath {
    /** Angle in degrees at vertex b formed by a-b-c. */
    fun angle(a: Landmark, b: Landmark, c: Landmark): Double {
        val baX = (a.x - b.x).toDouble(); val baY = (a.y - b.y).toDouble()
        val bcX = (c.x - b.x).toDouble(); val bcY = (c.y - b.y).toDouble()
        val dot = baX * bcX + baY * bcY
        val magBa = sqrt(baX * baX + baY * baY)
        val magBc = sqrt(bcX * bcX + bcY * bcY)
        if (magBa == 0.0 || magBc == 0.0) return 180.0
        val cos = (dot / (magBa * magBc)).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cos))
    }

    /** Vertical distance (normalized 0..1 coords); positive when [a] is above [b]. */
    fun heightAbove(a: Landmark, b: Landmark): Double = (b.y - a.y).toDouble()

    fun stdev(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        return sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size)
    }

    fun absDelta(a: Double, b: Double) = abs(a - b)
}
