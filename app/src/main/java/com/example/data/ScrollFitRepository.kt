package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class ScrollFitRepository(private val context: Context) {

    private val database: ScrollFitDatabase = getDatabase(context)

    val dao = database.dao

    // Helper functions
    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // Get live elements
    val settingsFlow: Flow<SettingsEntity?> = dao.getSettingsFlow()
    val limitsFlow: Flow<List<LimitEntity>> = dao.getLimitsFlow()
    val dailyUsageFlow: Flow<List<DailyUsageEntity>> = dao.getDailyUsageFlow()
    val scoreComponentsFlow: Flow<List<FocusScoreComponentEntity>> = dao.getFocusScoreComponentsFlow()
    val streaksFlow: Flow<StreakEntity?> = dao.getStreaksFlow()
    val pointsLedgerFlow: Flow<List<PointsLedgerEntity>> = dao.getPointsLedgerFlow()
    val checkinsFlow: Flow<List<CheckInEntity>> = dao.getCheckinsFlow()
    val exercisesFlow: Flow<List<ExerciseEntity>> = dao.getExercisesFlow()
    val scrollDebtFlow: Flow<List<ScrollDebtEntity>> = dao.getScrollDebtFlow()
    val coachInsightsFlow: Flow<List<CoachInsightEntity>> = dao.getCoachInsightsFlow()
    val intentLogsFlow: Flow<List<IntentLogEntity>> = dao.getIntentLogsFlow()
    val achievementsFlow: Flow<List<AchievementEntity>> = dao.getAchievementsFlow()
    val friendsFlow: Flow<List<FriendEntity>> = dao.getFriendsFlow()

    // Initialize default values if missing
    suspend fun initializeDefaults() = withContext(Dispatchers.IO) {
        // Settings
        val existingSettings = dao.getSettings()
        if (existingSettings == null) {
            dao.saveSettings(SettingsEntity())
        }

        // Limits
        val existingLimits = dao.getLimitsFlow().firstOrNull() ?: emptyList()
        if (existingLimits.isEmpty()) {
            dao.saveLimit(LimitEntity(platform = "instagram", dailyLimitMinutes = 30, reelLimit = 60))
            dao.saveLimit(LimitEntity(platform = "youtube", dailyLimitMinutes = 30, reelLimit = 60))
        }

        // Streaks
        val existingStreaks = dao.getStreaks()
        if (existingStreaks == null) {
            dao.saveStreaks(StreakEntity(currentStreak = 3, bestStreak = 7, freezeDaysRemaining = 1))
        }

        // Add initial points if ledger is empty
        val ledger = dao.getPointsLedgerFlow().firstOrNull() ?: emptyList()
        if (ledger.isEmpty()) {
            val today = getTodayDateString()
            dao.addPointsLedger(PointsLedgerEntity(date = today, points = 100, source = "Welcome Reward", balanceAfter = 100))
        }

        // Setup some mock insights if empty
        val insights = dao.getCoachInsightsFlow().firstOrNull() ?: emptyList()
        if (insights.isEmpty()) {
            val today = getTodayDateString()
            dao.saveCoachInsight(CoachInsightEntity(
                date = today,
                type = "late_night",
                message = "You scroll reels late at night. Limiting Instagram after 10 PM can save 45 minutes of sleep.",
                suggestedAction = "Enable 10 PM lock",
                accepted = false
            ))
            dao.saveCoachInsight(CoachInsightEntity(
                date = today,
                type = "peak_time",
                message = "Peak scroll wind happens around 2 PM. Try replacing it with a 2-min mindfulness stretch.",
                suggestedAction = "Schedule afternoon breathing nudge",
                accepted = false
            ))
        }

        // Ensure achievements are prefilled
        val achievements = dao.getAchievementsFlow().firstOrNull() ?: emptyList()
        if (achievements.isEmpty()) {
            dao.saveAchievement(AchievementEntity(1, "first_step", "First Step", "Logged your first awareness limit", "shield", true, System.currentTimeMillis()))
            dao.saveAchievement(AchievementEntity(2, "under_control", "Under Control", "Stayed under and earned check-in points", "check_circle", false, 0))
            dao.saveAchievement(AchievementEntity(3, "squats_champion", "Squats Boss", "Cleared 50 limit-breaking squats", "fitness_center", false, 0))
            dao.saveAchievement(AchievementEntity(4, "streak_warrior", "Streak Warrior", "Maintained a 7-day focus streak", "workspace_premium", false, 0))
        }

        // Initialize today's empty usage
        getOrCreateTodayUsage()
    }

    // Get or Create Daily Usage Row for Today
    suspend fun getOrCreateTodayUsage(): DailyUsageEntity = withContext(Dispatchers.IO) {
        val today = getTodayDateString()
        val existing = dao.getDailyUsageForDate(today)
        if (existing != null) {
            return@withContext existing
        }

        // Create new daily usage row
        val limits = dao.getLimitsFlow().firstOrNull() ?: emptyList()
        val combinedLimit = limits.sumOf { it.dailyLimitMinutes }
        
        val newUsage = DailyUsageEntity(
            date = today,
            instagramMinutes = 0,
            youtubeMinutes = 0,
            instagramEstReels = 0,
            youtubeEstShorts = 0,
            totalMinutes = 0,
            goalMet = true,
            focusScore = 100,
            hoursSaved = 1.2 // Average baseline hours saved initially
        )
        dao.saveDailyUsage(newUsage)

        // Initialize empty components ledger too
        val newComponents = FocusScoreComponentEntity(
            date = today,
            base = 50,
            underLimit = 25,
            healthyAction = 0,
            earlyDiscipline = 15,
            lateNightPenalty = 0,
            overagePenalty = 0,
            total = 90
        )
        dao.saveFocusScoreComponents(newComponents)
        
        return@withContext newUsage
    }

    // Increment Usage Stats (for live simulations or background tracking)
    suspend fun incrementUsage(platform: String, minutes: Int, lateNight: Boolean = false) = withContext(Dispatchers.IO) {
        val today = getTodayDateString()
        val usage = getOrCreateTodayUsage()
        
        val updatedUsage = when (platform) {
            "instagram" -> {
                val newMinutes = usage.instagramMinutes + minutes
                val estReels = (newMinutes * 60) / 30 // seconds divided by 30 => minutes * 2
                usage.copy(
                    instagramMinutes = newMinutes,
                    instagramEstReels = estReels,
                    totalMinutes = usage.totalMinutes + minutes
                )
            }
            "youtube" -> {
                val newMinutes = usage.youtubeMinutes + minutes
                val estShorts = (newMinutes * 60) / 35 // seconds divided by 35 => minutes * 1.7
                usage.copy(
                    youtubeMinutes = newMinutes,
                    youtubeEstShorts = estShorts,
                    totalMinutes = usage.totalMinutes + minutes
                )
            }
            else -> usage
        }

        // Recompute focus score and save
        recomputeTodayFocusScore(updatedUsage, lateNight)
    }

    // Get cumulative points balance
    suspend fun getPointsBalance(): Int = withContext(Dispatchers.IO) {
        val ledger = dao.getPointsLedgerFlow().firstOrNull() ?: emptyList()
        return@withContext if (ledger.isEmpty()) 0 else ledger.first().balanceAfter
    }

    // Add points to ledger
    suspend fun addPoints(amount: Int, source: String) = withContext(Dispatchers.IO) {
        val today = getTodayDateString()
        val currentBalance = getPointsBalance()
        val newBalance = currentBalance + amount
        dao.addPointsLedger(
            PointsLedgerEntity(
                date = today,
                points = amount,
                source = source,
                balanceAfter = newBalance
            )
        )
    }

    // Recompute Focus Score live based on standard formula (§1.7)
    suspend fun recomputeTodayFocusScore(currentUsage: DailyUsageEntity, isLateNightActive: Boolean) = withContext(Dispatchers.IO) {
        val today = getTodayDateString()
        val limits = dao.getLimitsFlow().firstOrNull() ?: emptyList()
        
        val igLimit = limits.find { it.platform == "instagram" }?.dailyLimitMinutes ?: 30
        val ytLimit = limits.find { it.platform == "youtube" }?.dailyLimitMinutes ?: 30
        val combinedGoal = igLimit + ytLimit
        
        val totalMinutes = currentUsage.totalMinutes
        val goalRatio = if (combinedGoal > 0) totalMinutes.toDouble() / combinedGoal else 0.0

        // 1. Base Score
        val base = 50

        // 2. Stayed Under Limit Credit: +25 * (1 - min(total/goal, 1))
        val ratioClamped = min(goalRatio, 1.0)
        val underLimitCredit = (25 * (1.0 - ratioClamped)).toInt()

        // 3. Healthy Action Credit: +10 * min(exercises_or_breaks, 3)/3
        val todayExercises = dao.getExercisesFlow().firstOrNull()?.filter { it.date == today } ?: emptyList()
        // Accumulate active breaks + exercises
        val exerciseCount = todayExercises.size
        val healthyActionCredit = (10 * min(exerciseCount, 3)) / 3

        // 4. Early Day Discipline: +15 * early_day_discipline (1.0 if under pace by midday, else scaled)
        // For our simulated environment, let's keep this as 15 points if under 80% of goal, scaling down is optional.
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isEarly = hour < 14 // Before 2 PM
        val earlyDisciplineCredit = if (isEarly && goalRatio < 0.5) {
            15
        } else if (goalRatio < 0.8) {
            10
        } else {
            5
        }

        // 5. Late Night Scroll Penalty: -15 if late night scroll over threshold
        val lateNightPenalty = if (isLateNightActive || (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 23 && currentUsage.instagramMinutes > 15)) {
            15
        } else {
            0
        }

        // 6. Overage Penalty: -20 * max(0, total/goal - 1)
        val overageMultiplier = max(0.0, goalRatio - 1.0)
        val overagePenalty = min(30, (20 * overageMultiplier).toInt())

        // Calculate compiled sum
        val rawSum = base + underLimitCredit + healthyActionCredit + earlyDisciplineCredit - lateNightPenalty - overagePenalty
        val finalScore = max(0, min(100, rawSum))

        // Save components representation for auditing / breakdown UI
        val components = FocusScoreComponentEntity(
            date = today,
            base = base,
            underLimit = underLimitCredit,
            healthyAction = healthyActionCredit,
            earlyDiscipline = earlyDisciplineCredit,
            lateNightPenalty = lateNightPenalty,
            overagePenalty = overagePenalty,
            total = finalScore
        )
        dao.saveFocusScoreComponents(components)

        // Save updated daily usage row
        val isGoalMet = totalMinutes <= combinedGoal
        val finalUsage = currentUsage.copy(
            focusScore = finalScore,
            goalMet = isGoalMet,
            hoursSaved = max(0.0, ((combinedGoal - totalMinutes).toDouble() / 60.0))
        )
        dao.saveDailyUsage(finalUsage)

        // Live calculate Scroll Debt if Instagram Reel limit is broken
        val igReelLimit = limits.find { it.platform == "instagram" }?.reelLimit ?: 60
        val estReels = currentUsage.instagramEstReels
        val debtSquats = ceil(max(0, estReels - igReelLimit) * 0.34).toInt()

        if (debtSquats > 0) {
            val existingDebt = dao.getScrollDebtForDate(today)
            val updatedDebt = ScrollDebtEntity(
                id = existingDebt?.id ?: 0,
                date = today,
                squatsOwed = debtSquats,
                squatsCleared = existingDebt?.squatsCleared ?: 0,
                expired = false
            )
            dao.saveScrollDebt(updatedDebt)
        }
    }

    // Complete Morning Check-in
    suspend fun completeCheckIn(goalType: String) = withContext(Dispatchers.IO) {
        val today = getTodayDateString()
        val existing = dao.getCheckInForDate(today)
        if (existing?.completed == true) return@withContext

        val checkIn = CheckInEntity(
            id = existing?.id ?: 0,
            date = today,
            selectedGoal = goalType,
            completed = true,
            rewardPoints = 5
        )
        dao.recordCheckIn(checkIn)
        addPoints(5, "Morning Check-In ($goalType)")
    }

    // Record Exercise Completed (Squats / Pushups etc.)
    suspend fun completeExercise(type: String, reps: Int, verifiedBy: String) = withContext(Dispatchers.IO) {
        val today = getTodayDateString()
        
        // Save Exercise log
        dao.recordExercise(
            ExerciseEntity(
                date = today,
                type = type,
                repsTarget = reps,
                repsDone = reps,
                verifiedBy = verifiedBy,
                pointsAwarded = 15
            )
        )

        // Award Momentum Points (+15 points reward per exercise completed)
        addPoints(15, "Completed $reps $type ($verifiedBy)")

        // Clear any Scroll Debt for today if squats were done
        if (type.lowercase() == "squats") {
            val debt = dao.getScrollDebtForDate(today)
            if (debt != null) {
                val updatedCleared = min(debt.squatsOwed, debt.squatsCleared + reps)
                dao.saveScrollDebt(
                    debt.copy(
                        squatsCleared = updatedCleared,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }

        // Trigger Recomposition
        val usage = getOrCreateTodayUsage()
        recomputeTodayFocusScore(usage, false)
    }

    // Simulate standard midnight transition (§2.5)
    suspend fun simulateMidnightReset() = withContext(Dispatchers.IO) {
        val today = getTodayDateString()
        val usage = getOrCreateTodayUsage()
        val streaks = dao.getStreaks() ?: StreakEntity()

        // 1. Evaluate today's goals to update streaks
        val limits = dao.getLimitsFlow().firstOrNull() ?: emptyList()
        val combinedGoal = limits.sumOf { it.dailyLimitMinutes }
        val goalMet = usage.totalMinutes <= combinedGoal

        val (newStreak, newFreeze, isFreezeUsed) = if (goalMet) {
            Triple(streaks.currentStreak + 1, streaks.freezeDaysRemaining, false)
        } else {
            if (streaks.freezeDaysRemaining > 0) {
                // Consume a streak freeze day
                Triple(streaks.currentStreak, streaks.freezeDaysRemaining - 1, true)
            } else {
                // Streak reset
                Triple(0, streaks.freezeDaysRemaining, false)
            }
        }

        // Update streak record
        val best = max(streaks.bestStreak, newStreak)
        dao.saveStreaks(
            streaks.copy(
                currentStreak = newStreak,
                bestStreak = best,
                freezeDaysRemaining = if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) 1 else newFreeze, // refill weekly
                lastEvaluatedDate = today,
                updatedAt = System.currentTimeMillis()
            )
        )

        // 2. Award end of day points if goal is met
        if (goalMet) {
            addPoints(10, "Stayed under goal reward")
        }
        if (newStreak == 7) addPoints(25, "7-day streak milestone")
        if (newStreak == 30) addPoints(100, "30-day streak milestone")

        // Unlock achievements check
        if (newStreak >= 7) {
            val achievements = dao.getAchievementsFlow().firstOrNull() ?: emptyList()
            val warrior = achievements.find { it.key == "streak_warrior" }
            if (warrior != null && !warrior.unlocked) {
                dao.saveAchievement(warrior.copy(unlocked = true, unlockedAt = System.currentTimeMillis()))
                addPoints(25, "Unlocked achievement: Streak Warrior")
            }
        }

        // Expire today's scroll debt
        val debt = dao.getScrollDebtForDate(today)
        if (debt != null && debt.squatsCleared < debt.squatsOwed) {
            dao.saveScrollDebt(debt.copy(expired = true, updatedAt = System.currentTimeMillis()))
            // Apply Focus Score penalty to the day components
            val comp = dao.getComponentsForDate(today)
            if (comp != null) {
                val updatedPenalty = comp.overagePenalty + 20
                dao.saveFocusScoreComponents(comp.copy(overagePenalty = updatedPenalty, total = max(0, comp.total - 20)))
            }
        }

        // Log notification log
        dao.addNotificationLog(NotificationLogEntity(type = "morning_summary"))

        // Create a fake yesterday record inside history and clear live metrics to simulate next day!
        // Move current record to a historic date by generating a random day (e.g. yesterday)
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        val yesterdayUsage = usage.copy(id = 0, date = yesterdayStr)
        dao.saveDailyUsage(yesterdayUsage)

        // Clear current today usage with 0s
        val blankToday = DailyUsageEntity(
            id = usage.id,
            date = today,
            instagramMinutes = 0,
            youtubeMinutes = 0,
            instagramEstReels = 0,
            youtubeEstShorts = 0,
            totalMinutes = 0,
            goalMet = true,
            focusScore = 100,
            hoursSaved = combinedGoal.toDouble() / 60.0
        )
        dao.saveDailyUsage(blankToday)

        val newComponents = FocusScoreComponentEntity(
            date = today,
            base = 50,
            underLimit = 25,
            healthyAction = 0,
            earlyDiscipline = 15,
            lateNightPenalty = 0,
            overagePenalty = 0,
            total = 90
        )
        dao.saveFocusScoreComponents(newComponents)
    }

    // ---- Real-tracking integration (called by TrackingService / DailyEvalWorker) ----

    /** Set today's absolute foreground minutes from UsageStatsManager and recompute. */
    suspend fun setTodayUsageAbsolute(igMin: Int, ytMin: Int) = withContext(Dispatchers.IO) {
        val usage = getOrCreateTodayUsage()
        val updated = usage.copy(
            instagramMinutes = igMin,
            youtubeMinutes = ytMin,
            instagramEstReels = (igMin * 60) / 30,
            youtubeEstShorts = (ytMin * 60) / 35,
            totalMinutes = igMin + ytMin
        )
        recomputeTodayFocusScore(updated, false)
    }

    /** Real midnight evaluation (reuses the day-cycle logic). */
    suspend fun dailyEvaluation() = simulateMidnightReset()

    /** Snooze charge applied from the system lock overlay. */
    suspend fun applySnoozePenalty() = withContext(Dispatchers.IO) {
        val today = getTodayDateString()
        val comp = dao.getComponentsForDate(today)
        if (comp != null) {
            dao.saveFocusScoreComponents(
                comp.copy(overagePenalty = comp.overagePenalty + 15, total = max(0, comp.total - 15))
            )
        }
        addPoints(-5, "Snoozed Hardcore Lock (Charge)")
    }

    /** Actually clears history (fixes the old no-op clearHistory). */
    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        dao.deleteAllDailyUsage()
        getOrCreateTodayUsage()
    }

    /** Local friends (no backend). Generates a share code for new friends. */
    suspend fun addFriend(name: String, reels: Int = 0, focus: Int = 0) = withContext(Dispatchers.IO) {
        val code = "SK-" + (1000..9999).random()
        dao.saveFriend(FriendEntity(code = code, name = name, lastReels = reels, lastFocus = focus))
    }

    suspend fun removeFriend(code: String) = withContext(Dispatchers.IO) { dao.deleteFriend(code) }

    companion object {
        @Volatile private var INSTANCE: ScrollFitDatabase? = null
        fun getDatabase(ctx: Context): ScrollFitDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    ctx.applicationContext, ScrollFitDatabase::class.java, "scrollfit_db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
