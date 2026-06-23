package com.example.exercise

/**
 * Per-exercise thresholds with WEEKLY ADAPTIVE DIFFICULTY.
 * Week 1 builds the habit (lenient ROM); Week 4+ demands full form.
 */
data class ExerciseSpec(
    val exercise: Exercise,
    val metric: MetricType,
    val topAngle: Double,       // extended position (e.g. standing/arms straight)
    val bottomAngle: Double,    // required flexion to count a rep (adaptive)
    val idealBottom: Double,    // perfect-form bottom (for confidence)
    val minVisibility: Float = 0.5f
) {
    companion object {
        /** Adaptive: week 1 lenient, ramping to strict by week 4+. */
        fun forWeek(exercise: Exercise, week: Int): ExerciseSpec {
            val w = week.coerceIn(1, 4)
            return when (exercise) {
                Exercise.SQUAT -> {
                    val bottom = when (w) { 1 -> 120.0; 2 -> 110.0; 3 -> 100.0; else -> 90.0 }
                    ExerciseSpec(exercise, MetricType.ANGLE, topAngle = 170.0, bottomAngle = bottom, idealBottom = 90.0)
                }
                Exercise.PUSHUP -> {
                    val bottom = when (w) { 1 -> 115.0; 2 -> 105.0; 3 -> 98.0; else -> 90.0 }
                    ExerciseSpec(exercise, MetricType.ANGLE, topAngle = 160.0, bottomAngle = bottom, idealBottom = 90.0)
                }
                Exercise.PULLUP -> // HEIGHT metric: bottom/top reused as normalized head-above-wrist deltas
                    ExerciseSpec(exercise, MetricType.HEIGHT, topAngle = 0.02, bottomAngle = if (w >= 4) 0.10 else 0.05, idealBottom = 0.12)
            }
        }
    }
}
