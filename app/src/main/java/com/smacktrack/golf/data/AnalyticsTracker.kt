package com.smacktrack.golf.data

import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.smacktrack.golf.ui.ShotResult

class AnalyticsTracker(private val analytics: FirebaseAnalytics) {

    fun logShot(result: ShotResult) {
        try {
            analytics.logEvent("shot_tracked") {
                param("club", result.club.name)
                param("distance_yards", result.distanceYards.toLong())
                param("weather_group", result.weatherDescription.take(100).ifEmpty { "Unknown" })
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log shot event", e)
        }
    }

    fun logAchievement(category: String, tier: String) {
        try {
            analytics.logEvent("achievement_unlocked") {
                param("category", category)
                param("tier", tier)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log achievement event", e)
        }
    }

    private companion object {
        const val TAG = "AnalyticsTracker"
    }
}
