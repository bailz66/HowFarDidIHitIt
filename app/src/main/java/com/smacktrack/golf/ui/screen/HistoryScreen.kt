package com.smacktrack.golf.ui.screen

/**
 * Shot history screen — chronological list of all recorded shots grouped by session.
 *
 * Each card shows the club name, temperature, weather description,
 * wind speed/direction with a compact wind arrow, and the shot distance.
 * Shots are grouped into sessions (< 30min gap) with date headers.
 * Most recent sessions appear at the top.
 */

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smacktrack.golf.ui.AppSettings
import com.smacktrack.golf.ui.DistanceUnit
import com.smacktrack.golf.ui.ShotResult
import com.smacktrack.golf.ui.formatTemperature
import com.smacktrack.golf.ui.formatWindSpeed
import com.smacktrack.golf.ui.primaryDistance
import com.smacktrack.golf.ui.shortUnitLabel
import com.smacktrack.golf.ui.theme.ChipUnselectedBg
import com.smacktrack.golf.ui.theme.DarkGreen
import com.smacktrack.golf.ui.theme.MidGray
import com.smacktrack.golf.ui.theme.Red40
import com.smacktrack.golf.ui.theme.TextPrimary
import com.smacktrack.golf.ui.theme.TextSecondary
import com.smacktrack.golf.ui.theme.TextTertiary
import com.smacktrack.golf.ui.theme.clubChipColor
import java.util.Calendar

@Composable
fun HistoryScreen(
    shotHistory: List<ShotResult>,
    settings: AppSettings,
    onDeleteShot: (Long) -> Unit = {},
    onShotClicked: (ShotResult) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var pendingDeleteTimestamp by remember { mutableStateOf<Long?>(null) }

    pendingDeleteTimestamp?.let { ts ->
        AlertDialog(
            onDismissRequest = { pendingDeleteTimestamp = null },
            title = { Text("Delete shot?") },
            text = { Text("This shot will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteShot(ts)
                    pendingDeleteTimestamp = null
                }) { Text("Delete", color = Red40) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTimestamp = null }) { Text("Cancel") }
            }
        )
    }
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
                text = "Tap SMACK on the Tracker tab\nto record your first shot",
                style = MaterialTheme.typography.bodyLarge,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    // Memoize session grouping — O(n log n) sort + O(n) scan, avoid on every recomposition
    val sessions = remember(shotHistory) { groupIntoSessions(shotHistory).reversed() }
    val sessionCount = sessions.size
    val sessionLabel = if (sessionCount == 1) "session" else "sessions"

    val pageSize = 5
    var visibleSessionCount by remember { mutableIntStateOf(pageSize) }

    // Only reset pagination when new shots are added, not on deletion
    var previousSize by remember { mutableIntStateOf(shotHistory.size) }
    LaunchedEffect(shotHistory.size) {
        if (shotHistory.size > previousSize) {
            visibleSessionCount = pageSize
        }
        previousSize = shotHistory.size
    }

    val visibleSessions = sessions.take(visibleSessionCount)
    val hasMore = visibleSessionCount < sessionCount
    val remaining = sessionCount - visibleSessionCount

    // Entrance animation
    val entranceProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entranceProgress.animateTo(1f, tween(500, easing = EaseOut))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .graphicsLayer {
                val p = entranceProgress.value
                alpha = p
                translationY = (1f - p) * 40f
            },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${shotHistory.size} shots",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "$sessionCount $sessionLabel" + if (hasMore) " (showing $visibleSessionCount)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }

        visibleSessions.forEachIndexed { index, session ->
            if (index > 0) {
                item { Spacer(Modifier.height(4.dp)) }
            }
            item {
                HistorySessionHeader(session)
            }

            // Inline session summary for sessions with 3+ shots
            val sessionSummary = computeSessionSummary(session.shots, settings.distanceUnit)
            if (sessionSummary != null) {
                item {
                    val unitLabel = if (settings.distanceUnit == DistanceUnit.YARDS) "yds" else "m"
                    SessionSummaryCard(summary = sessionSummary, unitLabel = unitLabel)
                }
            }

            val shotsReversed = session.shots.reversed()
            val sessionBestTs = if (session.shots.size >= 3) {
                val bestShot = session.shots.maxByOrNull {
                    if (settings.distanceUnit == DistanceUnit.YARDS) it.distanceYards else it.distanceMeters
                }
                bestShot?.timestampMs
            } else null
            items(shotsReversed, key = { it.timestampMs }) { shot ->
                ShotHistoryCard(
                    shot = shot,
                    settings = settings,
                    isSessionBest = shot.timestampMs == sessionBestTs,
                    onDelete = { pendingDeleteTimestamp = shot.timestampMs },
                    onClick = { onShotClicked(shot) }
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
private fun ShotHistoryCard(
    shot: ShotResult,
    settings: AppSettings,
    isSessionBest: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val distance = shot.primaryDistance(settings.distanceUnit)
    val distLabel = shot.shortUnitLabel(settings.distanceUnit)
    val chipColor = clubChipColor(shot.club.sortOrder)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, MidGray, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Club color accent bar — stretches to card height
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(chipColor)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Club chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(chipColor)
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = shot.club.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        if (isSessionBest) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFFF8E1))
                                    .border(1.dp, Color(0xFFFFD600), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "BEST",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFE6A800),
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${shot.formatTemperature(settings.temperatureUnit)} ${shot.weatherDescription} \u2022 ${shot.formatWindSpeed(settings.windUnit)} ${shot.windDirectionCompass}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(4.dp))
                        CompactWindArrow(
                            windDegrees = shot.windDirectionDegrees,
                            shotBearing = shot.shotBearingDegrees
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                // Distance
                Text(
                    text = "$distance",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = distLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    modifier = Modifier.padding(top = 6.dp)
                )
                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete shot",
                            tint = TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── History-specific session header with relative time ──────────────────────

private fun relativeTimeLabel(timestampMs: Long): String {
    val todayCal = Calendar.getInstance()
    val shotCal = Calendar.getInstance().apply { timeInMillis = timestampMs }

    // Strip time, compare dates only
    fun Calendar.daysSinceEpoch(): Long {
        val c = clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis / (24 * 60 * 60 * 1000L)
    }

    val dayDiff = todayCal.daysSinceEpoch() - shotCal.daysSinceEpoch()

    return when {
        dayDiff == 0L -> "Today"
        dayDiff == 1L -> "Yesterday"
        dayDiff < 7 -> "${dayDiff}d ago"
        dayDiff < 30 -> "${dayDiff / 7}w ago"
        else -> ""
    }
}

@Composable
private fun HistorySessionHeader(session: Session) {
    val relTime = remember(session) {
        if (session.shots.isNotEmpty()) relativeTimeLabel(session.shots.first().timestampMs) else ""
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(ChipUnselectedBg)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(DarkGreen)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = session.dateLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${session.shots.size} ${if (session.shots.size == 1) "shot" else "shots"}",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )
        if (relTime.isNotEmpty()) {
            Spacer(Modifier.weight(1f))
            Text(
                text = relTime,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = DarkGreen
            )
        }
    }
}
