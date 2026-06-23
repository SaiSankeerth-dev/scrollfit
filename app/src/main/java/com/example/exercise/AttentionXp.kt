package com.example.exercise

/** Game-style progression: XP = Focus Score + Streak + Time-Saved hours. */
data class AttentionProgress(
    val xp: Int,
    val tier: AttentionLevel,
    val numericLevel: Int,
    val nextTierXp: Int,
    val xpToNextTier: Int
)

object AttentionXpEngine {

    fun xp(focusScore: Int, streakDays: Int, timeSavedHours: Int): Int =
        focusScore + streakDays + timeSavedHours

    // Tier lower bounds chosen so the spec examples hold (102 -> Bronze, 172 -> Gold).
    private val tierFloors = listOf(
        0 to AttentionLevel.BRONZE,
        120 to AttentionLevel.SILVER,
        160 to AttentionLevel.GOLD,
        280 to AttentionLevel.PLATINUM,
        450 to AttentionLevel.DIAMOND
    )

    fun progress(focusScore: Int, streakDays: Int, timeSavedHours: Int): AttentionProgress {
        val xp = xp(focusScore, streakDays, timeSavedHours)
        val tier = tierFloors.last { xp >= it.first }.second
        val nextFloor = tierFloors.firstOrNull { it.first > xp }?.first ?: tierFloors.last().first
        return AttentionProgress(
            xp = xp,
            tier = tier,
            numericLevel = 1 + xp / 15,
            nextTierXp = nextFloor,
            xpToNextTier = maxOf(0, nextFloor - xp)
        )
    }
}
