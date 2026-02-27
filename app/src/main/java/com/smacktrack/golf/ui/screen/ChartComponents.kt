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
import com.smacktrack.golf.ui.ShotResult
import com.smacktrack.golf.ui.theme.ChipUnselectedBg
import com.smacktrack.golf.ui.theme.DarkGreen
import com.smacktrack.golf.ui.theme.TextPrimary
import com.smacktrack.golf.ui.theme.TextSecondary
import com.smacktrack.golf.ui.theme.TextTertiary
import com.smacktrack.golf.ui.theme.clubChipColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

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
