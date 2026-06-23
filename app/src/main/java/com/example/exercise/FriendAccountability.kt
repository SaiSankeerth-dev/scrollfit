package com.example.exercise

/**
 * Accountability, not a social network. Pure comparison + alert LOGIC.
 * NOTE: delivering an alert to another person's phone requires a backend (push).
 * This module produces the messages; a server/FCM sends them.
 */
data class FriendComparison(
    val youReels: Int, val friendReels: Int,
    val youFocus: Int, val friendFocus: Int
) {
    val reelDiff: Int get() = friendReels - youReels // positive = you scrolled less
    fun summary(): String = when {
        reelDiff > 0 -> "You saved ~$reelDiff more reels today"
        reelDiff < 0 -> "${-reelDiff} reels behind your friend today"
        else -> "Tied with your friend today"
    }
}

object FriendAccountability {
    val encouragements = listOf("💪 Stay focused", "🏋 Time for pushups", "🚀 Back to building")

    fun compare(youReels: Int, friendReels: Int, youFocus: Int, friendFocus: Int) =
        FriendComparison(youReels, friendReels, youFocus, friendFocus)

    /** Returns an alert string when the friend exceeded their goal, else null. */
    fun exceededAlert(friendName: String, reels: Int, reelLimit: Int): String? =
        if (reels > reelLimit) "$friendName exceeded today's goal (~$reels reels, limit $reelLimit). Send encouragement?"
        else null
}
