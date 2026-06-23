package com.example.exercise

enum class AttentionLevel { BRONZE, SILVER, GOLD, PLATINUM, DIAMOND }

/** Progression tiers by control-streak length. */
object AttentionLevels {
    fun forStreak(days: Int): AttentionLevel = when {
        days < 3 -> AttentionLevel.BRONZE
        days < 7 -> AttentionLevel.SILVER
        days < 14 -> AttentionLevel.GOLD
        days < 30 -> AttentionLevel.PLATINUM
        else -> AttentionLevel.DIAMOND
    }
}
