package com.example.data

import android.content.Context
import com.example.tracking.UsageTracker

/**
 * Clean seam between the UI/domain and Android's UsageStatsManager (Priority 1).
 * The dashboard depends on this interface, never on the Android API directly.
 */
interface UsageRepository {
    suspend fun getInstagramMinutesToday(): Int
    suspend fun getYoutubeMinutesToday(): Int
}

class UsageStatsRepositoryImpl(private val context: Context) : UsageRepository {
    override suspend fun getInstagramMinutesToday(): Int = UsageTracker.todayMinutes(context).first
    override suspend fun getYoutubeMinutesToday(): Int = UsageTracker.todayMinutes(context).second
}
