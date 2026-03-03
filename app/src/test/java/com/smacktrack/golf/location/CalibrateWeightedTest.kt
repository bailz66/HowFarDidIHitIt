package com.smacktrack.golf.location

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("calibrateWeighted tests")
class CalibrateWeightedTest {

    private fun sample(lat: Double, lon: Double, accuracy: Double, ts: Long = 0L) =
        GpsSample(lat, lon, accuracy, ts)

    @Nested
    @DisplayName("basic cases")
    inner class BasicCases {

        @Test
        fun `returns null for empty list`() {
            assertNull(calibrateWeighted(emptyList()))
        }

        @Test
        fun `returns null for single sample`() {
            assertNull(calibrateWeighted(listOf(sample(33.0, -84.0, 5.0))))
        }

        @Test
        fun `returns null for two samples (one dropped as cold-start)`() {
            val samples = listOf(
                sample(33.0, -84.0, 5.0),
                sample(33.0, -84.0, 5.0)
            )
            assertNull(calibrateWeighted(samples))
        }

        @Test
        fun `returns result for 5 samples with good accuracy`() {
            val samples = listOf(
                sample(33.000, -84.000, 5.0),  // dropped (cold-start)
                sample(33.001, -84.001, 3.0),
                sample(33.001, -84.001, 4.0),
                sample(33.001, -84.001, 3.5),
                sample(33.001, -84.001, 3.0)
            )
            val result = calibrateWeighted(samples)
            assertNotNull(result)
            assertEquals(4, result!!.sampleCount) // first dropped, 4 remain
            assertTrue(result.estimatedAccuracyMeters >= 0)
        }
    }

    @Nested
    @DisplayName("accuracy gating")
    inner class AccuracyGating {

        @Test
        fun `rejects samples with accuracy worse than 20m`() {
            val samples = listOf(
                sample(33.0, -84.0, 5.0),   // dropped (cold-start)
                sample(33.001, -84.001, 3.0),
                sample(33.001, -84.001, 25.0),  // rejected (>20m)
                sample(33.001, -84.001, 30.0),  // rejected (>20m)
                sample(33.001, -84.001, 4.0),
                sample(33.001, -84.001, 3.5)
            )
            val result = calibrateWeighted(samples)
            assertNotNull(result)
            assertEquals(3, result!!.sampleCount) // 3 valid after gating
        }

        @Test
        fun `rejects samples with accuracy below 0_1m`() {
            val samples = listOf(
                sample(33.0, -84.0, 5.0),   // dropped (cold-start)
                sample(33.001, -84.001, 0.05),  // rejected (<0.1m)
                sample(33.001, -84.001, 0.05),  // rejected
                sample(33.001, -84.001, 3.0),
                sample(33.001, -84.001, 4.0),
                sample(33.001, -84.001, 3.5)
            )
            val result = calibrateWeighted(samples)
            assertNotNull(result)
            assertEquals(3, result!!.sampleCount)
        }

        @Test
        fun `returns null when too few samples pass accuracy gate`() {
            val samples = listOf(
                sample(33.0, -84.0, 5.0),   // dropped (cold-start)
                sample(33.001, -84.001, 25.0),  // rejected
                sample(33.001, -84.001, 30.0),  // rejected
                sample(33.001, -84.001, 3.0),   // valid
                sample(33.001, -84.001, 50.0)   // rejected
            )
            // Only 1 valid sample after gating, need 3
            assertNull(calibrateWeighted(samples))
        }
    }

    @Nested
    @DisplayName("weighting")
    inner class Weighting {

        @Test
        fun `high accuracy sample dominates position`() {
            val samples = listOf(
                sample(33.0, -84.0, 5.0),       // dropped (cold-start)
                sample(33.0010, -84.0010, 1.0),  // high accuracy (weight = 1)
                sample(33.0020, -84.0020, 15.0), // low accuracy (weight = 1/225)
                sample(33.0010, -84.0010, 1.0),  // high accuracy
                sample(33.0010, -84.0010, 2.0)   // medium accuracy
            )
            val result = calibrateWeighted(samples)
            assertNotNull(result)
            // Position should be very close to (33.0010, -84.0010) since high-accuracy samples dominate
            assertEquals(33.0010, result!!.coordinate.lat, 0.0005)
            assertEquals(-84.0010, result.coordinate.lon, 0.0005)
        }
    }

    @Nested
    @DisplayName("outlier rejection")
    inner class OutlierRejection {

        @Test
        fun `outlier with low accuracy is dominated by high-accuracy cluster`() {
            // Cluster has high accuracy (1m), outlier has low accuracy (18m)
            val samples = listOf(
                sample(33.0, -84.0, 5.0),       // dropped (cold-start)
                sample(33.0010, -84.0010, 1.0),  // high accuracy cluster
                sample(33.0010, -84.0011, 1.0),
                sample(33.0010, -84.0009, 1.0),
                sample(33.0010, -84.0010, 1.0),
                sample(33.0100, -84.0100, 18.0)  // low accuracy outlier
            )
            val result = calibrateWeighted(samples)
            assertNotNull(result)
            // High-accuracy cluster should dominate over low-accuracy outlier
            assertEquals(33.0010, result!!.coordinate.lat, 0.001)
            assertEquals(-84.0010, result.coordinate.lon, 0.001)
        }
    }

    @Nested
    @DisplayName("CalibratedPosition fields")
    inner class ResultFields {

        @Test
        fun `estimatedAccuracyMeters is non-negative`() {
            val samples = listOf(
                sample(33.0, -84.0, 5.0),
                sample(33.001, -84.001, 3.0),
                sample(33.001, -84.001, 4.0),
                sample(33.001, -84.001, 3.5),
                sample(33.001, -84.001, 3.0)
            )
            val result = calibrateWeighted(samples)!!
            assertTrue(result.estimatedAccuracyMeters >= 0)
        }

        @Test
        fun `identical samples produce near-zero accuracy`() {
            val samples = listOf(
                sample(33.0, -84.0, 5.0),
                sample(33.001, -84.001, 3.0),
                sample(33.001, -84.001, 3.0),
                sample(33.001, -84.001, 3.0),
                sample(33.001, -84.001, 3.0)
            )
            val result = calibrateWeighted(samples)!!
            assertTrue(result.estimatedAccuracyMeters < 1.0,
                "Expected near-zero accuracy for identical samples but got ${result.estimatedAccuracyMeters}")
        }
    }
}
