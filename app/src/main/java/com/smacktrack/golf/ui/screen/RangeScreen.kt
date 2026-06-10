package com.smacktrack.golf.ui.screen

/**
 * Range Mode screen — the "one smack, many tracks" workflow.
 *
 * Mirrors the visual language of [ShotTrackerScreen] and reuses its calibration, button, and chip
 * composables. Four phases ([RangePhase]):
 * 1. **Idle** — pick the club you're dialing in, tap SMACK to lock your hitting spot
 * 2. **Calibrating origin** — GPS lock on the single origin (reuses [CalibratingContent])
 * 3. **Tracking** — live distance from origin, TRACK each ball, running dispersion stats + ball list
 * 4. **Summary** — session recap (count / avg / longest / shortest / spread)
 *
 * Each tracked ball is a normal [ShotResult] saved to history, so Stats and History pick them up
 * automatically and group them into one session.
 */

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smacktrack.golf.domain.Club
import com.smacktrack.golf.ui.DistanceUnit
import com.smacktrack.golf.ui.RangePhase
import com.smacktrack.golf.ui.ShotResult
import com.smacktrack.golf.ui.ShotTrackerUiState
import com.smacktrack.golf.ui.primaryDistance
import com.smacktrack.golf.ui.theme.ChipUnselectedBg
import com.smacktrack.golf.ui.theme.DarkGreen
import com.smacktrack.golf.ui.theme.Red40
import com.smacktrack.golf.ui.theme.RobotoFamily
import com.smacktrack.golf.ui.theme.TextPrimary
import com.smacktrack.golf.ui.theme.TextSecondary
import com.smacktrack.golf.ui.theme.TextTertiary
import androidx.compose.ui.text.TextStyle
import kotlin.math.roundToInt

@Composable
fun RangeScreen(
    uiState: ShotTrackerUiState,
    onSelectClub: (Club) -> Unit,
    onStartSession: () -> Unit,
    onTrackBall: () -> Unit,
    onEndSession: () -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings

    // Keep the screen awake while calibrating or tracking
    val keepScreenOn = uiState.rangePhase == RangePhase.CALIBRATING_ORIGIN ||
        uiState.rangePhase == RangePhase.TRACKING
    val activity = (LocalContext.current as? Activity)
    DisposableEffect(keepScreenOn) {
        if (keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    AnimatedContent(
        targetState = uiState.rangePhase,
        transitionSpec = {
            val isForward = targetState.ordinal > initialState.ordinal
            if (isForward) {
                (fadeIn(tween(400)) + scaleIn(initialScale = 0.95f, animationSpec = tween(400)) +
                    slideInVertically(tween(400)) { it / 12 }) togetherWith
                    (fadeOut(tween(250)) + slideOutVertically(tween(250)) { -it / 10 })
            } else {
                (fadeIn(tween(300)) + slideInVertically(tween(300)) { -it / 10 }) togetherWith
                    (fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 8 })
            }
        },
        modifier = modifier.fillMaxSize(),
        label = "rangePhase"
    ) { phase ->
        when (phase) {
            RangePhase.IDLE -> RangeIdleContent(
                selectedClub = uiState.rangeClub,
                enabledClubs = settings.enabledClubs,
                onSelectClub = onSelectClub,
                onStart = onStartSession
            )
            RangePhase.CALIBRATING_ORIGIN -> CalibratingContent(
                // "start" in the label routes to the real-accuracy adaptive UI + "Hold still at your ball"
                label = "Marking start position",
                realAccuracyMeters = uiState.calibrationAccuracyMeters,
                realProgress = uiState.calibrationProgress,
                onCancel = onReset
            )
            RangePhase.TRACKING -> RangeTrackingContent(
                uiState = uiState,
                onTrackBall = onTrackBall,
                onEndSession = onEndSession
            )
            RangePhase.SUMMARY -> RangeSummaryContent(
                club = uiState.rangeClub,
                balls = uiState.rangeBalls,
                distanceUnit = settings.distanceUnit,
                onDone = onDone
            )
        }
    }
}

// ── Idle: pick a club, smack a bucket ───────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RangeIdleContent(
    selectedClub: Club,
    enabledClubs: Set<Club>,
    onSelectClub: (Club) -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        Text(
            text = "RANGE MODE",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "One smack, many tracks — dial in a single club.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "CLUB",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Club.entries
                .filter { it in enabledClubs }
                .sortedBy { it.sortOrder }
                .forEach { c ->
                    ClubChip(
                        club = c,
                        selected = c == selectedClub,
                        onClick = { onSelectClub(c) }
                    )
                }
        }

        Spacer(Modifier.height(32.dp))

        EpicButton(text = "SMACK", pulsate = true) { onStart() }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(ChipUnselectedBg)
                .padding(16.dp)
        ) {
            Text(
                text = "Hit a bucket of balls, then walk out and TRACK each one. " +
                    "Every ball is measured from where you stood and saved to your stats.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── Tracking: live distance + TRACK + running stats + ball list ─────────────

@Composable
private fun RangeTrackingContent(
    uiState: ShotTrackerUiState,
    onTrackBall: () -> Unit,
    onEndSession: () -> Unit
) {
    var showEndConfirm by remember { mutableStateOf(false) }
    val settings = uiState.settings
    val useYards = settings.distanceUnit == DistanceUnit.YARDS
    val unitShort = if (useYards) "yd" else "m"
    val liveDistance = if (useYards) uiState.rangeLiveDistanceYards else uiState.rangeLiveDistanceMeters
    val secondary = if (useYards) "${uiState.rangeLiveDistanceMeters}m" else "${uiState.rangeLiveDistanceYards}yd"

    val distances = uiState.rangeBalls.map { it.primaryDistance(settings.distanceUnit) }
    val stats = RangeStats.from(distances)

    if (showEndConfirm) {
        AlertDialog(
            onDismissRequest = { showEndConfirm = false },
            title = { Text("End session?") },
            text = { Text("You've tracked ${distances.size} ${if (distances.size == 1) "ball" else "balls"}. They're already saved to your history.") },
            confirmButton = {
                TextButton(onClick = { showEndConfirm = false; onEndSession() }) {
                    Text("End", color = DarkGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirm = false }) { Text("Keep going") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Pinned at the top so it's always visible — red and compact (smaller than TRACK).
        // Nothing tracked yet → just exit; otherwise confirm so a session isn't lost by mistake.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            EndSessionButton(
                onClick = { if (distances.isEmpty()) onEndSession() else showEndConfirm = true }
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(4.dp))

            // Locked club badge
            ClubBadge(uiState.rangeClub)
            Spacer(Modifier.height(12.dp))

            // Live distance from origin
            Text(
                text = "$liveDistance",
                style = DistanceLive,
                color = TextPrimary
            )
            Text(
                text = if (useYards) "YARDS" else "METERS",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = secondary,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(Modifier.height(16.dp))

            EpicButton(
                text = if (uiState.rangeCapturing) "LOCKING…" else "TRACK",
                enabled = !uiState.rangeCapturing
            ) { onTrackBall() }

            Spacer(Modifier.height(16.dp))

            // Running dispersion stats
            if (stats != null) {
                RangeStatsCard(stats = stats, unitLabel = unitShort)
                Spacer(Modifier.height(12.dp))
            }

            // Tracked balls — newest first, numbered by hit order
            if (uiState.rangeBalls.isNotEmpty()) {
                Text(
                    text = "THIS SESSION",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                uiState.rangeBalls
                    .mapIndexed { index, ball -> (index + 1) to ball }
                    .reversed()
                    .forEach { (number, ball) ->
                        RangeBallRow(
                            number = number,
                            distance = ball.primaryDistance(settings.distanceUnit),
                            unitLabel = unitShort
                        )
                        Spacer(Modifier.height(6.dp))
                    }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EndSessionButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Red40)
            .clickable(onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 12.dp)
    ) {
        Text(
            text = "End Session",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Summary ─────────────────────────────────────────────────────────────────

@Composable
private fun RangeSummaryContent(
    club: Club,
    balls: List<ShotResult>,
    distanceUnit: DistanceUnit,
    onDone: () -> Unit
) {
    val distances = balls.map { it.primaryDistance(distanceUnit) }
    val stats = RangeStats.from(distances)
    val unitShort = if (distanceUnit == DistanceUnit.YARDS) "yd" else "m"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        Text(
            text = "SESSION COMPLETE",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(12.dp))
        ClubBadge(club)
        Spacer(Modifier.height(24.dp))

        if (stats != null) {
            RangeStatsCard(stats = stats, unitLabel = unitShort)
            Spacer(Modifier.height(12.dp))
            // Spread (consistency)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ChipUnselectedBg)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Spread (longest − shortest)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "${stats.spread} $unitShort",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        EpicButton(text = "DONE") { onDone() }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "Saved to your History & Stats.",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary
        )
        Spacer(Modifier.height(24.dp))
    }
}

// ── Shared pieces ────────────────────────────────────────────────────────────

private data class RangeStats(
    val count: Int,
    val avg: Int,
    val longest: Int,
    val shortest: Int
) {
    val spread: Int get() = longest - shortest

    companion object {
        fun from(distances: List<Int>): RangeStats? {
            if (distances.isEmpty()) return null
            return RangeStats(
                count = distances.size,
                avg = distances.average().roundToInt(),
                longest = distances.max(),
                shortest = distances.min()
            )
        }
    }
}

@Composable
private fun RangeStatsCard(stats: RangeStats, unitLabel: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ChipUnselectedBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RangeStatColumn("${stats.count}", "Balls")
            RangeStatColumn("${stats.avg}", "Avg $unitLabel")
            RangeStatColumn("${stats.longest}", "Long", valueColor = DarkGreen)
            RangeStatColumn("${stats.shortest}", "Short")
        }
    }
}

@Composable
private fun RangeStatColumn(value: String, label: String, valueColor: Color = TextPrimary) {
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

@Composable
private fun RangeBallRow(number: Int, distance: Int, unitLabel: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ChipUnselectedBg)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#$number",
                style = MaterialTheme.typography.labelLarge,
                color = TextTertiary,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$distance",
                    style = TextStyle(
                        fontFamily = RobotoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        fontFeatureSettings = "tnum"
                    ),
                    color = TextPrimary
                )
                Text(
                    text = " $unitLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
