package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Entities

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val mode: String = "gentle", // "gentle" or "hardcore"
    val pauseEnabled: Boolean = true,
    val pauseIntervalMin: Int = 20,
    val floatingEnabled: Boolean = false,
    val overlayEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val intentCheckEnabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "limits")
data class LimitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val platform: String, // "instagram" or "youtube"
    val dailyLimitMinutes: Int,
    val reelLimit: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_usage")
data class DailyUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // "YYYY-MM-DD"
    val instagramMinutes: Int = 0,
    val youtubeMinutes: Int = 0,
    val instagramEstReels: Int = 0,
    val youtubeEstShorts: Int = 0,
    val totalMinutes: Int = 0,
    val goalMet: Boolean = true,
    val focusScore: Int = 100,
    val hoursSaved: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "focus_score_components")
data class FocusScoreComponentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // "YYYY-MM-DD"
    val base: Int = 50,
    val underLimit: Int = 0,
    val healthyAction: Int = 0,
    val earlyDiscipline: Int = 0,
    val lateNightPenalty: Int = 0,
    val overagePenalty: Int = 0,
    val total: Int = 50
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val platform: String,
    val startTs: Long,
    val endTs: Long,
    val continuousMinutes: Int
)

@Entity(tableName = "streaks")
data class StreakEntity(
    @PrimaryKey val id: Int = 1,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val freezeDaysRemaining: Int = 1, // Refills weekly to 1
    val lastEvaluatedDate: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "points_ledger")
data class PointsLedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val points: Int,
    val source: String, // "checkin", "under_limit", "streak_milestone", "exercise", "break", "purchase"
    val balanceAfter: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "checkins")
data class CheckInEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // "YYYY-MM-DD"
    val selectedGoal: String, // "stay_under", "reduce_10", "observe"
    val completed: Boolean = false,
    val rewardPoints: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val type: String, // "squats", "pushups", "steps", "stretch", "water", "focus_task"
    val repsTarget: Int,
    val repsDone: Int,
    val verifiedBy: String, // "accelerometer" or "honor"
    val pointsAwarded: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "scroll_debt")
data class ScrollDebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val squatsOwed: Int,
    val squatsCleared: Int = 0,
    val expired: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "coach_insights")
data class CoachInsightEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val type: String, // "peak_time", "late_night", "trigger", "session_length"
    val message: String,
    val suggestedAction: String,
    val accepted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "intent_logs")
data class IntentLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ts: Long = System.currentTimeMillis(),
    val app: String,
    val reason: String, // "reply", "lookup", "boredom", "habit"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val key: String,
    val title: String,
    val description: String,
    val icon: String,
    val unlocked: Boolean = false,
    val unlockedAt: Long = 0
)

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val code: String,
    val name: String,
    val lastReels: Int = 0,
    val lastFocus: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications_log")
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "morning_summary", "pause", "limit", "milestone"
    val sentAt: Long = System.currentTimeMillis(),
    val opened: Boolean = false
)

// DAO

@Dao
interface ScrollFitDao {
    // Settings
    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettingsFlow(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun getSettings(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: SettingsEntity)

    // Limits
    @Query("SELECT * FROM limits")
    fun getLimitsFlow(): Flow<List<LimitEntity>>

    @Query("SELECT * FROM limits WHERE platform = :platform")
    suspend fun getLimitForPlatform(platform: String): LimitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLimit(limit: LimitEntity)

    // Daily Usage
    @Query("SELECT * FROM daily_usage ORDER BY date DESC")
    fun getDailyUsageFlow(): Flow<List<DailyUsageEntity>>

    @Query("SELECT * FROM daily_usage WHERE date = :date")
    suspend fun getDailyUsageForDate(date: String): DailyUsageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDailyUsage(dailyUsage: DailyUsageEntity)

    // Focus Score Components
    @Query("SELECT * FROM focus_score_components ORDER BY date DESC")
    fun getFocusScoreComponentsFlow(): Flow<List<FocusScoreComponentEntity>>

    @Query("SELECT * FROM focus_score_components WHERE date = :date")
    suspend fun getComponentsForDate(date: String): FocusScoreComponentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFocusScoreComponents(components: FocusScoreComponentEntity)

    // Sessions
    @Query("SELECT * FROM sessions ORDER BY startTs DESC")
    fun getSessionsFlow(): Flow<List<SessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: SessionEntity)

    // Streaks
    @Query("SELECT * FROM streaks WHERE id = 1")
    fun getStreaksFlow(): Flow<StreakEntity?>

    @Query("SELECT * FROM streaks WHERE id = 1")
    suspend fun getStreaks(): StreakEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStreaks(streaks: StreakEntity)

    // Points Ledger
    @Query("SELECT * FROM points_ledger ORDER BY createdAt DESC")
    fun getPointsLedgerFlow(): Flow<List<PointsLedgerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addPointsLedger(entry: PointsLedgerEntity)

    // Checkins
    @Query("SELECT * FROM checkins ORDER BY date DESC")
    fun getCheckinsFlow(): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM checkins WHERE date = :date")
    suspend fun getCheckInForDate(date: String): CheckInEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordCheckIn(checkIn: CheckInEntity)

    // Exercises
    @Query("SELECT * FROM exercises ORDER BY createdAt DESC")
    fun getExercisesFlow(): Flow<List<ExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordExercise(exercise: ExerciseEntity)

    // Scroll Debt
    @Query("SELECT * FROM scroll_debt WHERE date = :date")
    suspend fun getScrollDebtForDate(date: String): ScrollDebtEntity?

    @Query("SELECT * FROM scroll_debt ORDER BY date DESC")
    fun getScrollDebtFlow(): Flow<List<ScrollDebtEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveScrollDebt(scrollDebt: ScrollDebtEntity)

    // Coach Insights
    @Query("SELECT * FROM coach_insights ORDER BY date DESC")
    fun getCoachInsightsFlow(): Flow<List<CoachInsightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCoachInsight(insight: CoachInsightEntity)

    // Intent Logs
    @Query("SELECT * FROM intent_logs ORDER BY ts DESC")
    fun getIntentLogsFlow(): Flow<List<IntentLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addIntentLog(log: IntentLogEntity)

    // Achievements
    @Query("SELECT * FROM achievements ORDER BY id ASC")
    fun getAchievementsFlow(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAchievement(achievement: AchievementEntity)

    // Notifications Log
    @Query("SELECT * FROM notifications_log ORDER BY sentAt DESC")
    fun getNotificationsLogFlow(): Flow<List<NotificationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addNotificationLog(log: NotificationLogEntity)

    // Maintenance
    @Query("DELETE FROM daily_usage")
    suspend fun deleteAllDailyUsage()

    // Friends (local model; cross-device sync requires a backend)
    @Query("SELECT * FROM friends ORDER BY name ASC")
    fun getFriendsFlow(): Flow<List<FriendEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFriend(friend: FriendEntity)

    @Query("DELETE FROM friends WHERE code = :code")
    suspend fun deleteFriend(code: String)
}

// Database

@Database(
    entities = [
        SettingsEntity::class,
        LimitEntity::class,
        DailyUsageEntity::class,
        FocusScoreComponentEntity::class,
        SessionEntity::class,
        StreakEntity::class,
        PointsLedgerEntity::class,
        CheckInEntity::class,
        ExerciseEntity::class,
        ScrollDebtEntity::class,
        CoachInsightEntity::class,
        IntentLogEntity::class,
        AchievementEntity::class,
        NotificationLogEntity::class,
        FriendEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ScrollFitDatabase : RoomDatabase() {
    abstract val dao: ScrollFitDao
}
