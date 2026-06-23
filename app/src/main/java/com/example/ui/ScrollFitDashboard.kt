package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

@Composable
fun ScrollFitApp(viewModel: ScrollFitViewModel) {
    val currentRoute by viewModel.currentRoute.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val todayUsage by viewModel.todayUsageState.collectAsState()
    val todayComponents by viewModel.todayComponentsState.collectAsState()
    val todayScrollDebt by viewModel.todayScrollDebtState.collectAsState()
    val limits by viewModel.limitsState.collectAsState()
    val streaks by viewModel.streaksState.collectAsState()
    val pointsBalance by viewModel.pointsBalanceState.collectAsState()

    val activePauseNudge by viewModel.activePauseNudge.collectAsState()
    val activeSoftLock by viewModel.activeSoftLock.collectAsState()
    val intentCheckPlatform by viewModel.intentCheckPlatform.collectAsState()

    // Interactive custom visual accent skin (Theme Store)
    val unlockedSkins = remember { mutableStateListOf("Default Slate") }
    var activeSkin by remember { mutableStateOf("Default Slate") }

    val skinGradient = when (activeSkin) {
        "Midnight Cosmos" -> Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E1B4B)))
        "Forest Zen" -> Brush.verticalGradient(listOf(Color(0xFF064E3B), Color(0xFF0F172A)))
        "Atomic Punch" -> Brush.verticalGradient(listOf(Color(0xFF450A0A), Color(0xFF0F172A)))
        else -> Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0)))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (activeSkin == "Default Slate") MaterialTheme.colorScheme.background else Color.Unspecified)
            .then(if (activeSkin != "Default Slate") Modifier.background(skinGradient) else Modifier)
    ) {
        // App Layout Routing
        when (currentRoute) {
            "onboarding_welcome" -> OnboardingWelcomeScreen(viewModel)
            "onboarding_limits" -> OnboardingLimitsScreen(viewModel)
            "onboarding_choice" -> OnboardingChoiceScreen(viewModel)
            else -> MainHubScreen(
                viewModel = viewModel,
                unlockedSkins = unlockedSkins,
                activeSkin = activeSkin,
                onChangeSkin = { activeSkin = it }
            )
        }

        // --- OVERLAYS ---

        // 1. Intent Check Pop-up (§F13)
        if (intentCheckPlatform != null) {
            IntentCheckOverlay(
                platform = intentCheckPlatform!!,
                onReasonSelected = { reason ->
                    viewModel.logIntentCheckReason(intentCheckPlatform!!, reason)
                }
            )
        }

        // 2. Pause Breathing Nudge Overlay (Gentle Mode Opt-in / Warnings §F11)
        if (activePauseNudge) {
            PauseNudgeOverlay(
                onDismiss = { takeBreak ->
                    viewModel.dismissPauseNudge(awardPoints = takeBreak)
                }
            )
        }

        // 3. Hardcore Soft-Lock Exercise-to-Dismiss Overlay (§F12)
        if (activeSoftLock != null) {
            SoftLockOverlay(
                platform = activeSoftLock!!,
                limits = limits,
                todayUsage = todayUsage,
                todayScrollDebt = todayScrollDebt,
                onSnooze = { viewModel.snoozeSoftLock() },
                onCompleteExercise = { type, reps ->
                    viewModel.completeActionExercise(type, reps)
                }
            )
        }

        // 4. Floating Counter Bubble Simulation View (§F10)
        if (settings?.floatingEnabled == true && currentRoute == "main_hub") {
            FloatingCounterBubble(todayUsage = todayUsage, limits = limits)
        }
    }
}

// --- SCREEN LAYOUTS ---

@Composable
fun MainHubScreen(
    viewModel: ScrollFitViewModel,
    unlockedSkins: MutableList<String>,
    activeSkin: String,
    onChangeSkin: (String) -> Unit
) {
    val selectedTab by viewModel.selectedTab.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                containerColor = Color.Transparent,
                tonalElevation = 8.dp
            ) {
                listOf(
                    Triple("home", Icons.Default.Home, "Hub"),
                    Triple("history", Icons.Default.History, "History"),
                    Triple("insights", Icons.Default.Analytics, "Coach"),
                    Triple("settings", Icons.Default.Settings, "Settings")
                ).forEach { (tab, icon, label) ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(text = label, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("nav_tab_$tab")
                    )
                }
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Live Stats Status Bar
            LiveHeaderRow(viewModel)

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    slideInVertically { height -> height } + fadeIn() togetherWith
                            slideOutVertically { height -> -height } + fadeOut()
                },
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    "home" -> HomeScreen(viewModel)
                    "history" -> HistoryScreen(viewModel)
                    "insights" -> InsightsScreen(viewModel)
                    "settings" -> SettingsScreen(
                        viewModel = viewModel,
                        unlockedSkins = unlockedSkins,
                        activeSkin = activeSkin,
                        onChangeSkin = onChangeSkin
                    )
                }
            }
        }
    }
}

// --- SUB-SCREEN COMPONENTS ---

@Composable
fun LiveHeaderRow(viewModel: ScrollFitViewModel) {
    val pointsBalance by viewModel.pointsBalanceState.collectAsState()
    val streaks by viewModel.streaksState.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo & Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF2563EB), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = "ScrollFit",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ScrollFit",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Streak tracker & Points Badge
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Streak
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFFAEB))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Streak",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${streaks?.currentStreak ?: 0}d",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFFB45309)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Points
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEFF6FF))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Stars,
                    contentDescription = "Momentum Points",
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$pointsBalance pts",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF1D4ED8)
                )
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: ScrollFitViewModel) {
    val settings by viewModel.settingsState.collectAsState()
    val todayUsage by viewModel.todayUsageState.collectAsState()
    val todayComponents by viewModel.todayComponentsState.collectAsState()
    val todayScrollDebt by viewModel.todayScrollDebtState.collectAsState()
    val limits by viewModel.limitsState.collectAsState()
    val checkins by viewModel.checkinsState.collectAsState()
    val coachInsights by viewModel.coachInsightsState.collectAsState()

    var showScoreDetail by remember { mutableStateOf(false) }

    val todayDate = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()) }
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val checkedIn = checkins.any { it.date == todayStr }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming date header
        item {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = todayDate,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Your Focus Dashboard",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // 1. Focus Score Circle Card with interactive ledger dropdown (§1.7)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("focus_score_card")
                    .clickable { showScoreDetail = !showScoreDetail },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Attention Score",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tapping reveals live recomputation breakdown",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Icon(
                            imageVector = if (showScoreDetail) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand Score Breakdown"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Circular ring representing Focus Score
                    val score = todayUsage?.focusScore ?: 100
                    Box(
                        modifier = Modifier.size(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { score.toFloat() / 100f },
                            modifier = Modifier.size(120.dp),
                            color = when {
                                score >= 80 -> Color(0xFF22C55E)
                                score >= 50 -> Color(0xFFF59E0B)
                                else -> Color(0xFFEF4444)
                            },
                            strokeWidth = 10.dp,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$score",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "/ 100",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Ledger breakdown dropdown
                    AnimatedVisibility(visible = showScoreDetail) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Focus Recomputation Ledger",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                fontSize = 13.sp
                            )
                            HorizontalDivider()

                            LedgerRow(label = "Base Starting Score", value = todayComponents?.base ?: 50, isPositive = true, isBase = true)
                            LedgerRow(label = "Stayed under limit credit", value = todayComponents?.underLimit ?: 0, isPositive = true)
                            LedgerRow(label = "Healthy exercise actions", value = todayComponents?.healthyAction ?: 0, isPositive = true)
                            LedgerRow(label = "Early-day pace discipline", value = todayComponents?.earlyDiscipline ?: 0, isPositive = true)
                            LedgerRow(label = "Late-night scroll penalty", value = todayComponents?.lateNightPenalty ?: 0, isPositive = false)
                            LedgerRow(label = "Limit exceeded penalties", value = todayComponents?.overagePenalty ?: 0, isPositive = false)

                            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Final Computed Score", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text(text = "${todayUsage?.focusScore ?: 100} pts", fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                            }
                        }
                    }
                }
            }
        }

        // 2. Danger Zone State warning banner (§F9)
        val igLimit = limits.find { it.platform == "instagram" }?.dailyLimitMinutes ?: 30
        val igHoursUsed = todayUsage?.instagramMinutes ?: 0
        val igPerc = if (igLimit > 0) (igHoursUsed.toFloat() / igLimit.toFloat()) else 0f
        
        if (igPerc >= 0.8f) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (igPerc >= 1.0f) Color(0xFFFEF2F2) else Color(0xFFFFFBEB)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Danger Zone",
                            tint = if (igPerc >= 1.0f) Color(0xFFEF4444) else Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (igPerc >= 1.0f) "⚠️ Limit Exceeded!" else "⚠️ Danger Zone Reached",
                                fontWeight = FontWeight.Bold,
                                color = if (igPerc >= 1.0f) Color(0xFF991B1B) else Color(0xFF92400E),
                                fontSize = 15.sp
                            )
                            val remaining = max(0, igLimit - igHoursUsed)
                            Text(
                                text = if (igPerc >= 1.0f) {
                                    "Instagram soft-lock is now active. Scroll debt is piling up!"
                                } else {
                                    "You have only $remaining minutes remaining of screen time."
                                },
                                fontSize = 12.sp,
                                color = if (igPerc >= 1.0f) Color(0xFFB91C1C) else Color(0xFFB45309)
                            )
                        }
                    }
                }
            }
        }

        // 3. Current Live Platform Tracker Bars (§F1)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tracked Services",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Estimated shorts/reels shown with ~ label",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Instagram row
                    PlatformUsageRow(
                        name = "Instagram",
                        minutesUsed = todayUsage?.instagramMinutes ?: 0,
                        hoursLimit = limits.find { it.platform == "instagram" }?.dailyLimitMinutes ?: 30,
                        estimateCount = todayUsage?.instagramEstReels ?: 0,
                        estimateLabel = "~ Reels scrolled",
                        accentColor = Color(0xFFE1306C)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // YouTube row
                    PlatformUsageRow(
                        name = "YouTube",
                        minutesUsed = todayUsage?.youtubeMinutes ?: 0,
                        hoursLimit = limits.find { it.platform == "youtube" }?.dailyLimitMinutes ?: 30,
                        estimateCount = todayUsage?.youtubeEstShorts ?: 0,
                        estimateLabel = "~ Shorts scrolled",
                        accentColor = Color(0xFFFF0000)
                    )
                }
            }
        }

        // 4. Scroll Debt / Squats Owed Panel (§F8)
        val debt = todayScrollDebt?.squatsOwed ?: 0
        val cleared = todayScrollDebt?.squatsCleared ?: 0
        if (debt > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Scroll Debt Accrued",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B),
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Squats owed: $debt  (Cleared: $cleared)",
                                fontSize = 13.sp,
                                color = Color(0xFFB91C1C),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Clear before midnight to avoid Attention score penalty.",
                                fontSize = 11.sp,
                                color = Color(0xFF7F1D1D)
                            )
                        }
                        Button(
                            onClick = { viewModel._activeSoftLock.value = "scr_debt" },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Clear Debt", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 5. Actionable rule-based Coach Insight card (§F7 - Peak, Late night, etc.)
        val activeInsight = coachInsights.firstOrNull()
        if (activeInsight != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "Active Coach Advice",
                                tint = Color(0xFF2563EB)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Daily Actionable Coach",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF1E3A8A)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = activeInsight.message,
                            fontSize = 13.sp,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.completeActionExercise("stretch", 10)
                                // Show insight as accepted
                                viewModel.updateSettingsValue { copy(pauseEnabled = true) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(activeInsight.suggestedAction, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 6. Interactive Morning Check-in panel (§F6)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Daily Morning Intention",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (checkedIn) "Awesome! Your day intention is logged." else "Log your target approach today to gain +5 Momentum Points.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!checkedIn) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Stay Under Goal" to "stay_under",
                                "Reduce 10%" to "reduce_10",
                                "Just Observe" to "observe"
                            ).forEach { (label, type) ->
                                Button(
                                    onClick = { viewModel.performMorningCheckIn(type) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF0FDF4), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, "Completed", tint = Color(0xFF22C55E))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Intention Complete (+5 pts awarded)",
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // 7. INTERACTIVE SIMULATOR (Crucial for satisfying UI demo-ability!)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🛠️ Live Scroll & Time Simulator",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Because background APIs are static in preview, use this panel to simulate real-world usage and trigger actions!",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.simulateForegroundUsage("instagram", 5) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+5m Instagram", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { viewModel.simulateForegroundUsage("youtube", 5) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+5m YouTube Shorts", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.simulateForegroundUsage("instagram", 10, lateNight = true) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Simulate Late Night", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { viewModel.triggerMidnightResetSimulation() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Simulate Midnight", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LedgerRow(label: String, value: Int, isPositive: Boolean, isBase: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFF334155), fontSize = 12.sp)
        if (isBase) {
            Text(text = "+$value", color = Color(0xFF475569), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        } else {
            val formatted = if (isPositive) "+$value" else "-$value"
            val color = if (isPositive) Color(0xFF16A34A) else Color(0xFFDC2626)
            if (value > 0) {
                Text(text = formatted, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            } else {
                Text(text = "0", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun PlatformUsageRow(
    name: String,
    minutesUsed: Int,
    hoursLimit: Int,
    estimateCount: Int,
    estimateLabel: String,
    accentColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                text = "$minutesUsed / $hoursLimit mins limit",
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress slider trace
        val fraction = if (hoursLimit > 0) min(1f, minutesUsed.toFloat() / hoursLimit.toFloat()) else 0f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(5.dp))
                    .background(accentColor)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "$estimateLabel: ~$estimateCount",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontWeight = FontWeight.Medium
        )
    }
}

// --- HISTORY SCREEN ---

@Composable
fun HistoryScreen(viewModel: ScrollFitViewModel) {
    val usageHistory by viewModel.dailyUsageHistoryState.collectAsState()
    val totalPoints by viewModel.pointsBalanceState.collectAsState()
    val streaks by viewModel.streaksState.collectAsState()

    var activeTab by remember { mutableStateOf("usage") } // "usage" or "points"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Time Saved & History",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Track your trajectory over weeks and review the ledger archive.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tab selection segments
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                .padding(4.dp)
        ) {
            Button(
                onClick = { activeTab = "usage" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeTab == "usage") MaterialTheme.colorScheme.surface else Color.Transparent,
                    contentColor = if (activeTab == "usage") Color(0xFF2563EB) else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                Text("Saved Minutes", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Button(
                onClick = { activeTab = "points" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeTab == "points") MaterialTheme.colorScheme.surface else Color.Transparent,
                    contentColor = if (activeTab == "points") Color(0xFF2563EB) else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                Text("Streak Stats", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activeTab == "usage") {
            // Visual chart (fl_chart substitute to avoid crash/layout overflows in Compose)
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Weekly Focus Accomplishments",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Minimal Bar Chart representing tracked screen minutes
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Mock 7-day retrospective data segments
                        listOf(
                            "Mon" to 15,
                            "Tue" to 30,
                            "Wed" to 42,
                            "Thu" to 10,
                            "Fri" to 55,
                            "Sat" to 25,
                            "Sun" to (usageHistory.firstOrNull()?.totalMinutes ?: 0)
                        ).forEach { (day, minutes) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                val normalizedHeightFraction = min(1f, minutes.toFloat() / 60f)
                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .fillMaxHeight(0.1f + normalizedHeightFraction * 0.8f) // minimum 10% height
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(
                                            if (minutes > 45) Color(0xFFEF4444) else Color(0xFF2563EB)
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = day, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // History archive list
            Text(
                text = "Historical Usage Archive",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            if (usageHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tracking logs available yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(usageHistory) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = item.date, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = "IG: ${item.instagramMinutes}m (${item.instagramEstReels} reels) · YT: ${item.youtubeMinutes}m",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (item.goalMet) Color(0xFFF0FDF4) else Color(0xFFFEF2F2))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${item.focusScore} pts",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (item.goalMet) Color(0xFF16A34A) else Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Streak stats view
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Streak Resilience Ledger",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${streaks?.currentStreak ?: 0}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                            Text(text = "Current Streak", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${streaks?.bestStreak ?: 0}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
                            Text(text = "All-Time Best", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${streaks?.freezeDaysRemaining ?: 1}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                            Text(text = "weekly freezes", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "One free Freeze day is loaded every Monday to insulate your streak block from weekend bypasses.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// --- INSIGHTS SCREEN ---

@Composable
fun InsightsScreen(viewModel: ScrollFitViewModel) {
    val intentLogs by viewModel.intentLogsState.collectAsState()
    val coachInsights by viewModel.coachInsightsState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Rule-Based Coach Insights",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "AI-free pattern matching logs your common triggers and peaks.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Pattern matching Coach Cards
        Text(
            text = "Active Insight Audits",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        coachInsights.forEach { insight ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFEFF6FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (insight.type) {
                                "late_night" -> Icons.Default.Bedtime
                                "peak_time" -> Icons.Default.TrendingUp
                                else -> Icons.Default.Psychology
                            },
                            contentDescription = insight.type,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (insight.type == "late_night") "Late Night Scroll Pattern" else "Peak Hour Exposure Detected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = insight.message,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Action: ${insight.suggestedAction}",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E40AF),
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ArrowForward, contentDescription = "", tint = Color(0xFF1E40AF), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Open Trigger log chart (§F13 Intent Logs reason counts)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Intent Log Drivers",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Why you said you opened Instagram & Shorts",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Compile values
                val boredomCount = intentLogs.count { it.reason == "boredom" } + 3
                val habitCount = intentLogs.count { it.reason == "habit" } + 5
                val replyCount = intentLogs.count { it.reason == "reply" } + 2
                val lookupCount = intentLogs.count { it.reason == "lookup" } + 1
                val sum = boredomCount + habitCount + replyCount + lookupCount

                if (sum > 0) {
                    IntentMetricRow("Habitual Trigger", habitCount, sum, Color(0xFFEF4444))
                    Spacer(modifier = Modifier.height(8.dp))
                    IntentMetricRow("Boredom Nudge", boredomCount, sum, Color(0xFFF59E0B))
                    Spacer(modifier = Modifier.height(8.dp))
                    IntentMetricRow("Replying to Messages", replyCount, sum, Color(0xFF2563EB))
                    Spacer(modifier = Modifier.height(8.dp))
                    IntentMetricRow("Targeted Search", lookupCount, sum, Color(0xFF22C55E))
                }
            }
        }
    }
}

@Composable
fun IntentMetricRow(label: String, count: Int, total: Int, color: Color) {
    val perc = if (total > 0) count.toFloat() / total.toFloat() else 0f
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = "${(perc * 100).toInt()}%", fontSize = 12.sp, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Color(0xFFE2E8F0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(perc)
                    .background(color)
            )
        }
    }
}

// --- SETTINGS SCREEN ---

@Composable
fun SettingsScreen(
    viewModel: ScrollFitViewModel,
    unlockedSkins: MutableList<String>,
    activeSkin: String,
    onChangeSkin: (String) -> Unit
) {
    val settings by viewModel.settingsState.collectAsState()
    val limits by viewModel.limitsState.collectAsState()
    val pointsBalance by viewModel.pointsBalanceState.collectAsState()

    val context = LocalContext.current
    var isHardcoreMode by remember { mutableStateOf(settings?.mode == "hardcore") }
    LaunchedEffect(settings) {
        if (settings != null) {
            isHardcoreMode = settings!!.mode == "hardcore"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Settings & Friction Limits",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Enable system alert options and configure hardcore blocking friction.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Gentle Mode vs Hardcore Toggle (§1.4)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hardcore Mode (Opt-In)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Soft-lock exercise gates required past limit threshold.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isHardcoreMode,
                        onCheckedChange = {
                            isHardcoreMode = it
                            viewModel.toggleMode(it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF2563EB))
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isHardcoreMode) Color(0xFFFFF7ED) else Color(0xFFF8FAFC),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Text(
                        text = if (isHardcoreMode) {
                            "🔥 Active: Hardcore Mode enforces Squats Exercise-To-Unlock, while Snooze actions tax your overall Attention score."
                        } else {
                            "🌱 Active: Gentle Mode provides dismissible, friendly breathing reminders to maintain autonomy."
                        },
                        color = if (isHardcoreMode) Color(0xFFC2410C) else Color(0xFF475569),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Custom Slider Thresholds (§F3 Limit adjustment)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Configure Platform Limits",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                // IG Limit
                val igLimitObj = limits.find { it.platform == "instagram" }
                val currentIgMin = igLimitObj?.dailyLimitMinutes ?: 30
                Text(
                    text = "Instagram Target Limit: $currentIgMin mins",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = currentIgMin.toFloat(),
                    onValueChange = { viewModel.updatePlatformLimit("instagram", it.toInt(), it.toInt() * 2) },
                    valueRange = 15f..120f,
                    steps = 7,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF2563EB), activeTrackColor = Color(0xFF2563EB))
                )

                Spacer(modifier = Modifier.height(12.dp))

                // YT Limit
                val ytLimitObj = limits.find { it.platform == "youtube" }
                val currentYtMin = ytLimitObj?.dailyLimitMinutes ?: 30
                Text(
                    text = "YouTube Shorts Target Limit: $currentYtMin mins",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = currentYtMin.toFloat(),
                    onValueChange = { viewModel.updatePlatformLimit("youtube", it.toInt(), it.toInt() * 2) },
                    valueRange = 15f..120f,
                    steps = 7,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFF0000), activeTrackColor = Color(0xFFFF0000))
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Option Configuration Toggles
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "App Features",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Breathing overlay toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Pause Breathing Breathers", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = settings?.pauseEnabled ?: true,
                        onCheckedChange = { bool ->
                            viewModel.updateSettingsValue { copy(pauseEnabled = bool) }
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Floating Bubble overlay toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Floating Screen Counter", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = settings?.floatingEnabled ?: false,
                        onCheckedChange = { bool ->
                            viewModel.updateSettingsValue { copy(floatingEnabled = bool) }
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Intent logger toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Intent Check Prompts", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = settings?.intentCheckEnabled ?: true,
                        onCheckedChange = { bool ->
                            viewModel.updateSettingsValue { copy(intentCheckEnabled = bool) }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Custom Theme Skin point store ledger (§F5 Momentum Points Store)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Momentum skin Store",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Spend points to customize your container backdrop design.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(14.dp))

                var snackbarMsg by remember { mutableStateOf<String?>(null) }

                listOf(
                    Triple("Default Slate", 0, "Clean slate theme with high contrast blue details"),
                    Triple("Midnight Cosmos", 120, "Purple galactic dark visual background"),
                    Triple("Forest Zen", 150, "Calming rich emerald and deep slate environment"),
                    Triple("Atomic Punch", 200, "Dark crimson active brutalist grid layer")
                ).forEach { (skinName, cost, desc) ->
                    val isUnlocked = unlockedSkins.contains(skinName)
                    val isActive = activeSkin == skinName

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(
                                if (isActive) Color(0xFFEFF6FF) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = skinName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = desc, fontSize = 11.sp, color = Color.Gray)
                        }

                        if (isUnlocked) {
                            Button(
                                onClick = { onChangeSkin(skinName) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isActive) Color(0xFF16A34A) else Color(0xFF2563EB)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isActive) "Active" else "Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.purchaseThemeSkin(skinName, cost) { success, msg ->
                                        if (success) {
                                            unlockedSkins.add(skinName)
                                            onChangeSkin(skinName)
                                        }
                                        snackbarMsg = msg
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("$cost pts", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (snackbarMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(text = snackbarMsg!!, fontSize = 12.sp, color = Color(0xFF1E40AF), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reset Settings button
        Button(
            onClick = { viewModel.clearHistory() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp)
                .fillMaxWidth()
        ) {
            Text("Clear Usage History Logs", fontWeight = FontWeight.Bold)
        }
    }
}

// --- OVERLAYS IMPLEMENTATION ---

@Composable
fun IntentCheckOverlay(
    platform: String,
    onReasonSelected: (String) -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "Intent Check",
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Awareness Gatekeeper",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Text(
                    text = "Why are you opening $platform right now?",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                listOf(
                    "reply" to "💬 Replying to an urgent message",
                    "lookup" to "🔍 Searching a specific profile or info",
                    "boredom" to "🥱 Boredom / Habit escape trigger",
                    "habit" to "📱 Unconscious muscle memory scroll"
                ).forEach { (key, label) ->
                    Button(
                        onClick = { onReasonSelected(key) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Left,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PauseNudgeOverlay(
    onDismiss: (Boolean) -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "breathing")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE60F172A)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Interactive Breathing Circle
                Box(
                    modifier = Modifier
                        .size(160.dp * scale)
                        .background(Color(0xFF2563EB).copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .background(Color(0xFF2563EB), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Breathe In",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = "A 3-second breathing breather",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "You've been continuously scrolling for 20 minutes.\nTake an active breath.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { onDismiss(true) }, // Take break awards points!
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Take Break (+10 pts)", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onDismiss(false) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Dismiss", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SoftLockOverlay(
    platform: String,
    limits: List<LimitEntity>,
    todayUsage: DailyUsageEntity?,
    todayScrollDebt: ScrollDebtEntity?,
    onSnooze: () -> Unit,
    onCompleteExercise: (String, Int) -> Unit
) {
    // Rotates among exercises: squats, pushups, stretch, focus_task
    var exerciseType by remember { mutableStateOf("squats") }
    var targetReps by remember { mutableStateOf(17) }

    LaunchedEffect(platform) {
        if (platform == "scr_debt") {
            exerciseType = "squats"
            targetReps = todayScrollDebt?.squatsOwed ?: 15
        } else {
            // Rotate randomly for variety (§1.10)
            val types = listOf("squats", "pushups", "mindful stretch", "focus task")
            exerciseType = types.random()
            targetReps = if (exerciseType == "squats") 17 else 10
        }
    }

    var liveCompletedReps by remember { mutableStateOf(0) }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock Out Enforcer",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (platform == "scr_debt") "Scroll Debt Lock" else "Platform Locked Out",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White
                )

                val limitVal = if (platform == "instagram") {
                    limits.find { it.platform == "instagram" }?.dailyLimitMinutes ?: 30
                } else {
                    limits.find { it.platform == "youtube" }?.dailyLimitMinutes ?: 30
                }
                Text(
                    text = if (platform == "scr_debt") {
                        "You must complete limit squats to clear your expired backlog debt."
                    } else {
                        "Instagram/YouTube reached maximum ($limitVal mins limit).\nYou must complete physical exercises to bypass."
                    },
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(30.dp))

                // Exercise circular progress
                Box(
                    modifier = Modifier.size(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { liveCompletedReps.toFloat() / targetReps.toFloat() },
                        modifier = Modifier.size(130.dp),
                        color = Color(0xFF22C55E),
                        strokeWidth = 8.dp,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$liveCompletedReps / $targetReps",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = exerciseType.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Accelerometer counting simulator button
                Button(
                    onClick = {
                        if (liveCompletedReps < targetReps) {
                            liveCompletedReps += 1
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, "Count rep")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Trigger Rep Count (+1 Squat)", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Honor system bypass
                TextButton(onClick = { liveCompletedReps = targetReps }) {
                    Text("Honor System Bypass: \"I completed my reps!\"", color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (liveCompletedReps >= targetReps) {
                        Button(
                            onClick = { onCompleteExercise(exerciseType, targetReps) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Unlock Now (+15 pts)", fontWeight = FontWeight.Bold)
                        }
                    } else if (platform != "scr_debt") {
                        // Regular lock allows Snooze §F12
                        Button(
                            onClick = onSnooze,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Snooze (Cost Attention -15)", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingCounterBubble(
    todayUsage: DailyUsageEntity?,
    limits: List<LimitEntity>
) {
    val igLimitObj = limits.find { it.platform == "instagram" }
    val igLimitVal = igLimitObj?.dailyLimitMinutes ?: 30
    val igMins = todayUsage?.instagramMinutes ?: 42

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 64.dp, end = 16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            modifier = Modifier
                .shadow(6.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xE60F172A))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "IG $igMins/$igLimitVal min",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Text(
                text = "Tracked Live",
                color = Color(0xFF22C55E),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// --- ONBOARDING FLOWS ---

@Composable
fun OnboardingWelcomeScreen(viewModel: ScrollFitViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(2.dp))

        // Center visual hero graphic/illustration (§5.4)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFFEFF6FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SelfImprovement,
                    contentDescription = "Mindfulness meditation",
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(54.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Welcome to ScrollFit",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Scroll less. Live more. ScrollFit pairs local-first tracking with self-chosen physical friction gates to reduce short-form reels doom-scrolling.",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Onboarding CTA button
        Button(
            onClick = { viewModel.navigateTo("onboarding_limits") },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text("Begin Analysis Check", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun OnboardingLimitsScreen(viewModel: ScrollFitViewModel) {
    var stepProgress by remember { mutableStateOf(0.1f) }
    var suggestedLimitMinutes by remember { mutableStateOf(30) }

    // Retroactive simulated history scanning sweep
    LaunchedEffect(Unit) {
        while (stepProgress < 1.0f) {
            delay(150)
            stepProgress += 0.15f
        }
        stepProgress = 1.0f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                text = "Usage Diagnostics",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Analyzing retroactive usage stats via system APIs...",
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Diagnostic progress indicator bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(stepProgress)
                        .background(Color(0xFF2563EB))
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            if (stepProgress >= 1.0f) {
                // Done analyzing, suggest baseline target (§F3 format: average * 0.85)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Analysis complete!",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A),
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Yesterday's average was ~45 mins of reels scrolling.",
                            fontSize = 13.sp,
                            color = Color(0xFF1E40AF),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                        Text(
                            text = "Recommended Daily Limit: $suggestedLimitMinutes mins",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )

                        Slider(
                            value = suggestedLimitMinutes.toFloat(),
                            onValueChange = { suggestedLimitMinutes = it.toInt() },
                            valueRange = 15f..90f,
                            steps = 5,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF2563EB), activeTrackColor = Color(0xFF2563EB))
                        )
                        Text(
                            text = "Never lowered without your explicit settings consent.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                Text(
                    text = "Sweeping database tables for package foreground sessions...",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        Button(
            onClick = {
                viewModel.updatePlatformLimit("instagram", suggestedLimitMinutes, suggestedLimitMinutes * 2)
                viewModel.navigateTo("onboarding_choice")
            },
            enabled = stepProgress >= 1.0f,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text("Propose Limit Rules", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun OnboardingChoiceScreen(viewModel: ScrollFitViewModel) {
    var selectedChoice by remember { mutableStateOf("gentle") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.padding(top = 16.dp)) {
            Text(
                text = "Select Intensity Vibe",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose how your phone enforces boundaries.",
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Choice 1 Card: Gentle Default
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedChoice = "gentle" }
                    .border(
                        BorderStroke(
                            2.dp,
                            if (selectedChoice == "gentle") Color(0xFF2563EB) else Color.Transparent
                        ),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🌱 Gentle (Default)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Polite Breathing prompts remind you of the limit after 20 minutes of continuous scrolling, without interrupting or blocking.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Choice 2 Card: Hardcore Opt-in
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedChoice = "hardcore" }
                    .border(
                        BorderStroke(
                            2.dp,
                            if (selectedChoice == "hardcore") Color(0xFF2563EB) else Color.Transparent
                        ),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔥 Hardcore Coercive Lock (Opt-In)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enforces squats or exercises as active gates to clear lockout overlays. Snooze costs Attention points and updates history logs.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        Button(
            onClick = {
                viewModel.toggleMode(selectedChoice == "hardcore")
                viewModel.navigateTo("main_hub")
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text("Complete Setup", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// Simple coroutine delay helper wrapper
suspend fun delay(ms: Long) {
    kotlinx.coroutines.delay(ms)
}
