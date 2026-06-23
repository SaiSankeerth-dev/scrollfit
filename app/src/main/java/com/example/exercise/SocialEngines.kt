package com.example.exercise

/** Pure logic for invites, friend dashboard, and challenges.
 *  NOTE: cross-device connection + push DELIVERY require a backend (see docs). */

object InviteCodeEngine {
    /** {"inviteCode":"SK-8293"} */
    fun generateInviteCode(@Suppress("UNUSED_PARAMETER") userId: String): String =
        "SK-" + (1000..9999).random()

    fun isValidFormat(code: String): Boolean = Regex("^SK-\\d{4}$").matches(code)
}

/** {"savedReels":25,"winner":"you"} */
data class FriendDashboard(val savedReels: Int, val winner: String, val summary: String)

object FriendDashboardEngine {
    fun build(youReels: Int, friendReels: Int): FriendDashboard {
        val diff = friendReels - youReels
        val winner = when { diff > 0 -> "you"; diff < 0 -> "friend"; else -> "tie" }
        val summary = when {
            diff > 0 -> "You saved ~$diff more reels today"
            diff < 0 -> "${-diff} reels behind today"
            else -> "Tied today"
        }
        return FriendDashboard(savedReels = maxOf(0, diff), winner = winner, summary = summary)
    }
}

data class Challenge(val type: String, val durationDays: Int, val startedAt: Long = System.currentTimeMillis())
/** {"winner":"Sai","violations":0} */
data class ChallengeResult(val winner: String, val yourViolations: Int, val friendViolations: Int)

object ChallengeEngine {
    fun create(type: String, durationDays: Int) = Challenge(type, durationDays)

    fun result(youViolations: Int, friendViolations: Int, youName: String = "You", friendName: String = "Friend") =
        ChallengeResult(
            winner = when {
                youViolations < friendViolations -> youName
                friendViolations < youViolations -> friendName
                else -> "Tie"
            },
            yourViolations = youViolations,
            friendViolations = friendViolations
        )
}
