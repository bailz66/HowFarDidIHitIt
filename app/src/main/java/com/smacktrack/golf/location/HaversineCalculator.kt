package com.smacktrack.golf.location

/**
 * Great-circle distance and bearing calculations using the Haversine formula.
 *
 * Used to compute shot distances from GPS start/end coordinates and the
 * initial bearing (azimuth) of each shot for wind-relative calculations.
 */

import com.smacktrack.golf.domain.GpsCoordinate
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0
private const val METERS_PER_YARD = 0.9144

/**
 * Calculates the great-circle distance between two GPS coordinates
 * using the Haversine formula.
 *
 * @return Distance in meters.
 */
fun haversineMeters(start: GpsCoordinate, end: GpsCoordinate): Double {
    val dLat = Math.toRadians(end.lat - start.lat)
    val dLon = Math.toRadians(end.lon - start.lon)
    val lat1 = Math.toRadians(start.lat)
    val lat2 = Math.toRadians(end.lat)

    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return EARTH_RADIUS_METERS * c
}

/**
 * Calculates the initial bearing (forward azimuth) from [start] to [end].
 *
 * @return Bearing in degrees [0, 360). North = 0, East = 90, South = 180, West = 270.
 */
fun bearingDegrees(start: GpsCoordinate, end: GpsCoordinate): Double {
    val lat1 = Math.toRadians(start.lat)
    val lat2 = Math.toRadians(end.lat)
    val dLon = Math.toRadians(end.lon - start.lon)

    val x = sin(dLon) * cos(lat2)
    val y = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

    val bearing = Math.toDegrees(atan2(x, y))
    return (bearing + 360) % 360
}

/** Converts meters to yards. */
fun metersToYards(meters: Double): Double = meters / METERS_PER_YARD

/** Converts yards to meters. */
fun yardsToMeters(yards: Double): Double = yards * METERS_PER_YARD
