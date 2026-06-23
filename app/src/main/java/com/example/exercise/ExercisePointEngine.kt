package com.example.exercise

import kotlin.math.ceil

/**
 * Exercise POINTS instead of fixed reps, so unlock cost is fair across exercises.
 * Squat = 1, Pushup = 1, Pullup = 3. "17 points" -> 17 squats OR 6 pullups (user chooses).
 */
object ExercisePointEngine {
    fun weight(exercise: Exercise): Int = when (exercise) {
        Exercise.SQUAT -> 1
        Exercise.PUSHUP -> 1
        Exercise.PULLUP -> 3
    }

    fun pointsFor(exercise: Exercise, reps: Int): Int = reps * weight(exercise)

    fun repsNeeded(exercise: Exercise, pointsRequired: Int): Int =
        ceil(pointsRequired.toDouble() / weight(exercise)).toInt()
}
