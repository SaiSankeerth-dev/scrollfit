package com.example.exercise

/**
 * Friction ladder (PRD): propose tightening the daily limit as weeks pass AND the user
 * has been consistent. Reductions are PROPOSED, never applied silently.
 * Week 1: no change (habit-building). Wk2 -10%, Wk3 -20%, Wk4+ -30%. Floor 15 min, round to 5.
 */
object AdaptiveGoalEngine {

    fun shouldPropose(week: Int, daysUnderLastWeek: Int): Boolean =
        week >= 2 && daysUnderLastWeek >= 4

    fun proposedLimit(currentLimitMinutes: Int, week: Int, daysUnderLastWeek: Int): Int {
        if (daysUnderLastWeek < 4) return currentLimitMinutes // not consistent -> don't tighten
        val pct = when (week) {
            in 0..1 -> 0.0
            2 -> 0.10
            3 -> 0.20
            else -> 0.30
        }
        val reduced = (currentLimitMinutes * (1.0 - pct)).toInt()
        return maxOf(15, (reduced / 5) * 5)
    }
}
