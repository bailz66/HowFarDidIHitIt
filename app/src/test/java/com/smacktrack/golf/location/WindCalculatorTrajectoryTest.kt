package com.smacktrack.golf.location

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("WindCalculator trajectory multiplier tests")
class WindCalculatorTrajectoryTest {

    @Test
    fun `high trajectory increases headwind penalty`() {
        val normal = WindCalculator.estimateWindEffectYards(16.0, 180f, 200, 1.0)
        val high = WindCalculator.estimateWindEffectYards(16.0, 180f, 200, 1.3)
        assertTrue(high < normal, "High trajectory ($high) should be more negative than normal ($normal)")
    }

    @Test
    fun `low trajectory decreases headwind penalty`() {
        val normal = WindCalculator.estimateWindEffectYards(16.0, 180f, 200, 1.0)
        val low = WindCalculator.estimateWindEffectYards(16.0, 180f, 200, 0.75)
        assertTrue(low > normal, "Low trajectory ($low) should be less negative than normal ($normal)")
    }

    @Test
    fun `high trajectory increases tailwind benefit`() {
        val normal = WindCalculator.estimateWindEffectYards(16.0, 0f, 200, 1.0)
        val high = WindCalculator.estimateWindEffectYards(16.0, 0f, 200, 1.3)
        assertTrue(high > normal, "High trajectory ($high) should be more positive than normal ($normal)")
    }

    @Test
    fun `high trajectory increases lateral displacement`() {
        val normal = WindCalculator.estimateLateralDisplacementYards(16.0, 90f, 200, 1.0)
        val high = WindCalculator.estimateLateralDisplacementYards(16.0, 90f, 200, 1.3)
        assertTrue(kotlin.math.abs(high) > kotlin.math.abs(normal),
            "High trajectory lateral ($high) should be larger than normal ($normal)")
    }

    @Test
    fun `analyze with zero wind produces zero carry effect`() {
        val effect = WindCalculator.analyze(
            windSpeedKmh = 0.0,
            windFromDegrees = 0,
            shotBearingDegrees = 0.0,
            distanceYards = 200,
            temperatureF = 70
        )
        assertEquals(0, effect.carryEffectYards)
        assertEquals(0, effect.temperatureEffectYards)
        assertEquals(0, effect.totalWeatherEffectYards)
    }

    @Test
    fun `analyze with pure crosswind has minimal carry effect`() {
        val effect = WindCalculator.analyze(
            windSpeedKmh = 16.0,
            windFromDegrees = 90,
            shotBearingDegrees = 0.0,
            distanceYards = 200
        )
        // Crosswind should have minimal carry effect (only from cos(90) = 0)
        assertTrue(kotlin.math.abs(effect.carryEffectYards) <= 1,
            "Crosswind carry effect should be near zero but was ${effect.carryEffectYards}")
        // But should have significant lateral displacement
        assertTrue(kotlin.math.abs(effect.lateralDisplacementYards) > 1.0,
            "Crosswind should produce lateral displacement")
    }
}
