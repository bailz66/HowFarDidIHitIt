package com.smacktrack.golf.ui.screen

/**
 * Club statistics and analytics screen.
 *
 * Shows per-club averages (AVG / LNG / SHT) with time-period filtering and
 * an optional wind-adjusted toggle. Tapping a club row drills into a detail
 * view showing individual shot history with weather and wind data.
 *
 * Uses [AnimatedContent] for smooth slide transitions between the list and detail views.
 */

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smacktrack.golf.domain.Club
import com.smacktrack.golf.location.WindCalculator
import com.smacktrack.golf.ui.AppSettings
import com.smacktrack.golf.ui.DistanceUnit
import com.smacktrack.golf.ui.ShotResult
import com.smacktrack.golf.ui.TemperatureUnit
import com.smacktrack.golf.ui.WindUnit
import com.smacktrack.golf.ui.theme.ChipUnselectedBg
import com.smacktrack.golf.ui.theme.DarkGreen
import com.smacktrack.golf.ui.theme.TextPrimary
import com.smacktrack.golf.ui.theme.TextSecondary
import com.smacktrack.golf.ui.theme.TextTertiary
import com.smacktrack.golf.ui.theme.clubChipColor
import com.smacktrack.golf.ui.theme.windCategoryColor

enum class TimePeriod(val label: String, val days: Int) {
    WEEK("7d", 7),
    MONTH("30d", 30),
    QUARTER("90d", 90),
    HALF_YEAR("180d", 180),
    YEAR("365d", 365),
    ALL("All", Int.MAX_VALUE)
}

private data class ClubStats(
    val club: Club,
    val shotCount: Int,
    val avg: Int,
    val long: Int,
    val short: Int
)

@Composable
fun AnalyticsScreen(
    shotHistory: List<ShotResult>,
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableStateOf(TimePeriod.ALL) }
    var windAdjusted by remember { mutableStateOf(false) }
    var selectedClub by remember { mutableStateOf<Club?>(null) }

    if (shotHistory.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No shots yet",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Hit some shots to see\nyour club stats here",
                style = MaterialTheme.typography.bodyLarge,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    // Time-period filtering will apply once ShotResult includes a timestamp field.
    // For now, all shots are shown regardless of the selected period.
    val filtered = shotHistory

    AnimatedContent(
        targetState = selectedClub,
        transitionSpec = {
            if (targetState != null) {
                (fadeIn() + slideInHorizontally { it / 4 }) togetherWith
                        (fadeOut() + slideOutHorizontally { -it / 4 })
            } else {
                (fadeIn() + slideInHorizontally { -it / 4 }) togetherWith
                        (fadeOut() + slideOutHorizontally { it / 4 })
            }
        },
        modifier = modifier.fillMaxSize(),
        label = "analytics"
    ) { club ->
        if (club != null) {
            ClubDetailView(
                club = club,
                shots = filtered.filter { it.club == club },
                settings = settings,
                windAdjusted = windAdjusted,
                onBack = { selectedClub = null }
            )
        } else {
            StatsListView(
                shots = filtered,
                settings = settings,
                selectedPeriod = selectedPeriod,
                onPeriodChanged = { selectedPeriod = it },
                windAdjusted = windAdjusted,
                onWindAdjustedChanged = { windAdjusted = it },
                onClubClicked = { selectedClub = it }
            )
        }
    }
}

// ── Stats List ──────────────────────────────────────────────────────────────

@Composable
private fun StatsListView(
    shots: List<ShotResult>,
    settings: AppSettings,
    selectedPeriod: TimePeriod,
    onPeriodChanged: (TimePeriod) -> Unit,
    windAdjusted: Boolean,
    onWindAdjustedChanged: (Boolean) -> Unit,
    onClubClicked: (Club) -> Unit
) {
    val useYards = settings.distanceUnit == DistanceUnit.YARDS
    val unitLabel = if (useYards) "yds" else "m"

    val stats = shots
        .groupBy { it.club }
        .map { (club, clubShots) ->
            val distances = clubShots.map { shot ->
                val raw = if (useYards) shot.distanceYards else shot.distanceMeters
                if (windAdjusted) {
                    val effect = WindCalculator.estimateWindEffectYards(
                        shot.windSpeedKmh,
                        WindCalculator.relativeWindAngle(shot.windDirectionDegrees, shot.shotBearingDegrees),
                        shot.distanceYards,
                        settings.trajectory.multiplier
                    )
                    val adjustedYards = shot.distanceYards - effect
                    if (useYards) adjustedYards else (adjustedYards * 0.9144).toInt()
                } else {
                    raw
                }
            }
            ClubStats(
                club = club,
                shotCount = clubShots.size,
                avg = distances.average().toInt(),
                long = distances.max(),
                short = distances.min()
            )
        }
        .sortedBy { it.club.sortOrder }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Period filter
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
                    text = "${shots.size} shots",
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
                        modifier = Modifier.size(20.dp)
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

        // Club rows
        items(stats) { stat ->
            CompactClubRow(
                stat = stat,
                unitLabel = unitLabel,
                onClick = { onClubClicked(stat.club) }
            )
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun CompactClubRow(
    stat: ClubStats,
    unitLabel: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Club name chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(clubChipColor(stat.club.sortOrder))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .width(52.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stat.club.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
            }

            Spacer(Modifier.width(10.dp))

            // Shot count
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(28.dp)
            ) {
                Text(
                    text = "${stat.shotCount}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if (stat.shotCount == 1) "shot" else "shots",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    fontSize = 8.sp,
                    lineHeight = 10.sp
                )
            }

            Spacer(Modifier.width(8.dp))

            // AVG / LONG / SHORT
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniStat("AVG", stat.avg, unitLabel)
                MiniStat("LNG", stat.long, unitLabel)
                MiniStat("SHT", stat.short, unitLabel)
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: Int, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            fontSize = 9.sp,
            letterSpacing = 1.sp
        )
        Text(
            text = "$value",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

// ── Club Detail ─────────────────────────────────────────────────────────────

@Composable
private fun ClubDetailView(
    club: Club,
    shots: List<ShotResult>,
    settings: AppSettings,
    windAdjusted: Boolean,
    onBack: () -> Unit
) {
    val useYards = settings.distanceUnit == DistanceUnit.YARDS
    val unitLabel = if (useYards) "yds" else "m"

    fun distanceFor(shot: ShotResult): Int {
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

    val distances = shots.map { distanceFor(it) }
    val avg = if (distances.isNotEmpty()) distances.average().toInt() else 0
    val long = distances.maxOrNull() ?: 0
    val short = distances.minOrNull() ?: 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header with back button
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextSecondary
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(clubChipColor(club.sortOrder))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = club.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${shots.size} shots",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }

        // Summary stats
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ChipUnselectedBg)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DetailStat("AVG", avg, unitLabel)
                DetailStat("LONG", long, unitLabel)
                DetailStat("SHORT", short, unitLabel)
            }
        }

        // Individual shots (most recent first)
        items(shots.reversed()) { shot ->
            ShotDetailRow(shot, settings, windAdjusted)
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun DetailStat(label: String, value: Int, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "$value",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary
        )
    }
}

@Composable
private fun ShotDetailRow(
    shot: ShotResult,
    settings: AppSettings,
    windAdjusted: Boolean
) {
    val useYards = settings.distanceUnit == DistanceUnit.YARDS
    val rawDist = if (useYards) shot.distanceYards else shot.distanceMeters
    val unitLabel = if (useYards) "yds" else "m"

    val windEffect = WindCalculator.analyze(
        windSpeedKmh = shot.windSpeedKmh,
        windFromDegrees = shot.windDirectionDegrees,
        shotBearingDegrees = shot.shotBearingDegrees,
        distanceYards = shot.distanceYards,
        trajectoryMultiplier = settings.trajectory.multiplier
    )
    val adjustedYards = shot.distanceYards - windEffect.carryEffectYards
    val adjustedDist = if (useYards) adjustedYards else (adjustedYards * 0.9144).toInt()
    val displayDist = if (windAdjusted) adjustedDist else rawDist

    val windSpeed = if (settings.windUnit == WindUnit.KMH) {
        "${shot.windSpeedKmh.toInt()} km/h"
    } else {
        "${(shot.windSpeedKmh * 0.621371).toInt()} mph"
    }

    val tempDisplay = if (settings.temperatureUnit == TemperatureUnit.FAHRENHEIT) {
        "${shot.temperatureF}\u00B0F"
    } else {
        "${shot.temperatureC}\u00B0C"
    }

    val windColor = windCategoryColor(windEffect.colorCategory)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$tempDisplay ${shot.weatherDescription}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Wind: $windSpeed ${shot.windDirectionCompass}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                    Spacer(Modifier.width(4.dp))
                    CompactWindArrow(
                        windDegrees = shot.windDirectionDegrees,
                        shotBearing = shot.shotBearingDegrees
                    )
                }
                if (windEffect.carryEffectYards != 0) {
                    val diff = windEffect.carryEffectYards
                    val diffText = if (diff >= 0) "+$diff $unitLabel" else "$diff $unitLabel"
                    Text(
                        text = "Wind effect: $diffText",
                        style = MaterialTheme.typography.labelSmall,
                        color = windColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$displayDist",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = unitLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }
    }
}
