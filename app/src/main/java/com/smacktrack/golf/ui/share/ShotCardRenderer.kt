package com.smacktrack.golf.ui.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.smacktrack.golf.R
import com.smacktrack.golf.location.WindCalculator
import com.smacktrack.golf.ui.AppSettings
import com.smacktrack.golf.ui.DistanceUnit
import com.smacktrack.golf.ui.ShotResult
import com.smacktrack.golf.ui.TemperatureUnit
import com.smacktrack.golf.ui.WindUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ShotCardRenderer {

    private const val SIZE = 1080
    private const val CENTER_X = SIZE / 2f

    // Colors
    private const val DARK_GREEN = 0xFF1B5E20.toInt()
    private const val CHIP_GREEN_LIGHT = 0xFF43A047.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val TEXT_PRIMARY = 0xFF1A1C19.toInt()
    private const val TEXT_SECONDARY = 0xFF5F6158.toInt()
    private const val STRIP_BG = 0xFFF2F3EF.toInt()
    private const val GOLD = 0xFFFFAB00.toInt()
    private const val GOLD_BG = 0x1FFFAB00
    private const val GREEN_BG = 0x1A1B5E20

    // Gradient stops
    private const val GRADIENT_TOP = 0xFF0D3B12.toInt()
    private const val GRADIENT_MID = 0xFF1B5E20.toInt()
    private const val GRADIENT_BOTTOM = 0xFF245C2B.toInt()

    fun render(
        context: Context,
        result: ShotResult,
        settings: AppSettings,
        shotHistory: List<ShotResult> = emptyList()
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val poppinsBold = ResourcesCompat.getFont(context, R.font.poppins_bold) ?: Typeface.DEFAULT_BOLD
        val poppinsSemiBold = ResourcesCompat.getFont(context, R.font.poppins_semibold) ?: Typeface.DEFAULT_BOLD
        val poppinsMedium = ResourcesCompat.getFont(context, R.font.poppins_medium) ?: Typeface.DEFAULT
        val robotoBold = ResourcesCompat.getFont(context, R.font.roboto_bold) ?: Typeface.DEFAULT_BOLD

        // 1. Gradient background
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, SIZE.toFloat(),
                intArrayOf(GRADIENT_TOP, GRADIENT_MID, GRADIENT_BOTTOM),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, SIZE.toFloat(), SIZE.toFloat(), bgPaint)

        // 2. White card
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = WHITE }
        val cardRect = RectF(56f, 56f, 1024f, 1024f)
        canvas.drawRoundRect(cardRect, 48f, 48f, cardPaint)

        var y = 140f

        // 3. Club badge pill
        val clubName = result.club.displayName
        val clubColor = clubChipColor(result.club.sortOrder)
        val clubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = poppinsBold
            textSize = 36f
            color = WHITE
            textAlign = Paint.Align.CENTER
        }
        val clubTextWidth = clubPaint.measureText(clubName)
        val pillHPad = 48f
        val pillVPad = 18f
        val pillRect = RectF(
            CENTER_X - clubTextWidth / 2 - pillHPad,
            y - pillVPad,
            CENTER_X + clubTextWidth / 2 + pillHPad,
            y + 36f + pillVPad
        )
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = clubColor }
        canvas.drawRoundRect(pillRect, 40f, 40f, pillPaint)
        canvas.drawText(clubName, CENTER_X, y + 32f, clubPaint)

        y += 100f

        // 4. Primary distance
        val primaryDistance = if (settings.distanceUnit == DistanceUnit.YARDS) result.distanceYards else result.distanceMeters
        val primaryUnit = if (settings.distanceUnit == DistanceUnit.YARDS) "YARDS" else "METERS"
        val distancePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = robotoBold
            textSize = 180f
            color = TEXT_PRIMARY
            textAlign = Paint.Align.CENTER
            letterSpacing = -0.02f
        }
        canvas.drawText("$primaryDistance", CENTER_X, y + 160f, distancePaint)
        y += 180f

        // 5. Unit label
        val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = poppinsSemiBold
            textSize = 32f
            color = TEXT_SECONDARY
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.25f
        }
        canvas.drawText(primaryUnit, CENTER_X, y + 30f, unitPaint)
        y += 50f

        // 6. Secondary distance
        val secondaryDistance = if (settings.distanceUnit == DistanceUnit.YARDS) "${result.distanceMeters}m" else "${result.distanceYards}yd"
        val secondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = poppinsMedium
            textSize = 30f
            color = TEXT_SECONDARY
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(secondaryDistance, CENTER_X, y + 28f, secondaryPaint)
        y += 50f

        // 7. Celebration badge
        val currentDistance = if (settings.distanceUnit == DistanceUnit.YARDS) result.distanceYards else result.distanceMeters
        val priorShots = shotHistory.filter { it.club == result.club && it.timestampMs != result.timestampMs }
        if (priorShots.size >= 5) {
            val beatenCount = priorShots.count { shot ->
                val d = if (settings.distanceUnit == DistanceUnit.YARDS) shot.distanceYards else shot.distanceMeters
                currentDistance >= d
            }
            val percentile = beatenCount.toFloat() / priorShots.size.toFloat() * 100f
            val badgeText: String?
            val badgeTextColor: Int
            val badgeBgColor: Int
            val badgeSize: Float
            when {
                percentile >= 95f -> {
                    badgeText = "Absolutely Smacked!"
                    badgeTextColor = GOLD
                    badgeBgColor = GOLD_BG
                    badgeSize = 42f
                }
                percentile >= 80f -> {
                    badgeText = "Smacked!"
                    badgeTextColor = DARK_GREEN
                    badgeBgColor = GREEN_BG
                    badgeSize = 38f
                }
                else -> {
                    badgeText = null
                    badgeTextColor = 0
                    badgeBgColor = 0
                    badgeSize = 0f
                }
            }
            if (badgeText != null) {
                y += 16f
                val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = poppinsBold
                    textSize = badgeSize
                    color = badgeTextColor
                    textAlign = Paint.Align.CENTER
                }
                val bw = badgePaint.measureText(badgeText)
                val bPad = 40f
                val bvPad = 16f
                val badgeRect = RectF(
                    CENTER_X - bw / 2 - bPad, y,
                    CENTER_X + bw / 2 + bPad, y + badgeSize + bvPad * 2
                )
                val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = badgeBgColor }
                canvas.drawRoundRect(badgeRect, 40f, 40f, badgeBgPaint)
                canvas.drawText(badgeText, CENTER_X, y + badgeSize + bvPad / 2, badgePaint)
                y += badgeSize + bvPad * 2 + 8f
            }
        }

        y += 28f

        // 8. Weather + wind strip
        val stripRect = RectF(100f, y, 980f, y + 140f)
        val stripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = STRIP_BG }
        canvas.drawRoundRect(stripRect, 28f, 28f, stripPaint)

        val stripPad = 32f

        // Temperature
        val tempDisplay = if (settings.temperatureUnit == TemperatureUnit.FAHRENHEIT) {
            "${result.temperatureF}\u00B0F"
        } else {
            "${result.temperatureC}\u00B0C"
        }
        val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = poppinsSemiBold
            textSize = 34f
            color = TEXT_PRIMARY
        }
        canvas.drawText(tempDisplay, stripRect.left + stripPad, y + 48f, tempPaint)

        // Weather description
        val weatherPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = poppinsMedium
            textSize = 26f
            color = TEXT_SECONDARY
        }
        canvas.drawText(result.weatherDescription, stripRect.left + stripPad, y + 90f, weatherPaint)

        // Wind (right side)
        val windSpeed = if (settings.windUnit == WindUnit.KMH) {
            "${result.windSpeedKmh.toInt()} km/h"
        } else {
            "${(result.windSpeedKmh * 0.621371).toInt()} mph"
        }
        val windPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = poppinsSemiBold
            textSize = 34f
            color = TEXT_PRIMARY
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(windSpeed, stripRect.right - stripPad, y + 48f, windPaint)

        // Wind strength label
        val strengthLabel = windStrengthLabel(result.windSpeedKmh)
        val strengthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = poppinsMedium
            textSize = 26f
            color = TEXT_SECONDARY
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(strengthLabel, stripRect.right - stripPad, y + 90f, strengthPaint)

        // Wind arrow (centered in strip)
        if (result.windSpeedKmh > 0) {
            val relAngle = WindCalculator.relativeWindAngle(result.windDirectionDegrees, result.shotBearingDegrees)
            val colorCat = WindCalculator.windColorCategory(relAngle)
            val arrowColor = windCategoryColorInt(colorCat)
            drawWindArrow(canvas, CENTER_X, y + 70f, 56f, relAngle, arrowColor)
        }

        y += 160f

        // 9. Wind-adjusted distance
        if (result.windSpeedKmh > 0) {
            val windEffect = WindCalculator.analyze(
                windSpeedKmh = result.windSpeedKmh,
                windFromDegrees = result.windDirectionDegrees,
                shotBearingDegrees = result.shotBearingDegrees,
                distanceYards = result.distanceYards,
                trajectoryMultiplier = settings.trajectory.multiplier
            )
            val noWindYards = result.distanceYards - windEffect.carryEffectYards
            val noWindMeters = (noWindYards * 0.9144).toInt()
            val adjustedDisplay = if (settings.distanceUnit == DistanceUnit.YARDS) "$noWindYards" else "$noWindMeters"
            val unitLabel = if (settings.distanceUnit == DistanceUnit.YARDS) "yds" else "m"
            val diff = windEffect.carryEffectYards
            val diffText = if (diff >= 0) "(+$diff)" else "($diff)"

            val adjustedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = poppinsSemiBold
                textSize = 30f
                color = TEXT_SECONDARY
                textAlign = Paint.Align.CENTER
            }
            val diffPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = poppinsSemiBold
                textSize = 30f
                color = windCategoryColorInt(windEffect.colorCategory)
            }

            val baseText = "Wind Adjusted: $adjustedDisplay $unitLabel "
            val baseWidth = adjustedPaint.measureText(baseText)
            val diffWidth = diffPaint.measureText(diffText)
            val totalWidth = baseWidth + diffWidth
            val startX = CENTER_X - totalWidth / 2

            adjustedPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(baseText, startX, y + 28f, adjustedPaint)
            canvas.drawText(diffText, startX + baseWidth, y + 28f, diffPaint)
        }

        // 10. Date/time stamp
        val dateFormat = SimpleDateFormat("MMM d, yyyy \u00B7 h:mm a", Locale.getDefault())
        val dateText = dateFormat.format(Date(result.timestampMs))
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = poppinsMedium
            textSize = 26f
            color = TEXT_SECONDARY
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(dateText, CENTER_X, 924f, datePaint)

        // 11. Branding footer
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = poppinsBold
            textSize = 32f
            color = DARK_GREEN
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("SmackTrack", CENTER_X, 970f, brandPaint)

        return bitmap
    }

    private fun drawWindArrow(canvas: Canvas, cx: Float, cy: Float, arrowSize: Float, rotationDeg: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        canvas.save()
        canvas.rotate(rotationDeg, cx, cy)
        val half = arrowSize / 2
        val left = cx - half
        val top = cy - half
        val w = arrowSize
        val h = arrowSize
        val path = Path().apply {
            moveTo(left + w * 0.5f, top + h * 0.05f)
            lineTo(left + w * 0.2f, top + h * 0.45f)
            lineTo(left + w * 0.38f, top + h * 0.45f)
            lineTo(left + w * 0.38f, top + h * 0.95f)
            lineTo(left + w * 0.62f, top + h * 0.95f)
            lineTo(left + w * 0.62f, top + h * 0.45f)
            lineTo(left + w * 0.8f, top + h * 0.45f)
            close()
        }
        canvas.drawPath(path, paint)
        canvas.restore()
    }

    private fun clubChipColor(sortOrder: Int): Int {
        val fraction = (sortOrder - 1) / 17f
        return lerpColor(DARK_GREEN, CHIP_GREEN_LIGHT, fraction)
    }

    private fun lerpColor(start: Int, end: Int, fraction: Float): Int {
        val sA = (start shr 24) and 0xFF
        val sR = (start shr 16) and 0xFF
        val sG = (start shr 8) and 0xFF
        val sB = start and 0xFF
        val eA = (end shr 24) and 0xFF
        val eR = (end shr 16) and 0xFF
        val eG = (end shr 8) and 0xFF
        val eB = end and 0xFF
        return android.graphics.Color.argb(
            (sA + (eA - sA) * fraction).toInt(),
            (sR + (eR - sR) * fraction).toInt(),
            (sG + (eG - sG) * fraction).toInt(),
            (sB + (eB - sB) * fraction).toInt()
        )
    }

    private fun windCategoryColorInt(cat: WindCalculator.WindColorCategory): Int = when (cat) {
        WindCalculator.WindColorCategory.STRONG_HELPING  -> 0xFF2E7D32.toInt()
        WindCalculator.WindColorCategory.HELPING         -> 0xFF558B2F.toInt()
        WindCalculator.WindColorCategory.SLIGHT_HELPING  -> 0xFF9E9D24.toInt()
        WindCalculator.WindColorCategory.CROSSWIND       -> 0xFFE65100.toInt()
        WindCalculator.WindColorCategory.SLIGHT_HURTING  -> 0xFFD84315.toInt()
        WindCalculator.WindColorCategory.HURTING         -> 0xFFC62828.toInt()
        WindCalculator.WindColorCategory.STRONG_HURTING  -> 0xFFB71C1C.toInt()
    }

    private fun windStrengthLabel(windSpeedKmh: Double): String = when {
        windSpeedKmh < 6   -> "None"
        windSpeedKmh < 13  -> "Very Light"
        windSpeedKmh < 20  -> "Light"
        windSpeedKmh < 36  -> "Medium"
        windSpeedKmh < 50  -> "Strong"
        windSpeedKmh < 71  -> "Very Strong"
        else               -> "Extreme"
    }
}
