package com.smacktrack.golf.ui.screen

/**
 * Club statistics and analytics screen.
 *
 * Shows per-club averages (AVG / LNG / SHT) with time-period filtering,
 * a "Your Bag" summary card, mini sparklines, trend indicators, and
 * an optional weather-adjusted toggle. Tapping a club row drills into a detail
 * view showing a distance trend chart and session-grouped shot history.
 *
 * Uses [AnimatedContent] for smooth slide transitions between the list and detail views.
 */

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

private val cardBorderColor = Color(0xFFE0E2DC)

private enum class StatsView { CLUBS, DISTANCES }

private val categoryOrder = Club.Category.entries

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
    val short: Int,
    val recentDistances: List<Int>,
    val trendDirection: TrendDirection,
    val spread: Int
)

@Composable
fun AnalyticsScreen(
    shotHistory: List<ShotResult>,
    settings: AppSettings,
    onDeleteShot: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableStateOf(TimePeriod.ALL) }
    var weatherAdjusted by remember { mutableStateOf(false) }
    var selectedClub by remember { mutableStateOf<Club?>(null) }
    var viewMode by remember { mutableStateOf(StatsView.CLUBS) }

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

    // Time-period filtering using timestampMs
    val filtered = if (selectedPeriod == TimePeriod.ALL) {
        shotHistory
    } else {
        val cutoffMs = System.currentTimeMillis() - selectedPeriod.days.toLong() * 24 * 60 * 60 * 1000
        shotHistory.filter { it.timestampMs >= cutoffMs }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Segmented toggle — only visible when not in club detail
        if (selectedClub == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                StatsView.entries.forEach { mode ->
                    val selected = mode == viewMode
                    val label = when (mode) {
                        StatsView.CLUBS -> "Clubs"
                        StatsView.DISTANCES -> "Distances"
                    }
                    Box(
                        modifier = Modifier
                            .clip(
                                when (mode) {
                                    StatsView.CLUBS -> RoundedCornerShape(
                                        topStart = 10.dp, bottomStart = 10.dp,
                                        topEnd = 0.dp, bottomEnd = 0.dp
                                    )
                                    StatsView.DISTANCES -> RoundedCornerShape(
                                        topStart = 0.dp, bottomStart = 0.dp,
                                        topEnd = 10.dp, bottomEnd = 10.dp
                                    )
                                }
                            )
                            .background(if (selected) DarkGreen else ChipUnselectedBg)
                            .clickable { viewMode = mode }
                            .padding(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color.White else TextSecondary
                        )
                    }
                }
            }
        }

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
            modifier = Modifier.fillMaxSize(),
            label = "analytics"
        ) { club ->
            if (club != null) {
                ClubDetailView(
                    club = club,
                    shots = filtered.filter { it.club == club },
                    allShots = shotHistory,
                    settings = settings,
                    weatherAdjusted = weatherAdjusted,
                    onWeatherAdjustedChanged = { weatherAdjusted = it },
                    onDeleteShot = onDeleteShot,
                    onBack = { selectedClub = null }
                )
            } else {
                when (viewMode) {
                    StatsView.CLUBS -> StatsListView(
                        shots = filtered,
                        settings = settings,
                        selectedPeriod = selectedPeriod,
                        onPeriodChanged = { selectedPeriod = it },
                        weatherAdjusted = weatherAdjusted,
                        onWeatherAdjustedChanged = { weatherAdjusted = it },
                        onClubClicked = { selectedClub = it }
                    )
                    StatsView.DISTANCES -> DistanceChartView(
                        shots = filtered,
                        settings = settings,
                        selectedPeriod = selectedPeriod,
                        onPeriodChanged = { selectedPeriod = it },
                        weatherAdjusted = weatherAdjusted,
                        onWeatherAdjustedChanged = { weatherAdjusted = it }
                    )
                }
            }
        }
    }
}

// ── Distance helper ─────────────────────────────────────────────────────────

private fun distanceFor(shot: ShotResult, useYards: Boolean, weatherAdjusted: Boolean, settings: AppSettings): Int {
    val raw = if (useYards) shot.distanceYards else shot.distanceMeters
    if (!weatherAdjusted) return raw
    val effect = WindCalculator.analyze(
        windSpeedKmh = shot.windSpeedKmh,
        windFromDegrees = shot.windDirectionDegrees,
        shotBearingDegrees = shot.shotBearingDegrees,
        distanceYards = shot.distanceYards,
        trajectoryMultiplier = settings.trajectory.multiplier,
        temperatureF = shot.temperatureF
    )
    val adjustedYards = shot.distanceYards - effect.totalWeatherEffectYards
    return if (useYards) adjustedYards else (adjustedYards * 0.9144).toInt()
}

// ── Category Section Header ─────────────────────────────────────────────────

@Composable
private fun CategorySectionHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = TextTertiary,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
    )
}

// ── Highlight Cards ─────────────────────────────────────────────────────────

@Composable
private fun HighlightChip(label: String, value: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, cardBorderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = DarkGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
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
    weatherAdjusted: Boolean,
    onWeatherAdjustedChanged: (Boolean) -> Unit,
    onClubClicked: (Club) -> Unit
) {
    val useYards = settings.distanceUnit == DistanceUnit.YARDS
    val unitLabel = if (useYards) "yds" else "m"
    val context = LocalContext.current

    val stats = shots
        .groupBy { it.club }
        .map { (club, clubShots) ->
            val sortedShots = clubShots.sortedBy { it.timestampMs }
            val distances = sortedShots.map { distanceFor(it, useYards, weatherAdjusted, settings) }
            ClubStats(
                club = club,
                shotCount = clubShots.size,
                avg = distances.average().toInt(),
                long = distances.max(),
                short = distances.min(),
                recentDistances = distances.takeLast(5),
                trendDirection = computeTrend(distances),
                spread = stdDev(distances)
            )
        }
        .sortedBy { it.club.sortOrder }

    // Bag summary data
    val bagClubs = stats.map { BagClubSummary(it.club, it.avg, clubChipColor(it.club.sortOrder)) }

    // Highlight data
    val longest = stats.maxByOrNull { it.long }
    val mostConsistent = stats.filter { it.shotCount >= 3 }.minByOrNull { it.spread }
    val mostImproved = stats.filter { it.trendDirection == TrendDirection.UP }
        .maxByOrNull { it.shotCount }
    val showHighlights = stats.count { it.shotCount >= 3 } >= 3

    // Group stats by category
    val statsByCategory = stats.groupBy { it.club.category }

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

        // Wind adjusted toggle + shot count + share button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${shots.size} shots",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                    if (bagClubs.size >= 2) {
                        IconButton(
                            onClick = {
                                val text = buildString {
                                    appendLine("My SmackTrack Bag:")
                                    bagClubs.sortedByDescending { it.avg }.forEach { club ->
                                        appendLine("${club.club.displayName} — ${club.avg} $unitLabel")
                                    }
                                    append("${shots.size} shots tracked")
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share your bag"))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share bag summary",
                                tint = TextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onWeatherAdjustedChanged(!weatherAdjusted) }
                        .padding(start = 4.dp, end = 8.dp)
                ) {
                    Checkbox(
                        checked = weatherAdjusted,
                        onCheckedChange = { onWeatherAdjustedChanged(it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = DarkGreen,
                            uncheckedColor = TextTertiary
                        ),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Weather adj.",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (weatherAdjusted) DarkGreen else TextSecondary,
                        fontWeight = if (weatherAdjusted) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        // YOUR BAG summary card with border
        if (bagClubs.size >= 2) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cardBorderColor, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "YOUR BAG",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextTertiary,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        BagSummaryChart(clubs = bagClubs, unitLabel = unitLabel)
                    }
                }
            }

        }

        // Highlight cards
        if (showHighlights) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    longest?.let {
                        HighlightChip("LONGEST", "${it.club.displayName} \u2022 ${it.long} $unitLabel")
                    }
                    mostConsistent?.let {
                        HighlightChip("MOST CONSISTENT", "${it.club.displayName} \u2022 \u00B1${it.spread}")
                    }
                    mostImproved?.let {
                        HighlightChip("MOST IMPROVED", it.club.displayName)
                    }
                }
            }
        }

        // Club rows grouped by category
        categoryOrder.forEach { category ->
            val categoryStats = statsByCategory[category] ?: return@forEach
            if (categoryStats.isNotEmpty()) {
                item {
                    CategorySectionHeader(category.name + "S")
                }
                items(categoryStats) { stat ->
                    CompactClubRow(
                        stat = stat,
                        unitLabel = unitLabel,
                        onClick = { onClubClicked(stat.club) }
                    )
                }
            }
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
    val chipColor = clubChipColor(stat.club.sortOrder)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Club name chip
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(chipColor)
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

        Text(
            text = "${stat.shotCount} ${if (stat.shotCount == 1) "shot" else "shots"}",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )

        Spacer(Modifier.weight(1f))

        // Mini sparkline
        MiniSparkline(
            distances = stat.recentDistances,
            lineColor = chipColor
        )

        Spacer(Modifier.width(10.dp))

        // AVG distance
        Text(
            text = "${stat.avg}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = unitLabel,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )
    }
}

// ── Club Detail ─────────────────────────────────────────────────────────────

@Composable
private fun ClubDetailView(
    club: Club,
    shots: List<ShotResult>,
    allShots: List<ShotResult>,
    settings: AppSettings,
    weatherAdjusted: Boolean,
    onWeatherAdjustedChanged: (Boolean) -> Unit,
    onDeleteShot: (Int) -> Unit = {},
    onBack: () -> Unit
) {
    var pendingDeleteIndex by remember { mutableStateOf<Int?>(null) }

    pendingDeleteIndex?.let { index ->
        AlertDialog(
            onDismissRequest = { pendingDeleteIndex = null },
            title = { Text("Delete shot?") },
            text = { Text("This shot will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteShot(index)
                    pendingDeleteIndex = null
                }) { Text("Delete", color = Color(0xFFE53935)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteIndex = null }) { Text("Cancel") }
            }
        )
    }
    val useYards = settings.distanceUnit == DistanceUnit.YARDS
    val unitLabel = if (useYards) "yds" else "m"

    val sortedShots = shots.sortedBy { it.timestampMs }
    val distances = sortedShots.map { distanceFor(it, useYards, weatherAdjusted, settings) }
    val avg = if (distances.isNotEmpty()) distances.average().toInt() else 0
    val long = distances.maxOrNull() ?: 0
    val short = distances.minOrNull() ?: 0
    val trend = computeTrend(distances)
    val trendLabel = when (trend) {
        TrendDirection.UP -> "Improving"
        TrendDirection.DOWN -> "Declining"
        TrendDirection.FLAT -> "Stable"
    }
    val trendColor = when (trend) {
        TrendDirection.UP -> Color(0xFF2E7D32)
        TrendDirection.DOWN -> Color(0xFFC62828)
        TrendDirection.FLAT -> TextTertiary
    }

    val chipColor = clubChipColor(club.sortOrder)
    val sessions = groupIntoSessions(shots).reversed()

    val pageSize = 5
    var visibleSessionCount by remember { mutableIntStateOf(pageSize) }

    LaunchedEffect(club, shots.size) {
        visibleSessionCount = pageSize
    }

    val visibleSessions = sessions.take(visibleSessionCount)
    val hasMore = visibleSessionCount < sessions.size
    val remaining = sessions.size - visibleSessionCount

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header with back button — gradient background
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                chipColor.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(8.dp)
            ) {
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
                            .background(chipColor)
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

                    Spacer(Modifier.weight(1f))

                    // Weather adjusted toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onWeatherAdjustedChanged(!weatherAdjusted) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = weatherAdjusted,
                            onCheckedChange = { onWeatherAdjustedChanged(it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = DarkGreen,
                                uncheckedColor = TextTertiary
                            ),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Weather adj.",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (weatherAdjusted) DarkGreen else TextSecondary,
                            fontWeight = if (weatherAdjusted) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
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

        // Consistency dot strip
        if (distances.size >= 2) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cardBorderColor, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "CONSISTENCY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextTertiary,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        ShotScatterStrip(
                            distances = distances,
                            dotColor = chipColor
                        )
                    }
                }
            }
        }

        // Distance trend sparkline
        if (distances.size >= 2) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cardBorderColor, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DISTANCE TREND",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextTertiary,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = trendLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = trendColor
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        DistanceSparkline(
                            distances = distances,
                            lineColor = chipColor
                        )
                    }
                }
            }
        }

        // Session-grouped individual shots (most recent session first)
        visibleSessions.forEach { session ->
            item {
                SessionHeader(session)
            }
            val sessionShotsReversed = session.shots.reversed()
            items(sessionShotsReversed) { shot ->
                val actualIndex = allShots.indexOf(shot)
                ShotDetailRow(
                    shot = shot,
                    settings = settings,
                    weatherAdjusted = weatherAdjusted,
                    clubLong = long,
                    onDelete = if (actualIndex >= 0) {{ pendingDeleteIndex = actualIndex }} else null
                )
            }
        }

        if (hasMore) {
            item {
                ShowMoreButton(remainingSessions = remaining) {
                    visibleSessionCount += pageSize
                }
            }
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
    weatherAdjusted: Boolean,
    clubLong: Int = 0,
    onDelete: (() -> Unit)? = null
) {
    val useYards = settings.distanceUnit == DistanceUnit.YARDS
    val rawDist = if (useYards) shot.distanceYards else shot.distanceMeters
    val unitLabel = if (useYards) "yds" else "m"

    val weatherEffect = WindCalculator.analyze(
        windSpeedKmh = shot.windSpeedKmh,
        windFromDegrees = shot.windDirectionDegrees,
        shotBearingDegrees = shot.shotBearingDegrees,
        distanceYards = shot.distanceYards,
        trajectoryMultiplier = settings.trajectory.multiplier,
        temperatureF = shot.temperatureF
    )
    val adjustedYards = shot.distanceYards - weatherEffect.totalWeatherEffectYards
    val adjustedDist = if (useYards) adjustedYards else (adjustedYards * 0.9144).toInt()
    val displayDist = if (weatherAdjusted) adjustedDist else rawDist

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

    val windColor = windCategoryColor(weatherEffect.colorCategory)

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
                if (weatherEffect.carryEffectYards != 0) {
                    val diff = weatherEffect.carryEffectYards
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (clubLong > 0 && displayDist >= clubLong) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFFD600))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "PB",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF5D4037),
                                fontSize = 9.sp
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = "$displayDist",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Text(
                    text = unitLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete shot",
                        tint = TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
