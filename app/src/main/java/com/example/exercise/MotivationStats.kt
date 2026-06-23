package com.example.exercise

import kotlin.math.max

/** "Time saved" and "reels avoided" are more motivating than raw counts. */
object MotivationStats {
    private const val SEC_PER_REEL = 30

    /** Minutes saved today vs the user's recent baseline. */
    fun minutesSaved(baselineMinutes: Int, actualMinutes: Int): Int =
        max(0, baselineMinutes - actualMinutes)

    /** Estimated reels avoided (labelled with ~ in UI). */
    fun reelsAvoided(baselineMinutes: Int, actualMinutes: Int): Int =
        max(0, (baselineMinutes - actualMinutes) * 60 / SEC_PER_REEL)
}
