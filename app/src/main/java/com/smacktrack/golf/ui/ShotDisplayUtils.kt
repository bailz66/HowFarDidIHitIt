package com.smacktrack.golf.ui

import com.smacktrack.golf.location.WindCalculator
import kotlin.math.roundToInt

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
        "${windSpeedKmh.roundToInt()} km/h"
    } else {
        "${(windSpeedKmh * 0.621371).roundToInt()} mph"
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

// ── Weather-adjusted distance ───────────────────────────────────────────────

fun distanceFor(shot: ShotResult, useYards: Boolean, weatherAdjusted: Boolean, settings: AppSettings): Int {
    val raw = if (useYards) shot.distanceYards else shot.distanceMeters
    if (!weatherAdjusted) return raw
    val effect = WindCalculator.analyze(
        windSpeedKmh = shot.windSpeedKmh,
        windFromDegrees = shot.windDirectionDegrees,
        shotBearingDegrees = shot.shotBearingDegrees,
        distanceYards = shot.distanceYards,
        trajectoryMultiplier = settings.trajectory.multiplier,
        temperatureF = shot.temperatureF
    )
    val adjustedYards = (shot.distanceYards - effect.totalWeatherEffectYards).coerceAtLeast(0)
    return if (useYards) adjustedYards else (adjustedYards * 0.9144).roundToInt()
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
