package com.smacktrack.golf.ui

/**
 * Pure formatting extensions for [ShotResult] — no Compose dependency.
 * Usable by both Compose UI and Canvas-based [ShotCardRenderer].
 */

// ── Distance ────────────────────────────────────────────────────────────────

fun ShotResult.primaryDistance(unit: DistanceUnit): Int =
    if (unit == DistanceUnit.YARDS) distanceYards else distanceMeters

fun ShotResult.primaryUnitLabel(unit: DistanceUnit): String =
    if (unit == DistanceUnit.YARDS) "YARDS" else "METERS"

fun ShotResult.secondaryDistance(unit: DistanceUnit): String =
    if (unit == DistanceUnit.YARDS) "${distanceMeters}m" else "${distanceYards}yd"

fun ShotResult.shortUnitLabel(unit: DistanceUnit): String =
    if (unit == DistanceUnit.YARDS) "yds" else "m"

// ── Wind ────────────────────────────────────────────────────────────────────

fun ShotResult.formatWindSpeed(unit: WindUnit): String =
    if (unit == WindUnit.KMH) {
        "${windSpeedKmh.toInt()} km/h"
    } else {
        "${(windSpeedKmh * 0.621371).toInt()} mph"
    }

// ── Temperature ─────────────────────────────────────────────────────────────

fun ShotResult.formatTemperature(unit: TemperatureUnit): String =
    if (unit == TemperatureUnit.FAHRENHEIT) {
        "${temperatureF}\u00B0F"
    } else {
        "${temperatureC}\u00B0C"
    }

// ── Celebration badge percentile ────────────────────────────────────────────

/**
 * Returns the percentile (0-100) of this shot among prior shots with the same club,
 * or null if fewer than 5 prior shots exist for this club.
 */
fun ShotResult.percentileAmongClub(shotHistory: List<ShotResult>, unit: DistanceUnit): Float? {
    val priorShots = shotHistory.filter { it.club == club && it.timestampMs != timestampMs }
    if (priorShots.size < 5) return null
    val thisDistance = primaryDistance(unit)
    val beatenCount = priorShots.count { thisDistance >= it.primaryDistance(unit) }
    return beatenCount.toFloat() / priorShots.size.toFloat() * 100f
}

// ── Wind strength label ─────────────────────────────────────────────────────

fun windStrengthLabel(windSpeedKmh: Double): String = when {
    windSpeedKmh < 6   -> "None"
    windSpeedKmh < 13  -> "Very Light"
    windSpeedKmh < 20  -> "Light"
    windSpeedKmh < 36  -> "Medium"
    windSpeedKmh < 50  -> "Strong"
    windSpeedKmh < 71  -> "Very Strong"
    else               -> "Why are you even out here?!"
}
