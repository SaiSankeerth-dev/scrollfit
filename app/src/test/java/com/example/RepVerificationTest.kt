package com.example

import com.example.exercise.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Proves the verification engine counts full-ROM reps and rejects half-reps / shakes. */
class RepVerificationTest {

    private fun feed(sm: RepStateMachine, vararg angles: Double) { angles.forEach { sm.update(it) } }

    @Test
    fun fullDeepPushup_isCounted() {
        var counted = 0; var conf = 0
        val spec = ExerciseSpec.forWeek(Exercise.PUSHUP, week = 1)
        val sm = RepStateMachine(spec, confidenceThreshold = 80,
            onRep = { _, c -> counted++; conf = c })
        // 160 -> 92 (deep) -> 158 : a clean full-range rep
        feed(sm, 160.0, 140.0, 95.0, 92.0, 130.0, 158.0)
        assertEquals("one valid pushup", 1, sm.repCount())
        assertTrue("confidence should be high for a deep rep", conf >= 80)
        assertEquals(1, counted)
    }

    @Test
    fun halfPushup_isRejected() {
        var rejected = false
        val spec = ExerciseSpec.forWeek(Exercise.PUSHUP, week = 1)
        val sm = RepStateMachine(spec, onRep = { _, _ -> }, onReject = { rejected = true })
        // 160 -> 135 (never reaches the 115 depth) -> back to 160 : a fake half-rep
        feed(sm, 160.0, 140.0, 135.0, 160.0)
        assertEquals("half-rep must not count", 0, sm.repCount())
        assertTrue("half-rep must be rejected", rejected)
    }

    @Test
    fun escalatingCost_makesExtraReelsExpensive() {
        assertEquals(5, ExtraReelCostEngine.costForBatch(1))
        assertEquals(12, ExtraReelCostEngine.costForBatch(6))
        assertEquals(15, ExtraReelCostEngine.costForBatch(7)) // 12 + 3, capped
        assertEquals(15, ExtraReelCostEngine.costForBatch(8)) // capped at MAX_COST
        assertEquals(15, ExtraReelCostEngine.costForBatch(50)) // never exceeds cap
    }

    @Test
    fun difficultyRampsByWeek() {
        val w1 = ExerciseSpec.forWeek(Exercise.SQUAT, 1).bottomAngle
        val w4 = ExerciseSpec.forWeek(Exercise.SQUAT, 4).bottomAngle
        assertTrue("week 4 must demand a deeper squat than week 1", w4 < w1)
    }

    @Test
    fun fullPullup_isCounted_halfPullup_isNot() {
        val spec = ExerciseSpec.forWeek(Exercise.PULLUP, week = 1)
        var counted = 0
        val sm = RepStateMachine(spec, confidenceThreshold = 80, onRep = { _, _ -> counted++ })
        listOf(0.0, 0.05, 0.13, 0.0).forEach { sm.update(it) }
        assertEquals(1, sm.repCount())

        val sm2 = RepStateMachine(spec, confidenceThreshold = 80, onRep = { _, _ -> })
        listOf(0.0, 0.03, 0.0).forEach { sm2.update(it) }
        assertEquals(0, sm2.repCount())
    }

    @Test
    fun exercisePoints_makeUnlockFairAcrossExercises() {
        assertEquals(17, ExercisePointEngine.repsNeeded(Exercise.SQUAT, 17))
        assertEquals(17, ExercisePointEngine.repsNeeded(Exercise.PUSHUP, 17))
        assertEquals(6, ExercisePointEngine.repsNeeded(Exercise.PULLUP, 17)) // ceil(17/3)
        assertEquals(15, ExercisePointEngine.pointsFor(Exercise.PULLUP, 5))
    }

    @Test
    fun attentionLevels_progress() {
        assertEquals(AttentionLevel.BRONZE, AttentionLevels.forStreak(0))
        assertEquals(AttentionLevel.GOLD, AttentionLevels.forStreak(7))
        assertEquals(AttentionLevel.DIAMOND, AttentionLevels.forStreak(30))
    }

    @Test
    fun motivationStats_reelsAvoided() {
        assertEquals(20, MotivationStats.minutesSaved(60, 40))
        assertEquals(40, MotivationStats.reelsAvoided(60, 40)) // 20 min * 60 / 30
        assertEquals(0, MotivationStats.minutesSaved(40, 60)) // over baseline -> 0
    }

    @Test
    fun adaptiveGoal_proposesReductionOnlyWhenConsistent() {
        assertEquals(50, AdaptiveGoalEngine.proposedLimit(60, week = 2, daysUnderLastWeek = 5))
        assertEquals(40, AdaptiveGoalEngine.proposedLimit(60, week = 4, daysUnderLastWeek = 5))
        assertEquals(60, AdaptiveGoalEngine.proposedLimit(60, week = 4, daysUnderLastWeek = 2))
        assertTrue(AdaptiveGoalEngine.shouldPropose(2, 4))
        assertTrue(!AdaptiveGoalEngine.shouldPropose(1, 7))
    }

    @Test
    fun fullDeepSquat_isCounted_shallowSquat_isNot() {
        val spec = ExerciseSpec.forWeek(Exercise.SQUAT, week = 4) // strict: needs 90
        var counted = 0
        val sm = RepStateMachine(spec, confidenceThreshold = 80, onRep = { _, _ -> counted++ })
        listOf(170.0, 140.0, 95.0, 90.0, 130.0, 168.0).forEach { sm.update(it) }
        assertEquals(1, sm.repCount())

        var rejected = false
        val sm2 = RepStateMachine(spec, onRep = { _, _ -> }, onReject = { rejected = true })
        listOf(170.0, 150.0, 170.0).forEach { sm2.update(it) } // shallow
        assertEquals(0, sm2.repCount())
        assertTrue(rejected)
    }

    @Test
    fun attentionXp_matchesSpecExamples() {
        assertEquals(AttentionLevel.BRONZE, AttentionXpEngine.progress(82, 12, 8).tier)   // 102 XP
        assertEquals(AttentionLevel.GOLD, AttentionXpEngine.progress(92, 40, 40).tier)    // 172 XP
        assertEquals(102, AttentionXpEngine.xp(82, 12, 8))
    }

    @Test
    fun friendAccountability_comparisonAndAlert() {
        assertTrue(FriendAccountability.compare(35, 60, 88, 62).summary().contains("25"))
        assertEquals(null, FriendAccountability.exceededAlert("Sai", 40, 50))
        assertTrue(FriendAccountability.exceededAlert("Sai", 90, 50)!!.contains("exceeded"))
    }

    @Test
    fun inviteCode_hasCorrectFormat() {
        val code = InviteCodeEngine.generateInviteCode("123")
        assertTrue(InviteCodeEngine.isValidFormat(code))
        assertTrue(code.startsWith("SK-"))
        assertTrue(!InviteCodeEngine.isValidFormat("SK-12"))
    }

    @Test
    fun friendDashboard_matchesSpec() {
        val d = FriendDashboardEngine.build(youReels = 35, friendReels = 60)
        assertEquals(25, d.savedReels)
        assertEquals("you", d.winner)
    }

    @Test
    fun challenge_winnerIsFewestViolations() {
        val r = ChallengeEngine.result(youViolations = 0, friendViolations = 3, youName = "Sai", friendName = "Rahul")
        assertEquals("Sai", r.winner)
        assertEquals(0, r.yourViolations)
    }

}
