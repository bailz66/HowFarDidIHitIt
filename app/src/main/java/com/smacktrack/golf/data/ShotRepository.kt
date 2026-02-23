package com.smacktrack.golf.data

import android.content.Context
import com.smacktrack.golf.domain.Club
import com.smacktrack.golf.ui.AppSettings
import com.smacktrack.golf.ui.DistanceUnit
import com.smacktrack.golf.ui.ShotResult
import com.smacktrack.golf.ui.TemperatureUnit
import com.smacktrack.golf.ui.Trajectory
import com.smacktrack.golf.ui.WindUnit
import org.json.JSONArray
import org.json.JSONObject

class ShotRepository(context: Context) {

    private val prefs = context.getSharedPreferences("smacktrack_data", Context.MODE_PRIVATE)

    // ── Shots ────────────────────────────────────────────────────────────────

    fun loadShots(): List<ShotResult> {
        val json = prefs.getString("shot_history", null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i -> array.getJSONObject(i).toShotResult() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveShots(shots: List<ShotResult>) {
        val array = JSONArray()
        shots.forEach { array.put(it.toJson()) }
        prefs.edit().putString("shot_history", array.toString()).apply()
    }

    // ── Settings ─────────────────────────────────────────────────────────────

    fun loadSettings(): AppSettings {
        return try {
            AppSettings(
                distanceUnit = prefs.getString("distance_unit", null)
                    ?.let { enumValueOfOrNull<DistanceUnit>(it) }
                    ?: DistanceUnit.YARDS,
                windUnit = prefs.getString("wind_unit", null)
                    ?.let { enumValueOfOrNull<WindUnit>(it) }
                    ?: WindUnit.MPH,
                temperatureUnit = prefs.getString("temperature_unit", null)
                    ?.let { enumValueOfOrNull<TemperatureUnit>(it) }
                    ?: TemperatureUnit.FAHRENHEIT,
                trajectory = prefs.getString("trajectory", null)
                    ?.let { enumValueOfOrNull<Trajectory>(it) }
                    ?: Trajectory.MID,
                enabledClubs = loadEnabledClubs()
            )
        } catch (_: Exception) {
            AppSettings()
        }
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putString("distance_unit", settings.distanceUnit.name)
            .putString("wind_unit", settings.windUnit.name)
            .putString("temperature_unit", settings.temperatureUnit.name)
            .putString("trajectory", settings.trajectory.name)
            .putString("enabled_clubs", JSONArray().apply {
                settings.enabledClubs.forEach { put(it.name) }
            }.toString())
            .apply()
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun loadEnabledClubs(): Set<Club> {
        val json = prefs.getString("enabled_clubs", null) ?: return Club.entries.toSet()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                enumValueOfOrNull<Club>(array.getString(i))
            }.toSet()
        } catch (_: Exception) {
            Club.entries.toSet()
        }
    }

    private fun ShotResult.toJson(): JSONObject = JSONObject().apply {
        put("club", club.name)
        put("distanceYards", distanceYards)
        put("distanceMeters", distanceMeters)
        put("weatherDescription", weatherDescription)
        put("temperatureF", temperatureF)
        put("temperatureC", temperatureC)
        put("windSpeedKmh", windSpeedKmh)
        put("windDirectionCompass", windDirectionCompass)
        put("windDirectionDegrees", windDirectionDegrees)
        put("shotBearingDegrees", shotBearingDegrees)
        put("timestampMs", timestampMs)
    }

    private fun JSONObject.toShotResult(): ShotResult = ShotResult(
        club = enumValueOfOrNull<Club>(getString("club")) ?: Club.DRIVER,
        distanceYards = getInt("distanceYards"),
        distanceMeters = getInt("distanceMeters"),
        weatherDescription = getString("weatherDescription"),
        temperatureF = getInt("temperatureF"),
        temperatureC = getInt("temperatureC"),
        windSpeedKmh = getDouble("windSpeedKmh"),
        windDirectionCompass = getString("windDirectionCompass"),
        windDirectionDegrees = optInt("windDirectionDegrees", 0),
        shotBearingDegrees = optDouble("shotBearingDegrees", 0.0),
        timestampMs = optLong("timestampMs", System.currentTimeMillis())
    )

    private inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String): T? =
        try { enumValueOf<T>(name) } catch (_: IllegalArgumentException) { null }
}
