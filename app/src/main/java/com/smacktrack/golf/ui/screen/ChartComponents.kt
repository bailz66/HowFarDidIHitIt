package com.smacktrack.golf.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smacktrack.golf.domain.Club
import com.smacktrack.golf.location.WindCalculator
import com.smacktrack.golf.ui.AppSettings
import com.smacktrack.golf.ui.DistanceUnit
import com.smacktrack.golf.ui.ShotResult
import com.smacktrack.golf.ui.WindUnit
import com.smacktrack.golf.ui.formatTemperature
import com.smacktrack.golf.ui.formatWindSpeed
import com.smacktrack.golf.ui.primaryDistance
import com.smacktrack.golf.ui.primaryUnitLabel
import com.smacktrack.golf.ui.secondaryDistance
import com.smacktrack.golf.ui.shortUnitLabel
import com.smacktrack.golf.ui.theme.ChipUnselectedBg
import com.smacktrack.golf.ui.theme.DarkGreen
import com.smacktrack.golf.ui.theme.RobotoFamily
import com.smacktrack.golf.ui.theme.TextPrimary
import com.smacktrack.golf.ui.theme.TextSecondary
import com.smacktrack.golf.ui.theme.TextTertiary
import com.smacktrack.golf.ui.theme.clubChipColor
import com.smacktrack.golf.ui.theme.windCategoryColor
import com.smacktrack.golf.ui.share.ShareUtil
import com.smacktrack.golf.ui.share.ShotCardRenderer
import com.smacktrack.golf.ui.windStrengthLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn

// ── AnimatedCounter ─────────────────────────────────────────────────────────

@Composable
fun AnimatedCounter(
    targetValue: Int,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedValue = remember { Animatable(0f) }
    LaunchedEffect(targetValue) {
        animatedValue.snapTo(0f)
        animatedValue.animateTo(
            targetValue.toFloat(),
            animationSpec = tween(600, easing = FastOutSlowInEasing)
        )
    }
    Text(
        text = "${animatedValue.value.toInt()}",
        style = style,
        color = color,
        modifier = modifier
    )
}

// ── Session grouping ────────────────────────────────────────────────────────

data class Session(
    val index: Int,
    val dateLabel: String,
    val shots: List<ShotResult>
)

private const val SESSION_GAP_MS = 30 * 60 * 1000L // 30 minutes

fun groupIntoSessions(shots: List<ShotResult>): List<Session> {
    if (shots.isEmpty()) return emptyList()

    val sorted = shots.sortedBy { it.timestampMs }
    val groups = mutableListOf<MutableList<ShotResult>>()
    var current = mutableListOf(sorted.first())

    for (i in 1 until sorted.size) {
        if (sorted[i].timestampMs - sorted[i - 1].timestampMs > SESSION_GAP_MS) {
            groups.add(current)
            current = mutableListOf(sorted[i])
        } else {
            current.add(sorted[i])
        }
    }
    groups.add(current)

    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return groups.mapIndexed { idx, group ->
        Session(
            index = idx + 1,
            dateLabel = dateFormat.format(Date(group.first().timestampMs)),
            shots = group
        )
    }
}

// ── Session Summary ─────────────────────────────────────────────────────────

data class SessionSummary(
    val totalShots: Int,
    val avgDistance: Int,
    val bestClub: Club,
    val bestDistance: Int,
    val clubsUsedCount: Int
)

/**
 * Computes a summary for the given session shots. Returns null if fewer than 3 shots.
 */
fun computeSessionSummary(shots: List<ShotResult>, distanceUnit: DistanceUnit): SessionSummary? {
    if (shots.size < 3) return null
    val distances = shots.map { if (distanceUnit == DistanceUnit.YARDS) it.distanceYards else it.distanceMeters }
    val bestIndex = distances.indices.maxBy { distances[it] }
    return SessionSummary(
        totalShots = shots.size,
        avgDistance = distances.average().toInt(),
        bestClub = shots[bestIndex].club,
        bestDistance = distances[bestIndex],
        clubsUsedCount = shots.map { it.club }.distinct().size
    )
}

/**
 * Returns the most recent active session if the last shot was within 30 minutes of now.
 */
fun currentActiveSession(shots: List<ShotResult>): Session? {
    if (shots.isEmpty()) return null
    val sessions = groupIntoSessions(shots)
    val latest = sessions.lastOrNull() ?: return null
    val lastShotTime = latest.shots.maxOf { it.timestampMs }
    return if (System.currentTimeMillis() - lastShotTime <= SESSION_GAP_MS) latest else null
}

// ── SessionSummaryCard composable ───────────────────────────────────────────

@Composable
fun SessionSummaryCard(summary: SessionSummary, unitLabel: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ChipUnselectedBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryStatColumn("${summary.totalShots}", "Shots")
            SummaryStatColumn("${summary.avgDistance}", "Avg $unitLabel")
            SummaryStatColumn(
                "${summary.bestDistance}",
                "Best",
                valueColor = DarkGreen
            )
            SummaryStatColumn("${summary.clubsUsedCount}", "Clubs")
        }
    }
}

@Composable
private fun SummaryStatColumn(
    value: String,
    label: String,
    valueColor: Color = TextPrimary
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )
    }
}

// ── Trend direction ─────────────────────────────────────────────────────────

enum class TrendDirection { UP, DOWN, FLAT }

fun computeTrend(distances: List<Int>): TrendDirection {
    if (distances.size < 4) return TrendDirection.FLAT
    val recent3 = distances.takeLast(3).average()
    val overall = distances.average()
    val diff = recent3 - overall
    return when {
        diff > 3 -> TrendDirection.UP
        diff < -3 -> TrendDirection.DOWN
        else -> TrendDirection.FLAT
    }
}

fun stdDev(values: List<Int>): Int {
    if (values.size < 2) return 0
    val mean = values.average()
    val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
    return sqrt(variance).toInt()
}

// ── DistanceSparkline ───────────────────────────────────────────────────────

@Composable
fun DistanceSparkline(
    distances: List<Int>,
    lineColor: Color = DarkGreen,
    modifier: Modifier = Modifier
) {
    if (distances.size < 2) return

    val data = distances.takeLast(20)
    val avg = data.average().toFloat()
    val minVal = (data.min() - 10).toFloat()
    val maxVal = (data.max() + 10).toFloat()
    val range = (maxVal - minVal).coerceAtLeast(20f)

    val gridColor = Color(0xFFE0E2DC)
    val avgDashColor = Color(0xFF9E9E9E)
    val dotColor = lineColor

    // Draw-in animation: progress 0→1
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(800))
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val w = size.width
        val h = size.height
        val padTop = 8f
        val padBottom = 8f
        val chartH = h - padTop - padBottom

        // Grid lines (3 horizontal)
        for (i in 0..2) {
            val y = padTop + chartH * i / 2f
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }

        // Average dashed line
        val avgY = padTop + chartH * (1f - (avg - minVal) / range)
        drawLine(
            avgDashColor,
            Offset(0f, avgY),
            Offset(w, avgY),
            strokeWidth = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
        )

        // Data points + line segments
        val points = data.mapIndexed { i, v ->
            val x = if (data.size == 1) w / 2f else w * i / (data.size - 1).toFloat()
            val y = padTop + chartH * (1f - (v - minVal) / range)
            Offset(x, y)
        }

        val visibleSegments = (points.size * progress.value).toInt()
        for (i in 0 until (points.size - 1).coerceAtMost(visibleSegments)) {
            drawLine(lineColor, points[i], points[i + 1], strokeWidth = 2.5f, cap = StrokeCap.Round)
        }

        points.forEachIndexed { i, pt ->
            val dotAlpha = ((progress.value * points.size) - i).coerceIn(0f, 1f)
            drawCircle(Color.White.copy(alpha = dotAlpha), radius = 5f, center = pt)
            drawCircle(dotColor.copy(alpha = dotAlpha), radius = 3.5f, center = pt)
        }
    }
}

// ── MiniSparkline ───────────────────────────────────────────────────────────

@Composable
fun MiniSparkline(
    distances: List<Int>,
    lineColor: Color = DarkGreen,
    modifier: Modifier = Modifier
) {
    if (distances.size < 2) return

    val data = distances.takeLast(5)
    val minVal = data.min().toFloat()
    val maxVal = data.max().toFloat()
    val range = (maxVal - minVal).coerceAtLeast(5f)

    Canvas(
        modifier = modifier.size(width = 40.dp, height = 20.dp)
    ) {
        val w = size.width
        val h = size.height
        val pad = 2f

        val points = data.mapIndexed { i, v ->
            val x = if (data.size == 1) w / 2f else pad + (w - 2 * pad) * i / (data.size - 1).toFloat()
            val y = pad + (h - 2 * pad) * (1f - (v - minVal) / range)
            Offset(x, y)
        }

        for (i in 0 until points.size - 1) {
            drawLine(lineColor, points[i], points[i + 1], strokeWidth = 1.5f, cap = StrokeCap.Round)
        }
    }
}

// ── BagSummaryChart ─────────────────────────────────────────────────────────

data class BagClubSummary(
    val club: com.smacktrack.golf.domain.Club,
    val avg: Int,
    val chipColor: Color
)

@Composable
fun BagSummaryChart(
    clubs: List<BagClubSummary>,
    unitLabel: String,
    modifier: Modifier = Modifier
) {
    if (clubs.size < 2) return

    val sorted = clubs.sortedByDescending { it.avg }
    val maxAvg = sorted.first().avg.toFloat().coerceAtLeast(1f)

    // Bar grow animation
    var animateTarget by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateTarget = true }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        sorted.forEachIndexed { index, club ->
            val animatedFraction by animateFloatAsState(
                targetValue = if (animateTarget) club.avg / maxAvg else 0f,
                animationSpec = tween(600, delayMillis = index * 80),
                label = "barGrow$index"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Club chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(club.chipColor)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .width(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = club.club.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        fontSize = 10.sp
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Horizontal bar — animated grow
                Box(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = animatedFraction)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(club.chipColor.copy(alpha = 0.3f))
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Distance label
                Text(
                    text = "${club.avg}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.width(32.dp)
                )
            }

            // Gap warning
            if (index < sorted.size - 1) {
                val gap = sorted[index].avg - sorted[index + 1].avg
                if (gap > 15) {
                    Text(
                        text = "${gap} $unitLabel gap",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(start = 64.dp)
                    )
                }
            }
        }
    }
}

// ── Shot Scatter Strip ──────────────────────────────────────────────────────

@Composable
fun ShotScatterStrip(
    distances: List<Int>,
    dotColor: Color,
    modifier: Modifier = Modifier
) {
    if (distances.size < 2) return

    val minDist = distances.min()
    val maxDist = distances.max()
    val range = (maxDist - minDist).toFloat().coerceAtLeast(1f)
    val avg = distances.average().toFloat()

    val gridColor = Color(0xFFBDBDBD)
    val avgDashColor = Color(0xFF9E9E9E)

    // Stagger-in animation
    val scatterProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scatterProgress.animateTo(1f, animationSpec = tween(600))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            val w = size.width
            val h = size.height
            val centerY = h / 2f
            val padH = 16f

            // Horizontal track line
            drawLine(
                gridColor,
                Offset(padH, centerY),
                Offset(w - padH, centerY),
                strokeWidth = 1.dp.toPx()
            )

            // Dashed vertical avg line
            val avgX = padH + (w - 2 * padH) * ((avg - minDist) / range)
            drawLine(
                avgDashColor,
                Offset(avgX, 4f),
                Offset(avgX, h - 4f),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            )

            // Dots with deterministic vertical jitter — staggered entrance
            distances.forEachIndexed { i, dist ->
                val dotAlpha = ((scatterProgress.value * distances.size) - i).coerceIn(0f, 1f)
                val x = padH + (w - 2 * padH) * ((dist - minDist) / range)
                // Deterministic jitter based on index to avoid overlap
                val jitter = ((i * 7 + dist * 3) % 11 - 5) / 5f * (h * 0.28f)
                val y = centerY + jitter

                // White outline
                drawCircle(
                    color = Color.White.copy(alpha = dotAlpha),
                    radius = 7.dp.toPx() / 2f,
                    center = Offset(x, y)
                )
                // Filled dot
                drawCircle(
                    color = dotColor.copy(alpha = dotAlpha),
                    radius = 5.dp.toPx() / 2f,
                    center = Offset(x, y)
                )
            }
        }

        // Min/max labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$minDist",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                fontSize = 9.sp
            )
            Text(
                text = "avg ${avg.toInt()}",
                style = MaterialTheme.typography.labelSmall,
                color = avgDashColor,
                fontSize = 9.sp
            )
            Text(
                text = "$maxDist",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                fontSize = 9.sp
            )
        }
    }
}

// ── Session header composable ───────────────────────────────────────────────

@Composable
fun SessionHeader(session: Session) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(DarkGreen)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = session.dateLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${session.shots.size} ${if (session.shots.size == 1) "shot" else "shots"}",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )
    }
}

// ── Show More button ────────────────────────────────────────────────────────

@Composable
fun ShowMoreButton(remainingSessions: Int, onClick: () -> Unit) {
    val label = "Show more ($remainingSessions ${if (remainingSessions == 1) "session" else "sessions"})"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ChipUnselectedBg)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = DarkGreen
        )
    }
}

// ── Shared TextStyle ────────────────────────────────────────────────────────

val DistanceResult = TextStyle(
    fontFamily = RobotoFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 80.sp,
    lineHeight = 80.sp,
    letterSpacing = (-1).sp,
    fontFeatureSettings = "tnum"
)

// ── Club Badge ──────────────────────────────────────────────────────────────

@Composable
fun ClubBadge(club: Club) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(clubChipColor(club.sortOrder))
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            text = club.displayName,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 1.sp
        )
    }
}

// ── Weather + Wind Strip ────────────────────────────────────────────────────

@Composable
fun WeatherWindStrip(
    shot: ShotResult,
    settings: AppSettings,
    editable: Boolean = false,
    onDirectionTap: (() -> Unit)? = null,
    onSpeedUp: (() -> Unit)? = null,
    onSpeedDown: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ChipUnselectedBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = shot.formatTemperature(settings.temperatureUnit),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = shot.weatherDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            WindIndicator(
                windSpeedLabel = shot.formatWindSpeed(settings.windUnit),
                windCompass = shot.windDirectionCompass,
                windDegrees = shot.windDirectionDegrees,
                shotBearing = shot.shotBearingDegrees,
                windSpeedKmh = shot.windSpeedKmh,
                distanceYards = shot.distanceYards,
                trajectoryMultiplier = settings.trajectory.multiplier,
                showEffect = true,
                onDirectionTap = if (editable) onDirectionTap else null,
                onSpeedUp = if (editable) onSpeedUp else null,
                onSpeedDown = if (editable) onSpeedDown else null
            )
        }
    }
}

// ── Weather-Adjusted Distance ───────────────────────────────────────────────

@Composable
fun WeatherAdjustedDistance(shot: ShotResult, settings: AppSettings) {
    if (shot.windSpeedKmh <= 0 && shot.temperatureF == 70) return

    val weatherEffect = remember(shot, settings) {
        WindCalculator.analyze(
            windSpeedKmh = shot.windSpeedKmh,
            windFromDegrees = shot.windDirectionDegrees,
            shotBearingDegrees = shot.shotBearingDegrees,
            distanceYards = shot.distanceYards,
            trajectoryMultiplier = settings.trajectory.multiplier,
            temperatureF = shot.temperatureF
        )
    }
    val adjustedYards = (shot.distanceYards - weatherEffect.totalWeatherEffectYards).coerceAtLeast(0)
    val adjustedMeters = (adjustedYards * 0.9144).toInt()
    val adjustedDisplay = if (settings.distanceUnit == DistanceUnit.YARDS) "$adjustedYards" else "$adjustedMeters"
    val unitLabel = shot.shortUnitLabel(settings.distanceUnit)
    val diff = weatherEffect.totalWeatherEffectYards
    val diffText = if (diff >= 0) "(+$diff)" else "($diff)"

    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Weather Adjusted: $adjustedDisplay $unitLabel ",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
        Text(
            text = diffText,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = windCategoryColor(weatherEffect.colorCategory)
        )
    }
}

// ── Share Shot Button ───────────────────────────────────────────────────────

@Composable
fun ShareShotButton(shot: ShotResult, settings: AppSettings, shotHistory: List<ShotResult>) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable {
                var bitmap: android.graphics.Bitmap? = null
                try {
                    bitmap = ShotCardRenderer.render(
                        context = context,
                        result = shot,
                        settings = settings,
                        shotHistory = shotHistory
                    )
                    ShareUtil.shareShotCard(context, bitmap)
                } catch (e: Exception) {
                    android.util.Log.e("ShareShotButton", "Failed to share shot card", e)
                } finally {
                    bitmap?.recycle()
                }
            }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Share shot",
            tint = DarkGreen,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = "Share",
            style = MaterialTheme.typography.labelLarge,
            color = DarkGreen,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Achievement Unlock Banner ───────────────────────────────────────────────

@Composable
fun AchievementUnlockBanner(
    emoji: String,
    title: String,
    description: String
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + scaleIn(
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFFFF8E1))
                .border(1.dp, Color(0xFFFFE082), RoundedCornerShape(16.dp))
                .padding(vertical = 16.dp, horizontal = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = emoji,
                    fontSize = 28.sp
                )
                Column {
                    Text(
                        text = "ACHIEVEMENT UNLOCKED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6D4C41),
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4E342E)
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8D6E63)
                    )
                }
            }
        }
    }
}
