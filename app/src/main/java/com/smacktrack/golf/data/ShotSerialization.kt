package com.smacktrack.golf.data

import com.smacktrack.golf.domain.Club
import com.smacktrack.golf.ui.AppSettings
import com.smacktrack.golf.ui.DistanceUnit
import com.smacktrack.golf.ui.ShotResult
import com.smacktrack.golf.ui.TemperatureUnit
import com.smacktrack.golf.ui.Trajectory
import com.smacktrack.golf.ui.WindUnit
import org.json.JSONObject

// ── JSON serialization (SharedPreferences) ──────────────────────────────────

internal fun ShotResult.toJson(): JSONObject = JSONObject().apply {
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
    put("schemaVersion", 1)
}

internal fun JSONObject.toShotResult(): ShotResult = ShotResult(
    club = enumValueOfOrNull<Club>(optString("club", "DRIVER")) ?: Club.DRIVER,
    distanceYards = optInt("distanceYards", 0),
    distanceMeters = optInt("distanceMeters", 0),
    weatherDescription = optString("weatherDescription", ""),
    temperatureF = optInt("temperatureF", 70),
    temperatureC = optInt("temperatureC", 21),
    windSpeedKmh = optDouble("windSpeedKmh", 0.0),
    windDirectionCompass = optString("windDirectionCompass", "N"),
    windDirectionDegrees = optInt("windDirectionDegrees", 0),
    shotBearingDegrees = optDouble("shotBearingDegrees", 0.0),
    timestampMs = optLong("timestampMs", 0L)
)

// ── Firestore serialization ─────────────────────────────────────────────────

internal fun ShotResult.toFirestoreMap(): Map<String, Any> = mapOf(
    "club" to club.name,
    "distanceYards" to distanceYards,
    "distanceMeters" to distanceMeters,
    "weatherDescription" to weatherDescription,
    "temperatureF" to temperatureF,
    "temperatureC" to temperatureC,
    "windSpeedKmh" to windSpeedKmh,
    "windDirectionCompass" to windDirectionCompass,
    "windDirectionDegrees" to windDirectionDegrees,
    "shotBearingDegrees" to shotBearingDegrees,
    "timestampMs" to timestampMs,
    "schemaVersion" to 1
)

internal fun AppSettings.toFirestoreMap(): Map<String, Any> = mapOf(
    "distanceUnit" to distanceUnit.name,
    "windUnit" to windUnit.name,
    "temperatureUnit" to temperatureUnit.name,
    "trajectory" to trajectory.name,
    "enabledClubs" to enabledClubs.map { it.name },
    "schemaVersion" to 1
)

// ── Enum helper ─────────────────────────────────────────────────────────────

internal inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String): T? =
    try { enumValueOf<T>(name) } catch (_: IllegalArgumentException) { null }
