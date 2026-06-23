package com.example.exercise

/**
 * After the daily limit, each extra batch of reels costs progressively more reps,
 * so the user voluntarily stops ("this reel isn't worth 12 pushups").
 * Ladder: 5, 6, 7, 8, 10, 12, then +3 each subsequent batch.
 */
object ExtraReelCostEngine {
    private val ladder = intArrayOf(5, 6, 7, 8, 10, 12)

    /** Cost (reps) for the Nth extra batch after the limit (n starts at 1). */
    /** Capped at MAX_COST so extra-reel cost never spirals into uninstall territory. */
    const val MAX_COST = 15

    fun costForBatch(n: Int): Int {
        if (n <= 0) return 0
        val raw = if (n <= ladder.size) ladder[n - 1] else ladder.last() + (n - ladder.size) * 3
        return minOf(MAX_COST, raw)
    }
}
