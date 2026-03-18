package com.smacktrack.golf.data

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.smacktrack.golf.ui.AppSettings
import com.smacktrack.golf.ui.ShotResult

class AnalyticsTracker(private val analytics: FirebaseAnalytics) {

    fun setEnabled(enabled: Boolean) {
        analytics.setAnalyticsCollectionEnabled(enabled)
    }

    fun logShot(result: ShotResult) {
        try {
            analytics.logEvent("shot_tracked") {
                param("club", result.club.name)
                param("distance_yards", result.distanceYards.toLong())
                param("weather_group", result.weatherDescription.take(100).ifEmpty { "Unknown" })
                param("has_wind", if (result.windSpeedKmh > 0) "true" else "false")
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

    fun logSignIn(method: String = "google") {
        try {
            analytics.logEvent(FirebaseAnalytics.Event.LOGIN) {
                param(FirebaseAnalytics.Param.METHOD, method)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log sign-in event", e)
        }
    }

    fun logSignOut() {
        try {
            analytics.logEvent("sign_out", null)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log sign-out event", e)
        }
    }

    fun logShare(club: String, distanceYards: Int) {
        try {
            analytics.logEvent(FirebaseAnalytics.Event.SHARE) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "shot_card")
                param("club", club)
                param("distance_yards", distanceYards.toLong())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log share event", e)
        }
    }

    fun logShotDeleted() {
        try {
            analytics.logEvent("shot_deleted", null)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log shot deleted event", e)
        }
    }

    fun logClubChanged(fromClub: String, toClub: String) {
        try {
            analytics.logEvent("club_changed") {
                param("from_club", fromClub)
                param("to_club", toClub)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log club change event", e)
        }
    }

    fun logSettingsChanged(setting: String, value: String) {
        try {
            analytics.logEvent("settings_changed") {
                param("setting", setting)
                param("value", value)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log settings change event", e)
        }
    }

    fun logScreenView(screenName: String) {
        try {
            analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log screen view event", e)
        }
    }

    fun logGpsError(error: String) {
        try {
            analytics.logEvent("gps_error") {
                param("error_type", error.take(100))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log GPS error event", e)
        }
    }

    fun logMigration(shotCount: Int, success: Boolean) {
        try {
            analytics.logEvent("data_migration") {
                param("shot_count", shotCount.toLong())
                param("success", if (success) "true" else "false")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log migration event", e)
        }
    }

    fun setUserProperties(settings: AppSettings, shotCount: Int) {
        try {
            analytics.setUserProperty("distance_unit", settings.distanceUnit.name)
            analytics.setUserProperty("wind_unit", settings.windUnit.name)
            analytics.setUserProperty("temperature_unit", settings.temperatureUnit.name)
            analytics.setUserProperty("trajectory", settings.trajectory.name)
            analytics.setUserProperty("clubs_enabled", settings.enabledClubs.size.toString())
            analytics.setUserProperty("shot_count_tier", when {
                shotCount >= 500 -> "500+"
                shotCount >= 100 -> "100+"
                shotCount >= 50 -> "50+"
                shotCount >= 10 -> "10+"
                shotCount > 0 -> "1+"
                else -> "0"
            })
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set user properties", e)
        }
    }

    private companion object {
        const val TAG = "AnalyticsTracker"
    }
}
