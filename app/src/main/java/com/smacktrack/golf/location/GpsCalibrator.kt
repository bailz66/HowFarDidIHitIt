package com.smacktrack.golf.location

import com.smacktrack.golf.domain.GpsCoordinate

private const val MIN_SAMPLES = 3
private const val OUTLIER_MAD_FACTOR = 2.5
private const val ACCURACY_GATE_METERS = 20.0

/**
 * A single GPS reading with reported accuracy.
 */
data class GpsSample(
    val lat: Double,
    val lon: Double,
    val accuracyMeters: Double,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Result of GPS calibration with estimated accuracy.
 */
data class CalibratedPosition(
    val coordinate: GpsCoordinate,
    val estimatedAccuracyMeters: Double,
    val sampleCount: Int
)

/**
 * Calibrates a GPS position using inverse-variance weighted averaging
 * with accuracy gating and MAD-based outlier rejection.
 *
 * Algorithm:
 * 1. Skip the first sample (GPS cold-start jitter)
 * 2. Reject samples with reported accuracy worse than [ACCURACY_GATE_METERS]
 * 3. Compute weighted centroid using 1/accuracy^2 weights
 * 4. Calculate distances from centroid, reject outliers via MAD * [OUTLIER_MAD_FACTOR]
 * 5. Recompute weighted average from inliers
 * 6. Estimate final accuracy from weighted inlier spread
 *
 * @param samples Raw GPS readings including accuracy. Should contain 4+ samples
 *                collected over ~2.5s at 500ms intervals.
 * @return Calibrated position with accuracy estimate, or null if insufficient valid samples.
 */
fun calibrateWeighted(samples: List<GpsSample>): CalibratedPosition? {
    if (samples.size < 2) return null

    // Step 1: Skip first sample (cold-start jitter)
    val withoutFirst = samples.drop(1)

    // Step 2: Accuracy gate — reject readings worse than threshold
    val gated = withoutFirst.filter { it.accuracyMeters in 0.1..ACCURACY_GATE_METERS }

    if (gated.size < MIN_SAMPLES) return null

    // Step 3: Weighted centroid (inverse-variance: weight = 1/accuracy^2)
    val centroid = weightedCentroid(gated)

    // Step 4: MAD-based outlier rejection on distance from centroid
    val distances = gated.map { haversineMeters(GpsCoordinate(it.lat, it.lon), centroid) }
    val madDistance = median(distances)

    val threshold = if (madDistance < 0.01) {
        // All points nearly identical — accept everything
        Double.MAX_VALUE
    } else {
        madDistance * OUTLIER_MAD_FACTOR
    }

    val inliers = gated.filterIndexed { index, _ -> distances[index] <= threshold }

    if (inliers.size < MIN_SAMPLES) return null

    // Step 5: Final weighted average from inliers only
    val finalPosition = weightedCentroid(inliers)

    // Step 6: Estimate accuracy — weighted RMS distance from final position
    val totalWeight = inliers.sumOf { 1.0 / (it.accuracyMeters * it.accuracyMeters) }
    val weightedSumSqDist = inliers.sumOf { sample ->
        val w = 1.0 / (sample.accuracyMeters * sample.accuracyMeters)
        val d = haversineMeters(GpsCoordinate(sample.lat, sample.lon), finalPosition)
        w * d * d
    }
    val estimatedAccuracy = if (totalWeight > 0) {
        kotlin.math.sqrt(weightedSumSqDist / totalWeight)
    } else {
        inliers.minOf { it.accuracyMeters }
    }

    return CalibratedPosition(
        coordinate = finalPosition,
        estimatedAccuracyMeters = estimatedAccuracy,
        sampleCount = inliers.size
    )
}

/**
 * Computes inverse-variance weighted centroid from GPS samples.
 * Weight for each sample = 1 / accuracy^2
 */
private fun weightedCentroid(samples: List<GpsSample>): GpsCoordinate {
    var sumLat = 0.0
    var sumLon = 0.0
    var totalWeight = 0.0

    for (sample in samples) {
        val w = 1.0 / (sample.accuracyMeters * sample.accuracyMeters)
        sumLat += sample.lat * w
        sumLon += sample.lon * w
        totalWeight += w
    }

    return GpsCoordinate(sumLat / totalWeight, sumLon / totalWeight)
}

/**
 * Legacy calibration — simple median + MAD outlier rejection without accuracy weighting.
 * Kept for backward compatibility with existing tests.
 */
fun calibrate(samples: List<GpsCoordinate>): GpsCoordinate? {
    if (samples.size < MIN_SAMPLES) return null

    val medianLat = median(samples.map { it.lat })
    val medianLon = median(samples.map { it.lon })
    val medianCoord = GpsCoordinate(medianLat, medianLon)

    val distances = samples.map { haversineMeters(it, medianCoord) }
    val madDistance = median(distances)

    val threshold = if (madDistance == 0.0) {
        Double.MAX_VALUE
    } else {
        madDistance * 2.0
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
