package com.smacktrack.golf.location

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.math.abs

@DisplayName("WindCalculator tests")
class WindCalculatorTest {

    // ── Temperature effect (existing) ───────────────────────────────────────

    @Nested
    @DisplayName("Temperature effect")
    inner class TemperatureEffectTests {
        @Test
        @DisplayName("temperature effect at baseline 70F is zero")
        fun temperatureEffectAtBaseline() {
            assertEquals(0, WindCalculator.estimateTemperatureEffectYards(200, 70))
        }

        @Test
        @DisplayName("hot day 90F on 200yd shot gives positive yards")
        fun hotDayPositiveEffect() {
            assertEquals(4, WindCalculator.estimateTemperatureEffectYards(200, 90))
        }

        @Test
        @DisplayName("cold day 40F on 200yd shot gives negative yards")
        fun coldDayNegativeEffect() {
            assertEquals(-6, WindCalculator.estimateTemperatureEffectYards(200, 40))
        }

        @Test
        @DisplayName("zero distance returns zero temperature effect")
        fun zeroDistanceReturnsZero() {
            assertEquals(0, WindCalculator.estimateTemperatureEffectYards(0, 90))
        }
    }

    // ── analyze (existing) ──────────────────────────────────────────────────

    @Test
    @DisplayName("analyze returns combined weather effect (wind + temp)")
    fun analyzeCombinesWindAndTemp() {
        val result = WindCalculator.analyze(
            windSpeedKmh = 16.0,
            windFromDegrees = 0,
            shotBearingDegrees = 0.0,
            distanceYards = 200,
            trajectoryMultiplier = 1.0,
            temperatureF = 90
        )
        assertEquals(
            WindCalculator.estimateTemperatureEffectYards(200, 90),
            result.temperatureEffectYards
        )
        assertEquals(
            result.carryEffectYards + result.temperatureEffectYards,
            result.totalWeatherEffectYards
        )
    }

    // ── relativeWindAngle ───────────────────────────────────────────────────

    @Nested
    @DisplayName("relativeWindAngle")
    inner class RelativeWindAngleTests {
        @Test
        @DisplayName("pure headwind: wind from N, hitting N → 180° relative (headwind)")
        fun pureHeadwind() {
            // Wind from 0 (N), shot bearing 0 (N)
            // windGoesTo = 180, diff = 180 - 0 = 180
            val angle = WindCalculator.relativeWindAngle(0, 0.0)
            assertEquals(180f, angle, 0.01f)
        }

        @Test
        @DisplayName("pure tailwind: wind from S (180), hitting N (0)")
        fun pureTailwind() {
            // Wind from 180, windGoesTo = 0, diff = 0 - 0 = 0
            val angle = WindCalculator.relativeWindAngle(180, 0.0)
            assertEquals(0f, angle, 0.01f)
        }

        @Test
        @DisplayName("crosswind right: wind from W (270), hitting N (0)")
        fun crosswindRight() {
            // Wind from 270, windGoesTo = 90, diff = 90 - 0 = 90
            val angle = WindCalculator.relativeWindAngle(270, 0.0)
            assertEquals(90f, angle, 0.01f)
        }

        @Test
        @DisplayName("crosswind left: wind from E (90), hitting N (0)")
        fun crosswindLeft() {
            // Wind from 90, windGoesTo = 270, diff = 270 - 0 = 270 → wraps to -90
            val angle = WindCalculator.relativeWindAngle(90, 0.0)
            assertEquals(-90f, angle, 0.01f)
        }

        @Test
        @DisplayName("wrapping: wind from 350, hitting 10")
        fun wrapping() {
            // windGoesTo = 170, diff = 170 - 10 = 160
            val angle = WindCalculator.relativeWindAngle(350, 10.0)
            assertEquals(160f, angle, 0.01f)
        }
    }

    // ── decomposeWind ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("decomposeWind")
    inner class DecomposeWindTests {
        @Test
        @DisplayName("pure tailwind: relAngle 0° → all along, no cross")
        fun pureTailwind() {
            val (along, cross) = WindCalculator.decomposeWind(10.0, 0f)
            assertEquals(10.0, along, 0.01)
            assertEquals(0.0, cross, 0.01)
        }

        @Test
        @DisplayName("pure headwind: relAngle 180° → negative along, no cross")
        fun pureHeadwind() {
            val (along, cross) = WindCalculator.decomposeWind(10.0, 180f)
            assertEquals(-10.0, along, 0.01)
            assertTrue(abs(cross) < 0.01)
        }

        @Test
        @DisplayName("pure crosswind right: relAngle 90°")
        fun pureCrosswindRight() {
            val (along, cross) = WindCalculator.decomposeWind(10.0, 90f)
            assertTrue(abs(along) < 0.01)
            assertEquals(10.0, cross, 0.01)
        }

        @Test
        @DisplayName("zero wind returns zero components")
        fun zeroWind() {
            val (along, cross) = WindCalculator.decomposeWind(0.0, 45f)
            assertEquals(0.0, along, 0.01)
            assertEquals(0.0, cross, 0.01)
        }
    }

    // ── estimateWindEffectYards ──────────────────────────────────────────────

    @Nested
    @DisplayName("estimateWindEffectYards")
    inner class EstimateWindEffectTests {
        @Test
        @DisplayName("headwind gives negative carry effect")
        fun headwindNegative() {
            val effect = WindCalculator.estimateWindEffectYards(16.0, 180f, 200)
            assertTrue(effect < 0, "Headwind should reduce distance, got $effect")
        }

        @Test
        @DisplayName("tailwind gives positive carry effect")
        fun tailwindPositive() {
            val effect = WindCalculator.estimateWindEffectYards(16.0, 0f, 200)
            assertTrue(effect > 0, "Tailwind should increase distance, got $effect")
        }

        @Test
        @DisplayName("headwind penalty is greater than tailwind benefit (asymmetry)")
        fun headwindTailwindAsymmetry() {
            val headwindEffect = WindCalculator.estimateWindEffectYards(16.0, 180f, 200)
            val tailwindEffect = WindCalculator.estimateWindEffectYards(16.0, 0f, 200)
            assertTrue(
                abs(headwindEffect) > abs(tailwindEffect),
                "Headwind ${abs(headwindEffect)} should exceed tailwind $tailwindEffect"
            )
        }

        @Test
        @DisplayName("zero wind returns zero effect")
        fun zeroWind() {
            assertEquals(0, WindCalculator.estimateWindEffectYards(0.0, 180f, 200))
        }

        @Test
        @DisplayName("zero distance returns zero effect")
        fun zeroDistance() {
            assertEquals(0, WindCalculator.estimateWindEffectYards(16.0, 180f, 0))
        }
    }

    // ── estimateLateralDisplacementYards ─────────────────────────────────────

    @Nested
    @DisplayName("estimateLateralDisplacementYards")
    inner class LateralDisplacementTests {
        @Test
        @DisplayName("crosswind right causes positive displacement")
        fun crosswindRightPositive() {
            val lateral = WindCalculator.estimateLateralDisplacementYards(16.0, 90f, 200)
            assertTrue(lateral > 0, "Right crosswind should push right, got $lateral")
        }

        @Test
        @DisplayName("crosswind left causes negative displacement")
        fun crosswindLeftNegative() {
            val lateral = WindCalculator.estimateLateralDisplacementYards(16.0, -90f, 200)
            assertTrue(lateral < 0, "Left crosswind should push left, got $lateral")
        }

        @Test
        @DisplayName("zero wind returns zero displacement")
        fun zeroWind() {
            assertEquals(0.0, WindCalculator.estimateLateralDisplacementYards(0.0, 90f, 200), 0.01)
        }

        @Test
        @DisplayName("zero distance returns zero displacement")
        fun zeroDistance() {
            assertEquals(0.0, WindCalculator.estimateLateralDisplacementYards(16.0, 90f, 0), 0.01)
        }

        @Test
        @DisplayName("pure headwind has negligible lateral displacement")
        fun headwindNoLateral() {
            val lateral = WindCalculator.estimateLateralDisplacementYards(16.0, 180f, 200)
            assertTrue(abs(lateral) < 0.1, "Pure headwind lateral should be ~0, got $lateral")
        }
    }

    // ── windLabel16 ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("windLabel16")
    inner class WindLabel16Tests {
        @Test fun tailwind() = assertEquals("Tailwind", WindCalculator.windLabel16(0f))
        @Test fun helpingSlightR() = assertEquals("Helping, slight R", WindCalculator.windLabel16(22.5f))
        @Test fun helpingR() = assertEquals("Helping R", WindCalculator.windLabel16(45f))
        @Test fun crossRHelping() = assertEquals("Cross R, helping", WindCalculator.windLabel16(67.5f))
        @Test fun crosswindR() = assertEquals("Crosswind R", WindCalculator.windLabel16(90f))
        @Test fun crossRHurting() = assertEquals("Cross R, hurting", WindCalculator.windLabel16(112.5f))
        @Test fun hurtingR() = assertEquals("Hurting R", WindCalculator.windLabel16(135f))
        @Test fun headwindSlightR() = assertEquals("Headwind, slight R", WindCalculator.windLabel16(157.5f))
        @Test fun headwind() = assertEquals("Headwind", WindCalculator.windLabel16(180f))
        @Test fun headwindSlightL() = assertEquals("Headwind, slight L", WindCalculator.windLabel16(-157.5f))
        @Test fun hurtingL() = assertEquals("Hurting L", WindCalculator.windLabel16(-135f))
        @Test fun crossLHurting() = assertEquals("Cross L, hurting", WindCalculator.windLabel16(-112.5f))
        @Test fun crosswindL() = assertEquals("Crosswind L", WindCalculator.windLabel16(-90f))
        @Test fun crossLHelping() = assertEquals("Cross L, helping", WindCalculator.windLabel16(-67.5f))
        @Test fun helpingL() = assertEquals("Helping L", WindCalculator.windLabel16(-45f))
        @Test fun helpingSlightL() = assertEquals("Helping, slight L", WindCalculator.windLabel16(-22.5f))
    }

    // ── windColorCategory ───────────────────────────────────────────────────

    @Nested
    @DisplayName("windColorCategory")
    inner class WindColorCategoryTests {
        @Test
        @DisplayName("0° → STRONG_HELPING")
        fun strongHelping() {
            assertEquals(
                WindCalculator.WindColorCategory.STRONG_HELPING,
                WindCalculator.windColorCategory(0f)
            )
        }

        @Test
        @DisplayName("22.5° boundary → STRONG_HELPING")
        fun strongHelpingBoundary() {
            assertEquals(
                WindCalculator.WindColorCategory.STRONG_HELPING,
                WindCalculator.windColorCategory(22.5f)
            )
        }

        @Test
        @DisplayName("45° → HELPING")
        fun helping() {
            assertEquals(
                WindCalculator.WindColorCategory.HELPING,
                WindCalculator.windColorCategory(45f)
            )
        }

        @Test
        @DisplayName("90° → CROSSWIND")
        fun crosswind() {
            assertEquals(
                WindCalculator.WindColorCategory.CROSSWIND,
                WindCalculator.windColorCategory(90f)
            )
        }

        @Test
        @DisplayName("135° → HURTING")
        fun hurting() {
            assertEquals(
                WindCalculator.WindColorCategory.HURTING,
                WindCalculator.windColorCategory(135f)
            )
        }

        @Test
        @DisplayName("180° → STRONG_HURTING")
        fun strongHurting() {
            assertEquals(
                WindCalculator.WindColorCategory.STRONG_HURTING,
                WindCalculator.windColorCategory(180f)
            )
        }

        @Test
        @DisplayName("negative angles use absolute value")
        fun negativeAngle() {
            assertEquals(
                WindCalculator.windColorCategory(90f),
                WindCalculator.windColorCategory(-90f)
            )
        }
    }
}
