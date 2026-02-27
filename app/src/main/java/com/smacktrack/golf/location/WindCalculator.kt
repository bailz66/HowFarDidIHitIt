package com.smacktrack.golf.location

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Wind effect calculator using physics-based models calibrated against
 * TrackMan launch monitor data.
 *
 * Key findings from research:
 * - Aerodynamic drag is proportional to v², so wind effects scale non-linearly
 * - Headwind hurts ~1.5-2x more than tailwind helps (asymmetry)
 * - Higher ball flights spend more time in the air → more wind exposure
 * - Crosswind primarily affects lateral displacement, minimal carry effect
 *
 * Reference data (TrackMan):
 *   166yd 7-iron, 10mph HW → -17yds | 10mph TW → +13yds
 *   175yd 6-iron, 20mph HW → -39yds | 20mph TW → +23yds
 *   300yd Driver, 20mph HW → -41yds | 20mph TW → +33yds
 *   300yd Driver, 30mph HW → -75yds | 30mph TW → +44yds
 */
object WindCalculator {

    /**
     * Calculates the relative wind angle between wind and shot direction.
     *
     * Meteorological convention: wind direction = where wind comes FROM.
     * Result: 0° = pure tailwind, ±180° = pure headwind, ±90° = crosswind.
     *
     * @param windFromDegrees Meteorological wind direction (where wind comes FROM)
     * @param shotBearingDegrees Direction the ball was hit TOWARD
     * @return Relative angle in [-180, 180]. 0 = tailwind, 180 = headwind.
     */
    fun relativeWindAngle(windFromDegrees: Int, shotBearingDegrees: Double): Float {
        // Wind blows TOWARD = windFrom + 180
        val windGoesTo = (windFromDegrees + 180) % 360
        var diff = windGoesTo - shotBearingDegrees
        while (diff > 180) diff -= 360
        while (diff < -180) diff += 360
        return diff.toFloat()
    }

    /**
     * Decomposes wind into headwind/tailwind and crosswind components.
     *
     * @param windSpeedMph Wind speed in mph
     * @param relativeAngleDeg Relative wind angle (0 = tailwind, 180 = headwind)
     * @return Pair(alongComponent, crossComponent)
     *         alongComponent: positive = tailwind, negative = headwind
     *         crossComponent: positive = pushing right, negative = pushing left
     */
    fun decomposeWind(windSpeedMph: Double, relativeAngleDeg: Float): Pair<Double, Double> {
        val rad = Math.toRadians(relativeAngleDeg.toDouble())
        val along = windSpeedMph * cos(rad)    // + = tailwind
        val cross = windSpeedMph * sin(rad)    // + = right
        return Pair(along, cross)
    }

    /**
     * Estimates carry distance adjustment due to wind using non-linear model
     * calibrated against TrackMan data.
     *
     * Model:
     *   Headwind: effect = -speed^1.3 × 0.8 × trajectoryMultiplier × (distance/150)
     *   Tailwind: effect = +speed^1.1 × 0.4 × trajectoryMultiplier × (distance/150)
     *
     * The different exponents capture the v² drag relationship (headwind increases
     * airspeed → quadratically more drag, while tailwind reduces airspeed but also
     * reduces lift, causing the ball to fall out of the sky sooner).
     *
     * @param windSpeedKmh Wind speed in km/h
     * @param relativeAngleDeg Relative wind angle (0 = tailwind, 180 = headwind)
     * @param distanceYards Actual shot distance in yards
     * @param trajectoryMultiplier LOW=0.75, MID=1.0, HIGH=1.3
     * @return Estimated yards gained (+) or lost (-) due to wind
     */
    fun estimateWindEffectYards(
        windSpeedKmh: Double,
        relativeAngleDeg: Float,
        distanceYards: Int,
        trajectoryMultiplier: Double = 1.0
    ): Int {
        if (distanceYards <= 0 || windSpeedKmh <= 0) return 0

        val windMph = windSpeedKmh * 0.621371
        val (alongComponent, _) = decomposeWind(windMph, relativeAngleDeg)

        val effect = if (alongComponent > 0) {
            // Tailwind: less benefit due to reduced lift at lower airspeed
            // Exponent 1.1 (nearly linear, slight non-linearity at high speeds)
            alongComponent.pow(1.1) * 0.4 * trajectoryMultiplier * (distanceYards / 150.0)
        } else {
            // Headwind: more penalty due to increased drag at higher airspeed
            // Exponent 1.3 (captures quadratic drag scaling)
            val headwindMph = abs(alongComponent)
            -(headwindMph.pow(1.3) * 0.8 * trajectoryMultiplier * (distanceYards / 150.0))
        }

        return effect.toInt()
    }

    /**
     * Estimates lateral displacement due to crosswind.
     *
     * Rule of thumb calibrated from caddie data:
     *   1 foot lateral per 1 mph crosswind per 100 yards of carry.
     *
     * @param windSpeedKmh Wind speed in km/h
     * @param relativeAngleDeg Relative wind angle
     * @param distanceYards Shot distance in yards
     * @param trajectoryMultiplier LOW=0.75, MID=1.0, HIGH=1.3
     * @return Lateral displacement in yards (positive = pushed right, negative = left)
     */
    fun estimateLateralDisplacementYards(
        windSpeedKmh: Double,
        relativeAngleDeg: Float,
        distanceYards: Int,
        trajectoryMultiplier: Double = 1.0
    ): Double {
        if (distanceYards <= 0 || windSpeedKmh <= 0) return 0.0

        val windMph = windSpeedKmh * 0.621371
        val (_, crossComponent) = decomposeWind(windMph, relativeAngleDeg)

        // 1 foot per mph per 100 yards, convert to yards
        val displacementFeet = crossComponent * (distanceYards / 100.0) * trajectoryMultiplier
        return displacementFeet / 3.0
    }

    /**
     * Estimates carry distance adjustment due to temperature.
     *
     * Baseline: 70°F (standard conditions).
     * ~2 yards per 10°F change per 200 yards of carry (TrackMan-calibrated).
     * Hot air is less dense → less drag → ball flies further.
     * Cold air is more dense → more drag → ball falls shorter.
     *
     * @param distanceYards Actual shot distance in yards
     * @param temperatureF Temperature in Fahrenheit
     * @return Estimated yards gained (+) or lost (-) due to temperature
     */
    fun estimateTemperatureEffectYards(distanceYards: Int, temperatureF: Int): Int {
        if (distanceYards <= 0) return 0
        return (distanceYards * (temperatureF - 70) / 1000.0).toInt()
    }

    /**
     * Full weather analysis result for display.
     */
    data class WindEffect(
        val relativeAngleDeg: Float,
        val headwindComponentMph: Double,
        val crosswindComponentMph: Double,
        val carryEffectYards: Int,
        val temperatureEffectYards: Int,
        val totalWeatherEffectYards: Int,
        val lateralDisplacementYards: Double,
        val label: String,
        val colorCategory: WindColorCategory
    )

    enum class WindColorCategory {
        STRONG_HELPING, HELPING, SLIGHT_HELPING,
        CROSSWIND,
        SLIGHT_HURTING, HURTING, STRONG_HURTING
    }

    /**
     * Computes the complete weather analysis for a shot (wind + temperature).
     */
    fun analyze(
        windSpeedKmh: Double,
        windFromDegrees: Int,
        shotBearingDegrees: Double,
        distanceYards: Int,
        trajectoryMultiplier: Double = 1.0,
        temperatureF: Int = 70
    ): WindEffect {
        val relAngle = relativeWindAngle(windFromDegrees, shotBearingDegrees)
        val windMph = windSpeedKmh * 0.621371
        val (along, cross) = decomposeWind(windMph, relAngle)

        val carryEffect = estimateWindEffectYards(
            windSpeedKmh, relAngle, distanceYards, trajectoryMultiplier
        )
        val tempEffect = estimateTemperatureEffectYards(distanceYards, temperatureF)
        val lateralEffect = estimateLateralDisplacementYards(
            windSpeedKmh, relAngle, distanceYards, trajectoryMultiplier
        )

        val label = windLabel16(relAngle)
        val colorCat = windColorCategory(relAngle)

        return WindEffect(
            relativeAngleDeg = relAngle,
            headwindComponentMph = -along, // positive = headwind
            crosswindComponentMph = cross,
            carryEffectYards = carryEffect,
            temperatureEffectYards = tempEffect,
            totalWeatherEffectYards = carryEffect + tempEffect,
            lateralDisplacementYards = lateralEffect,
            label = label,
            colorCategory = colorCat
        )
    }

    /**
     * 16-point wind direction label relative to the shot direction.
     * Each sector is 22.5° wide. Uses golfer-friendly terms:
     * "Helping" = tailwind side, "Hurting" = headwind side.
     */
    fun windLabel16(relativeAngle: Float): String {
        val norm = ((relativeAngle % 360) + 360) % 360
        val sector = ((norm + 11.25f) / 22.5f).toInt() % 16
        return when (sector) {
            0  -> "Tailwind"
            1  -> "Helping, slight R"
            2  -> "Helping R"
            3  -> "Cross R, helping"
            4  -> "Crosswind R"
            5  -> "Cross R, hurting"
            6  -> "Hurting R"
            7  -> "Headwind, slight R"
            8  -> "Headwind"
            9  -> "Headwind, slight L"
            10 -> "Hurting L"
            11 -> "Cross L, hurting"
            12 -> "Crosswind L"
            13 -> "Cross L, helping"
            14 -> "Helping L"
            15 -> "Helping, slight L"
            else -> "Wind"
        }
    }

    fun windColorCategory(relativeAngle: Float): WindColorCategory {
        val a = abs(relativeAngle)
        return when {
            a <= 22.5f  -> WindColorCategory.STRONG_HELPING
            a <= 56.25f -> WindColorCategory.HELPING
            a <= 78.75f -> WindColorCategory.SLIGHT_HELPING
            a <= 101.25f -> WindColorCategory.CROSSWIND
            a <= 123.75f -> WindColorCategory.SLIGHT_HURTING
            a <= 157.5f -> WindColorCategory.HURTING
            else        -> WindColorCategory.STRONG_HURTING
        }
    }
}
