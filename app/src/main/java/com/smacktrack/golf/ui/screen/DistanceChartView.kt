package com.smacktrack.golf.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smacktrack.golf.domain.Club
import com.smacktrack.golf.location.WindCalculator
import com.smacktrack.golf.ui.AppSettings
import com.smacktrack.golf.ui.DistanceUnit
import com.smacktrack.golf.ui.ShotResult
import com.smacktrack.golf.ui.theme.ChipUnselectedBg
import com.smacktrack.golf.ui.theme.DarkGreen
import com.smacktrack.golf.ui.theme.TextPrimary
import com.smacktrack.golf.ui.theme.TextSecondary
import com.smacktrack.golf.ui.theme.TextTertiary
import com.smacktrack.golf.ui.theme.clubChipColor

// ── Data class for each club's range bar ────────────────────────────────────

private data class ClubDistanceBar(
    val club: Club,
    val min: Int,
    val max: Int,
    val avg: Int,
    val windAdjAvg: Int?,
    val shotCount: Int,
    val chipColor: Color
)

// ── Distance helper (mirrors AnalyticsScreen) ───────────────────────────────

private fun distanceFor(shot: ShotResult, useYards: Boolean, windAdjusted: Boolean, settings: AppSettings): Int {
    val raw = if (useYards) shot.distanceYards else shot.distanceMeters
    if (!windAdjusted) return raw
    val effect = WindCalculator.estimateWindEffectYards(
        shot.windSpeedKmh,
        WindCalculator.relativeWindAngle(shot.windDirectionDegrees, shot.shotBearingDegrees),
        shot.distanceYards,
        settings.trajectory.multiplier
    )
    val adjustedYards = shot.distanceYards - effect
    return if (useYards) adjustedYards else (adjustedYards * 0.9144).toInt()
}

// ── Main composable ─────────────────────────────────────────────────────────

@Composable
fun DistanceChartView(
    shots: List<ShotResult>,
    settings: AppSettings,
    selectedPeriod: TimePeriod,
    onPeriodChanged: (TimePeriod) -> Unit,
    windAdjusted: Boolean,
    onWindAdjustedChanged: (Boolean) -> Unit
) {
    val useYards = settings.distanceUnit == DistanceUnit.YARDS
    val unitLabel = if (useYards) "yds" else "m"

    // Build per-club bars
    val bars = shots
        .groupBy { it.club }
        .map { (club, clubShots) ->
            val rawDistances = clubShots.map { distanceFor(it, useYards, windAdjusted = false, settings) }
            val adjDistances = clubShots.map { distanceFor(it, useYards, windAdjusted = true, settings) }
            ClubDistanceBar(
                club = club,
                min = rawDistances.min(),
                max = rawDistances.max(),
                avg = rawDistances.average().toInt(),
                windAdjAvg = if (windAdjusted) adjDistances.average().toInt() else null,
                shotCount = clubShots.size,
                chipColor = clubChipColor(club.sortOrder)
            )
        }
        .sortedBy { it.club.sortOrder }

    // Global scale: 0 to the max distance across all clubs (rounded up to nearest 50)
    val globalMax = if (bars.isNotEmpty()) {
        val rawMax = bars.maxOf { it.max }
        ((rawMax / 50) + 1) * 50
    } else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Period filter chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TimePeriod.entries.forEach { period ->
                    val selected = period == selectedPeriod
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) DarkGreen else ChipUnselectedBg)
                            .clickable { onPeriodChanged(period) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = period.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color.White else TextSecondary
                        )
                    }
                }
            }
        }

        // Wind adjusted toggle + shot count
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${shots.size} shots across ${bars.size} clubs",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onWindAdjustedChanged(!windAdjusted) }
                        .padding(start = 4.dp, end = 8.dp)
                ) {
                    Checkbox(
                        checked = windAdjusted,
                        onCheckedChange = { onWindAdjustedChanged(it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = DarkGreen,
                            uncheckedColor = TextTertiary
                        ),
                        modifier = Modifier.height(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Wind adjusted",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (windAdjusted) DarkGreen else TextSecondary,
                        fontWeight = if (windAdjusted) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        // Scale header
        if (bars.isNotEmpty()) {
            item {
                ScaleHeader(globalMax = globalMax, unitLabel = unitLabel)
            }
        }

        // Club range bar rows
        items(bars) { bar ->
            ClubRangeRow(
                bar = bar,
                globalMax = globalMax,
                unitLabel = unitLabel,
                windAdjusted = windAdjusted
            )
        }

        // Legend
        if (bars.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                ChartLegend(windAdjusted = windAdjusted)
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ── Scale header ────────────────────────────────────────────────────────────

@Composable
private fun ScaleHeader(globalMax: Int, unitLabel: String) {
    val step = when {
        globalMax <= 100 -> 25
        globalMax <= 200 -> 50
        else -> 50
    }
    val markers = (0..globalMax step step).toList()

    // Left padding to align with bar area (chip 60dp + spacing 8dp = 68dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 68.dp, end = 44.dp, top = 4.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        ) {
            val w = size.width
            val tickColor = Color(0xFFBDBDBD)

            markers.forEach { value ->
                val x = if (globalMax > 0) w * value.toFloat() / globalMax else 0f
                drawLine(tickColor, Offset(x, 14f), Offset(x, 20f), strokeWidth = 1f)
            }
        }
        // Text labels
        markers.forEach { value ->
            val fraction = if (globalMax > 0) value.toFloat() / globalMax else 0f
            Text(
                text = "$value",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                fontSize = 8.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        start = with(androidx.compose.ui.platform.LocalDensity.current) {
                            // Approximate: we can't know exact Canvas width in dp precisely,
                            // but we use fillMaxWidth minus padding. This is a best-effort alignment.
                            0.dp
                        }
                    )
            )
        }
    }
    // Simpler approach: just show min and max labels at the edges
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 68.dp, end = 44.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "0", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 9.sp)
        Text(text = "$globalMax $unitLabel", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 9.sp)
    }
}

// ── Club range bar row ──────────────────────────────────────────────────────

@Composable
private fun ClubRangeRow(
    bar: ClubDistanceBar,
    globalMax: Int,
    unitLabel: String,
    windAdjusted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Club chip
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(bar.chipColor)
                .padding(horizontal = 6.dp, vertical = 3.dp)
                .width(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bar.club.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                fontSize = 10.sp
            )
        }

        Spacer(Modifier.width(8.dp))

        // Range bar canvas
        Box(modifier = Modifier.weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            ) {
                val w = size.width
                val h = size.height
                val centerY = h / 2f
                val trackHeight = 4.dp.toPx()
                val barHeight = 10.dp.toPx()

                // Gray background track
                drawRoundRect(
                    color = Color(0xFFE8E8E8),
                    topLeft = Offset(0f, centerY - trackHeight / 2),
                    size = androidx.compose.ui.geometry.Size(w, trackHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2)
                )

                if (globalMax > 0) {
                    val minX = w * bar.min.toFloat() / globalMax
                    val maxX = w * bar.max.toFloat() / globalMax
                    val avgX = w * bar.avg.toFloat() / globalMax
                    val barWidth = (maxX - minX).coerceAtLeast(4.dp.toPx())

                    // Colored range bar (min to max)
                    drawRoundRect(
                        color = bar.chipColor.copy(alpha = 0.4f),
                        topLeft = Offset(minX, centerY - barHeight / 2),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barHeight / 2)
                    )

                    // Average marker — filled circle
                    drawCircle(
                        color = bar.chipColor,
                        radius = 6.dp.toPx(),
                        center = Offset(avgX, centerY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = Offset(avgX, centerY)
                    )
                    drawCircle(
                        color = bar.chipColor,
                        radius = 2.5.dp.toPx(),
                        center = Offset(avgX, centerY)
                    )

                    // Wind-adjusted average marker — hollow diamond
                    if (windAdjusted && bar.windAdjAvg != null) {
                        val waX = w * bar.windAdjAvg.toFloat() / globalMax
                        val diamondSize = 5.dp.toPx()
                        val diamondPath = Path().apply {
                            moveTo(waX, centerY - diamondSize)
                            lineTo(waX + diamondSize, centerY)
                            lineTo(waX, centerY + diamondSize)
                            lineTo(waX - diamondSize, centerY)
                            close()
                        }
                        drawPath(
                            path = diamondPath,
                            color = Color(0xFFE65100),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(4.dp))

        // Average distance label
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.width(40.dp)
        ) {
            Text(
                text = "${bar.avg}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = unitLabel,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                fontSize = 9.sp
            )
        }
    }
}

// ── Chart legend ────────────────────────────────────────────────────────────

@Composable
private fun ChartLegend(windAdjusted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Range bar legend
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(DarkGreen.copy(alpha = 0.4f))
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "Min–Max",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            fontSize = 10.sp
        )

        Spacer(Modifier.width(16.dp))

        // Average dot legend
        Canvas(modifier = Modifier.height(12.dp).width(12.dp)) {
            val c = Offset(size.width / 2, size.height / 2)
            drawCircle(color = DarkGreen, radius = 4.dp.toPx(), center = c)
            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = c)
            drawCircle(color = DarkGreen, radius = 1.5.dp.toPx(), center = c)
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = "Avg",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            fontSize = 10.sp
        )

        if (windAdjusted) {
            Spacer(Modifier.width(16.dp))

            // Wind-adjusted diamond legend
            Canvas(modifier = Modifier.height(12.dp).width(12.dp)) {
                val c = Offset(size.width / 2, size.height / 2)
                val s = 4.dp.toPx()
                val path = Path().apply {
                    moveTo(c.x, c.y - s)
                    lineTo(c.x + s, c.y)
                    lineTo(c.x, c.y + s)
                    lineTo(c.x - s, c.y)
                    close()
                }
                drawPath(path, color = Color(0xFFE65100), style = Stroke(width = 1.5.dp.toPx()))
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Wind Adj",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                fontSize = 10.sp
            )
        }
    }
}
