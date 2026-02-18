package com.example.howfardidihitit.validation

import com.example.howfardidihitit.domain.Club
import com.example.howfardidihitit.domain.GpsCoordinate

/** Maximum plausible shot distance in yards. */
private const val MAX_DISTANCE_YARDS = 500.0

/** Minimum plausible shot distance in yards. */
private const val MIN_DISTANCE_YARDS = 0.0

/** Earth temperature record extremes. */
private const val MIN_TEMP_CELSIUS = -89.2
private const val MAX_TEMP_CELSIUS = 56.7

/** Highest recorded wind speed in mph. */
private const val MAX_WIND_MPH = 253.0

/** Earliest valid timestamp (Jan 1 2023 UTC). */
private const val MIN_TIMESTAMP_MS = 1672531200000L

data class ValidationResult(val isValid: Boolean, val errors: List<String> = emptyList())

fun validateCoordinate(coord: GpsCoordinate): ValidationResult {
    val errors = mutableListOf<String>()
    if (coord.lat.isNaN() || coord.lat.isInfinite()) {
        errors.add("Latitude must be a finite number")
    } else if (coord.lat !in -90.0..90.0) {
        errors.add("Latitude must be between -90 and 90, got ${coord.lat}")
    }
    if (coord.lon.isNaN() || coord.lon.isInfinite()) {
        errors.add("Longitude must be a finite number")
    } else if (coord.lon !in -180.0..180.0) {
        errors.add("Longitude must be between -180 and 180, got ${coord.lon}")
    }
    return ValidationResult(errors.isEmpty(), errors)
}

fun validateDistance(distanceYards: Double): ValidationResult {
    val errors = mutableListOf<String>()
    if (distanceYards.isNaN() || distanceYards.isInfinite()) {
        errors.add("Distance must be a finite number")
    } else if (distanceYards < MIN_DISTANCE_YARDS || distanceYards > MAX_DISTANCE_YARDS) {
        errors.add("Distance must be between $MIN_DISTANCE_YARDS and $MAX_DISTANCE_YARDS yards, got $distanceYards")
    }
    return ValidationResult(errors.isEmpty(), errors)
}

/**
 * Per-club plausible distance ranges in yards.
 */
val CLUB_DISTANCE_RANGES: Map<Club, ClosedFloatingPointRange<Double>> = mapOf(
    Club.DRIVER to 100.0..400.0,
    Club.THREE_WOOD to 80.0..280.0,
    Club.FIVE_WOOD to 70.0..250.0,
    Club.SEVEN_WOOD to 60.0..230.0,
    Club.THREE_IRON to 60.0..230.0,
    Club.FOUR_IRON to 55.0..220.0,
    Club.FIVE_IRON to 50.0..210.0,
    Club.SIX_IRON to 45.0..200.0,
    Club.SEVEN_IRON to 40.0..190.0,
    Club.EIGHT_IRON to 35.0..180.0,
    Club.NINE_IRON to 30.0..170.0,
    Club.PITCHING_WEDGE to 20.0..160.0,
    Club.GAP_WEDGE to 15.0..150.0,
    Club.SAND_WEDGE to 10.0..130.0,
    Club.LOB_WEDGE to 5.0..120.0,
    Club.PUTTER to 1.0..100.0,
    Club.HYBRID_3 to 60.0..240.0,
    Club.HYBRID_4 to 55.0..230.0
)

fun validateDistanceForClub(distanceYards: Double, club: Club): ValidationResult {
    val range = CLUB_DISTANCE_RANGES[club]
        ?: return ValidationResult(true) // unknown club, skip soft check
    return if (distanceYards in range) {
        ValidationResult(true)
    } else {
        ValidationResult(
            false,
            listOf("Distance $distanceYards yards is outside plausible range for ${club.displayName}: ${range.start}-${range.endInclusive} yards")
        )
    }
}

fun validateWeather(
    temperatureCelsius: Double?,
    windSpeedMph: Double?,
    windDirectionDegrees: Int?,
    weatherCode: Int?
): ValidationResult {
    // All-or-nothing: all present or all null
    val fields = listOf(temperatureCelsius, windSpeedMph, windDirectionDegrees, weatherCode)
    val presentCount = fields.count { it != null }
    if (presentCount == 0) return ValidationResult(true)

    val errors = mutableListOf<String>()
    if (presentCount != fields.size) {
        errors.add("Weather fields must be all present or all absent, got $presentCount of ${fields.size}")
    }

    temperatureCelsius?.let {
        if (it < MIN_TEMP_CELSIUS || it > MAX_TEMP_CELSIUS) {
            errors.add("Temperature $it°C is outside Earth extremes ($MIN_TEMP_CELSIUS to $MAX_TEMP_CELSIUS)")
        }
    }
    windSpeedMph?.let {
        if (it < 0.0 || it > MAX_WIND_MPH) {
            errors.add("Wind speed $it mph is outside valid range (0 to $MAX_WIND_MPH)")
        }
    }
    windDirectionDegrees?.let {
        if (it < 0 || it > 359) {
            errors.add("Wind direction $it° is outside valid range (0 to 359)")
        }
    }

    return ValidationResult(errors.isEmpty(), errors)
}

fun validateTimestamp(timestampMs: Long): ValidationResult {
    val errors = mutableListOf<String>()
    if (timestampMs <= 0) {
        errors.add("Timestamp must be positive, got $timestampMs")
    } else if (timestampMs < MIN_TIMESTAMP_MS) {
        errors.add("Timestamp $timestampMs is before minimum (Jan 1 2023)")
    } else if (timestampMs < 1_000_000_000_000L) {
        errors.add("Timestamp $timestampMs appears to be in seconds, not milliseconds")
    } else if (timestampMs > System.currentTimeMillis() + 60_000L) {
        errors.add("Timestamp $timestampMs is in the future")
    }
    return ValidationResult(errors.isEmpty(), errors)
}
