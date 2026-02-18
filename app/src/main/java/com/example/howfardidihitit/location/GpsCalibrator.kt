package com.example.howfardidihitit.location

import com.example.howfardidihitit.domain.GpsCoordinate
import kotlin.math.abs

private const val MIN_SAMPLES = 3
private const val OUTLIER_THRESHOLD_FACTOR = 2.0

/**
 * Calibrates a GPS position from multiple samples by rejecting outliers
 * and averaging the remaining points.
 *
 * Outliers are points whose distance from the median exceeds
 * [OUTLIER_THRESHOLD_FACTOR] times the median absolute deviation.
 *
 * @param samples Raw GPS readings. Must contain at least [MIN_SAMPLES] points.
 * @return Calibrated coordinate, or null if insufficient valid samples remain.
 */
fun calibrate(samples: List<GpsCoordinate>): GpsCoordinate? {
    if (samples.size < MIN_SAMPLES) return null

    val medianLat = median(samples.map { it.lat })
    val medianLon = median(samples.map { it.lon })
    val median = GpsCoordinate(medianLat, medianLon)

    val distances = samples.map { haversineMeters(it, median) }
    val madDistance = median(distances)

    val threshold = if (madDistance == 0.0) {
        // All points identical or nearly so — accept everything
        Double.MAX_VALUE
    } else {
        madDistance * OUTLIER_THRESHOLD_FACTOR
    }

    val inliers = samples.filterIndexed { index, _ -> distances[index] <= threshold }

    if (inliers.size < MIN_SAMPLES) return null

    val avgLat = inliers.map { it.lat }.average()
    val avgLon = inliers.map { it.lon }.average()

    return GpsCoordinate(avgLat, avgLon)
}

private fun median(values: List<Double>): Double {
    val sorted = values.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 0) {
        (sorted[mid - 1] + sorted[mid]) / 2.0
    } else {
        sorted[mid]
    }
}
