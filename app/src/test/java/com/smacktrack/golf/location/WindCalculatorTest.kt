package com.smacktrack.golf.location

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("WindCalculator temperature and weather effect tests")
class WindCalculatorTest {

    @Test
    @DisplayName("temperature effect at baseline 70F is zero")
    fun temperatureEffectAtBaseline() {
        val effect = WindCalculator.estimateTemperatureEffectYards(200, 70)
        assertEquals(0, effect)
    }

    @Test
    @DisplayName("hot day 90F on 200yd shot gives positive yards")
    fun hotDayPositiveEffect() {
        val effect = WindCalculator.estimateTemperatureEffectYards(200, 90)
        // (200 * (90-70) / 1000) = 4
        assertEquals(4, effect)
    }

    @Test
    @DisplayName("cold day 40F on 200yd shot gives negative yards")
    fun coldDayNegativeEffect() {
        val effect = WindCalculator.estimateTemperatureEffectYards(200, 40)
        // (200 * (40-70) / 1000) = -6
        assertEquals(-6, effect)
    }

    @Test
    @DisplayName("zero distance returns zero temperature effect")
    fun zeroDistanceReturnsZero() {
        val effect = WindCalculator.estimateTemperatureEffectYards(0, 90)
        assertEquals(0, effect)
    }

    @Test
    @DisplayName("analyze returns combined weather effect (wind + temp)")
    fun analyzeCombinesWindAndTemp() {
        // Pure headwind (180° relative angle) at 16 km/h on 200yd shot at 90°F
        val result = WindCalculator.analyze(
            windSpeedKmh = 16.0,
            windFromDegrees = 0,    // wind from north
            shotBearingDegrees = 0.0, // hitting north → headwind
            distanceYards = 200,
            trajectoryMultiplier = 1.0,
            temperatureF = 90
        )
        val tempEffect = WindCalculator.estimateTemperatureEffectYards(200, 90)
        assertEquals(tempEffect, result.temperatureEffectYards)
        assertEquals(
            result.carryEffectYards + result.temperatureEffectYards,
            result.totalWeatherEffectYards
        )
    }
}
