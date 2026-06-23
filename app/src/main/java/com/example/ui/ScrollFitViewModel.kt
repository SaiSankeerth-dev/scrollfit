package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.exercise.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ScrollFitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScrollFitRepository(application)
    val dao = repository.dao

    // State flows
    val settingsState: StateFlow<SettingsEntity?> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val limitsState: StateFlow<List<LimitEntity>> = repository.limitsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyUsageHistoryState: StateFlow<List<DailyUsageEntity>> = repository.dailyUsageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active today components
    private val _todayUsageState = MutableStateFlow<DailyUsageEntity?>(null)
    val todayUsageState: StateFlow<DailyUsageEntity?> = _todayUsageState.asStateFlow()

    private val _todayComponentsState = MutableStateFlow<FocusScoreComponentEntity?>(null)
    val todayComponentsState: StateFlow<FocusScoreComponentEntity?> = _todayComponentsState.asStateFlow()

    private val _todayScrollDebtState = MutableStateFlow<ScrollDebtEntity?>(null)
    val todayScrollDebtState: StateFlow<ScrollDebtEntity?> = _todayScrollDebtState.asStateFlow()

    val streaksState: StateFlow<StreakEntity?> = repository.streaksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _pointsBalanceState = MutableStateFlow(0)
    val pointsBalanceState: StateFlow<Int> = _pointsBalanceState.asStateFlow()

    val checkinsState: StateFlow<List<CheckInEntity>> = repository.checkinsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exercisesState: StateFlow<List<ExerciseEntity>> = repository.exercisesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val coachInsightsState: StateFlow<List<CoachInsightEntity>> = repository.coachInsightsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val intentLogsState: StateFlow<List<IntentLogEntity>> = repository.intentLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val achievementsState: StateFlow<List<AchievementEntity>> = repository.achievementsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friendsState: StateFlow<List<FriendEntity>> = repository.friendsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Attention progression for the dashboard (closes "logic exists, UI not connected").
    val attentionProgressState: StateFlow<AttentionProgress?> =
        combine(todayUsageState, streaksState) { usage, streak ->
            if (usage == null) null
            else AttentionXpEngine.progress(
                focusScore = usage.focusScore,
                streakDays = streak?.currentStreak ?: 0,
                timeSavedHours = usage.hoursSaved.toInt()
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Active App State triggers (Nudges, Overlays)
    val _activePauseNudge = MutableStateFlow(false)
    val activePauseNudge = _activePauseNudge.asStateFlow()

    val _activeSoftLock = MutableStateFlow<String?>(null) // Non-null represents blocked platform string e.g. "instagram"
    val activeSoftLock = _activeSoftLock.asStateFlow()

    // Intent Dialog trigger
    val _intentCheckPlatform = MutableStateFlow<String?>(null) // Platform requesting open reason
    val intentCheckPlatform = _intentCheckPlatform.asStateFlow()

    // Screen navigation route state (Bottom Bar, Onboarding flow)
    // Routes: "onboarding_welcome", "onboarding_limits", "onboarding_choice", "main_hub"
    private val _currentRoute = MutableStateFlow("main_hub")
    val currentRoute: StateFlow<String> = _currentRoute.asStateFlow()

    // Selected Tab inside the Main Hub: "home", "history", "insights", "settings"
    private val _selectedTab = MutableStateFlow("home")
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeDefaults()
            refreshLiveStates()
            
            // Check if limits have been set, if not, direct users to onboarding screens!
            val limits = repository.limitsFlow.firstOrNull() ?: emptyList()
            if (limits.isEmpty()) {
                _currentRoute.value = "onboarding_welcome"
            }
        }
    }

    suspend fun refreshLiveStates() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        _todayUsageState.value = repository.getOrCreateTodayUsage()
        _todayComponentsState.value = dao.getComponentsForDate(todayStr)
        _todayScrollDebtState.value = dao.getScrollDebtForDate(todayStr)
        _pointsBalanceState.value = repository.getPointsBalance()
    }

    fun navigateTo(route: String) {
        _currentRoute.value = route
    }

    fun selectTab(tab: String) {
        _selectedTab.value = tab
    }

    // Actions

    fun toggleMode(isHardcore: Boolean) {
        viewModelScope.launch {
            val settings = dao.getSettings() ?: SettingsEntity()
            dao.saveSettings(settings.copy(
                mode = if (isHardcore) "hardcore" else "gentle",
                updatedAt = System.currentTimeMillis()
            ))
            refreshLiveStates()
        }
    }

    fun updatePlatformLimit(platform: String, minutes: Int, reelsLimit: Int) {
        viewModelScope.launch {
            val limit = dao.getLimitForPlatform(platform) ?: LimitEntity(platform = platform, dailyLimitMinutes = minutes, reelLimit = reelsLimit)
            dao.saveLimit(limit.copy(
                dailyLimitMinutes = minutes,
                reelLimit = reelsLimit,
                updatedAt = System.currentTimeMillis()
            ))
            
            // Recompute score to reflect new limits immediately
            val todayUsage = repository.getOrCreateTodayUsage()
            repository.recomputeTodayFocusScore(todayUsage, false)
            refreshLiveStates()
        }
    }

    // Simulate Foreground usage increment
    fun simulateForegroundUsage(platform: String, minutes: Int, lateNight: Boolean = false) {
        viewModelScope.launch {
            // First check if intent check is enabled and we are starting a scroll!
            val settings = dao.getSettings()
            if (settings?.intentCheckEnabled == true && minutes > 0 && _intentCheckPlatform.value == null) {
                // Peek if we need to show the intent prompt
                // Only show if it's the start of the session (from 0 usage or random trigger)
                _intentCheckPlatform.value = platform
            }

            repository.incrementUsage(platform, minutes, lateNight)
            refreshLiveStates()

            // Post-increment action checks:
            val todayUsage = _todayUsageState.value
            val settingsObj = dao.getSettings()
            val limits = limitsState.value

            todayUsage?.let { usage ->
                // Pause Nudge: Trigger every 20 mins of simulated session
                if (settingsObj?.pauseEnabled == true) {
                    val platformMin = if (platform == "instagram") usage.instagramMinutes else usage.youtubeMinutes
                    if (platformMin > 0 && settingsObj.pauseIntervalMin > 0 && platformMin % settingsObj.pauseIntervalMin == 0) {
                        _activePauseNudge.value = true
                    }
                }

                // Hardcore Lock check: at limit
                if (settingsObj?.mode == "hardcore") {
                    val igLimit = limits.find { it.platform == "instagram" }?.dailyLimitMinutes ?: 30
                    val ytLimit = limits.find { it.platform == "youtube" }?.dailyLimitMinutes ?: 30
                    
                    if (platform == "instagram" && usage.instagramMinutes >= igLimit) {
                        _activeSoftLock.value = "instagram"
                    } else if (platform == "youtube" && usage.youtubeMinutes >= ytLimit) {
                        _activeSoftLock.value = "youtube"
                    }
                } else {
                    // Gentle nudge if limit reached!
                    val igLimit = limits.find { it.platform == "instagram" }?.dailyLimitMinutes ?: 30
                    val ytLimit = limits.find { it.platform == "youtube" }?.dailyLimitMinutes ?: 30
                    if (platform == "instagram" && usage.instagramMinutes == igLimit) {
                        _activePauseNudge.value = true // Show a gentle warning nudge
                    }
                }
            }
        }
    }

    // Log user open intent (§F13)
    fun logIntentCheckReason(platform: String, reason: String) {
        viewModelScope.launch {
            dao.addIntentLog(
                IntentLogEntity(
                    app = platform,
                    reason = reason
                )
            )
            _intentCheckPlatform.value = null
            repository.addPoints(5, "Logged awareness intent ($reason)")
            refreshLiveStates()
        }
    }

    // Dismiss overlay nudges
    fun dismissPauseNudge(awardPoints: Boolean = false) {
        viewModelScope.launch {
            _activePauseNudge.value = false
            if (awardPoints) {
                repository.addPoints(10, "Breather Break completed successfully")
                refreshLiveStates()
            }
        }
    }

    // Handle snoozing hard lock overlay (snoozing costs 10 Focus points & is logged, per §F12)
    fun snoozeSoftLock() {
        viewModelScope.launch {
            _activeSoftLock.value = null
            // Cost focus
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val comp = dao.getComponentsForDate(todayStr)
            if (comp != null) {
                val updatedPenalty = comp.overagePenalty + 15
                dao.saveFocusScoreComponents(comp.copy(
                    overagePenalty = updatedPenalty,
                    total = maxOf(0, comp.total - 15)
                ))
            }

            // Record snooze log in ledger
            repository.addPoints(-5, "Snoozed Hardcore Lock (Charge)")
            refreshLiveStates()
        }
    }

    // Complete active exercise gating
    fun completeActionExercise(type: String, reps: Int) {
        viewModelScope.launch {
            repository.completeExercise(type, reps, "accelerometer")
            _activeSoftLock.value = null
            refreshLiveStates()
        }
    }

    // Complete Morning Check-in
    fun performMorningCheckIn(selectedGoal: String) {
        viewModelScope.launch {
            repository.completeCheckIn(selectedGoal)
            refreshLiveStates()
        }
    }

    // Simulate Midnight Reset
    fun triggerMidnightResetSimulation() {
        viewModelScope.launch {
            repository.simulateMidnightReset()
            refreshLiveStates()
        }
    }

    // Toggle option settings via ID
    fun updateSettingsValue(action: SettingsEntity.() -> SettingsEntity) {
        viewModelScope.launch {
            val current = dao.getSettings() ?: SettingsEntity()
            dao.saveSettings(current.action())
            refreshLiveStates()
        }
    }

    // Purchase theme skin from Momentum score store (§F5)
    fun purchaseThemeSkin(themeName: String, cost: Int, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val balance = repository.getPointsBalance()
            if (balance >= cost) {
                repository.addPoints(-cost, "Unlocked Skin: $themeName")
                onComplete(true, "Successfully unlocked the premium '$themeName' dark layout vibe!")
                refreshLiveStates()
            } else {
                onComplete(false, "Insufficient Momentum Points balance. Need $cost pts, you have $balance pts.")
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            refreshLiveStates()
        }
    }

    fun addFriend(name: String, reels: Int = 0, focus: Int = 0) {
        viewModelScope.launch { repository.addFriend(name, reels, focus); refreshLiveStates() }
    }

    fun removeFriend(code: String) {
        viewModelScope.launch { repository.removeFriend(code); refreshLiveStates() }
    }

    /** Today's you-vs-friend comparison for the Friends UI. */
    fun compareWithFriend(friend: FriendEntity): FriendComparison {
        val you = todayUsageState.value
        return FriendAccountability.compare(
            youReels = you?.instagramEstReels ?: 0,
            friendReels = friend.lastReels,
            youFocus = you?.focusScore ?: 0,
            friendFocus = friend.lastFocus
        )
    }

    fun attentionLevelLabel(): String = when (attentionProgressState.value?.tier) {
        AttentionLevel.BRONZE -> "Bronze"; AttentionLevel.SILVER -> "Silver"
        AttentionLevel.GOLD -> "Gold"; AttentionLevel.PLATINUM -> "Platinum"
        AttentionLevel.DIAMOND -> "Diamond"; else -> "Bronze"
    }
}
