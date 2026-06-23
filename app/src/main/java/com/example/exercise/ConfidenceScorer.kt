package com.example.exercise

import kotlin.math.max
import kotlin.math.min

/** 0..100 confidence from depth, range-of-motion, and stability. */
object ConfidenceScorer {
    fun score(spec: ExerciseSpec, minReached: Double, maxReached: Double, samples: List<Double>): Int {
        return when (spec.metric) {
            MetricType.ANGLE -> {
                // Depth: how close the deepest angle got to ideal bottom (smaller = deeper).
                // Depth scaled across the full top->ideal range (works even when bottom == ideal).
                val depth = ((spec.topAngle - minReached) / max(1.0, spec.topAngle - spec.idealBottom)).coerceIn(0.0, 1.0)
                val rom = ((maxReached - minReached) / max(1.0, spec.topAngle - spec.idealBottom)).coerceIn(0.0, 1.0)
                val stability = (1.0 - min(1.0, PoseMath.stdev(samples) / 60.0))
                combine(depth, rom, stability)
            }
            MetricType.HEIGHT -> {
                val reach = (maxReached / max(0.0001, spec.idealBottom)).coerceIn(0.0, 1.0)
                val rom = reach
                val stability = (1.0 - min(1.0, PoseMath.stdev(samples) / 0.2))
                combine(reach, rom, stability)
            }
        }
    }

    private fun combine(depth: Double, rom: Double, stability: Double): Int {
        val s = 100.0 * (0.5 * depth + 0.3 * rom + 0.2 * stability)
        return s.toInt().coerceIn(0, 100)
    }
}
