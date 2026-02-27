package com.smacktrack.golf.ui.screen

/**
 * Main shot tracking screen — the core user-facing UI.
 *
 * Uses [AnimatedContent] to transition between five phases:
 * 1. **Club Select** — choose a club, see recent shots, tap START
 * 2. **Calibrating Start** — GPS position lock with progress indicator
 * 3. **Walking** — live distance counter, ability to change club mid-walk
 * 4. **Calibrating End** — GPS position lock at landing spot
 * 5. **Result** — distance card with weather, wind arrow, and wind-adjusted carry
 *
 * Also contains reusable wind composables: [WindIndicator], [CompactWindArrow], [WindArrow].
 */

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalContext
import com.smacktrack.golf.domain.Club
import com.smacktrack.golf.location.WindCalculator
import com.smacktrack.golf.ui.AppSettings
import com.smacktrack.golf.ui.DistanceUnit
import com.smacktrack.golf.ui.ShotPhase
import com.smacktrack.golf.ui.ShotResult
import com.smacktrack.golf.ui.ShotTrackerUiState
import com.smacktrack.golf.ui.TemperatureUnit
import com.smacktrack.golf.ui.WindUnit
import com.smacktrack.golf.ui.theme.ChipBorder
import com.smacktrack.golf.ui.theme.ChipUnselectedBg
import com.smacktrack.golf.ui.theme.ChipUnselectedText
import com.smacktrack.golf.ui.theme.DarkGreen
import com.smacktrack.golf.ui.theme.RobotoFamily
import com.smacktrack.golf.ui.theme.TextPrimary
import com.smacktrack.golf.ui.theme.TextSecondary
import com.smacktrack.golf.ui.theme.TextTertiary
import com.smacktrack.golf.ui.theme.clubChipColor
import com.smacktrack.golf.ui.theme.windCategoryColor
import com.smacktrack.golf.ui.share.ShareUtil
import com.smacktrack.golf.ui.share.ShotCardRenderer

// Distance number styles — Roboto with tabular figures so digits don't jump
private val DistanceLive = TextStyle(
    fontFamily = RobotoFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 96.sp,
    lineHeight = 96.sp,
    letterSpacing = (-1).sp,
    fontFeatureSettings = "tnum"
)

private val DistanceResult = TextStyle(
    fontFamily = RobotoFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 80.sp,
    lineHeight = 80.sp,
    letterSpacing = (-1).sp,
    fontFeatureSettings = "tnum"
)

@Composable
fun ShotTrackerScreen(
    uiState: ShotTrackerUiState,
    onClubSelected: (Club) -> Unit,
    onMarkStart: () -> Unit,
    onMarkEnd: () -> Unit,
    onNextShot: () -> Unit,
    onReset: () -> Unit,
    onWindDirectionChange: () -> Unit = {},
    onWindSpeedChange: (Double) -> Unit = {},
    onDeleteShot: (Int) -> Unit = {},
    animateEntrance: Boolean = false,
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings

    AnimatedContent(
        targetState = uiState.phase,
        transitionSpec = {
            (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 8 }) togetherWith
                    (fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 8 })
        },
        modifier = modifier.fillMaxSize(),
        label = "phase"
    ) { phase ->
        when (phase) {
            ShotPhase.CLUB_SELECT -> ClubSelectContent(
                recentShots = uiState.shotHistory.takeLast(3).reversed(),
                shotHistorySize = uiState.shotHistory.size,
                settings = settings,
                onStart = onMarkStart,
                onDeleteShot = onDeleteShot,
                animateEntrance = animateEntrance
            )
            ShotPhase.CALIBRATING_START -> CalibratingContent(label = "Locking position")
            ShotPhase.WALKING -> {
                val club = uiState.selectedClub ?: return@AnimatedContent
                WalkingContent(
                    club = club,
                    enabledClubs = settings.enabledClubs,
                    distanceYards = uiState.liveDistanceYards,
                    distanceMeters = uiState.liveDistanceMeters,
                    settings = settings,
                    onClubSelected = onClubSelected,
                    onEnd = onMarkEnd,
                    onReset = onReset
                )
            }
            ShotPhase.CALIBRATING_END -> CalibratingContent(label = "Locking position")
            ShotPhase.RESULT -> {
                val result = uiState.shotResult ?: return@AnimatedContent
                ResultContent(
                    result = result,
                    shotHistory = uiState.shotHistory,
                    settings = settings,
                    onNextShot = onNextShot,
                    onWindDirectionChange = onWindDirectionChange,
                    onWindSpeedChange = onWindSpeedChange
                )
            }
        }
    }
}

// ── Club Selection ──────────────────────────────────────────────────────────

@Composable
private fun ClubSelectContent(
    recentShots: List<ShotResult>,
    shotHistorySize: Int,
    settings: AppSettings,
    onStart: () -> Unit,
    onDeleteShot: (Int) -> Unit = {},
    animateEntrance: Boolean = false
) {
    var pendingDeleteIndex by remember { mutableStateOf<Int?>(null) }

    // Stagger entrance animation state
    var staggerStarted by remember { mutableStateOf(false) }
    var buttonAlpha by remember { mutableStateOf(if (animateEntrance) 0f else 1f) }
    var buttonScale by remember { mutableStateOf(if (animateEntrance) 0.85f else 1f) }
    var recentLabelAlpha by remember { mutableStateOf(if (animateEntrance) 0f else 1f) }
    var recentLabelOffsetY by remember { mutableStateOf(if (animateEntrance) 12f else 0f) }
    var shotAlphas by remember { mutableStateOf(List(3) { if (animateEntrance) 0f else 1f }) }
    var shotOffsetsY by remember { mutableStateOf(List(3) { if (animateEntrance) 16f else 0f }) }

    LaunchedEffect(animateEntrance) {
        if (!animateEntrance || staggerStarted) return@LaunchedEffect
        staggerStarted = true

        // SMACK button: scale 0.85→1.0 (spring-like) + alpha 0→1 (0ms delay)
        val btnSteps = 14
        for (i in 1..btnSteps) {
            val p = i.toFloat() / btnSteps
            buttonAlpha = p
            buttonScale = 0.85f + 0.15f * p + 0.04f * kotlin.math.sin(p * Math.PI.toFloat())
            delay(20)
        }
        buttonAlpha = 1f
        buttonScale = 1f

        // "RECENT" label (200ms delay from button start, ~80ms after button finishes)
        delay(80)
        val labelSteps = 10
        for (i in 1..labelSteps) {
            val p = i.toFloat() / labelSteps
            recentLabelAlpha = p
            recentLabelOffsetY = 12f * (1f - p)
            delay(15)
        }
        recentLabelAlpha = 1f
        recentLabelOffsetY = 0f

        // Recent shot rows — stagger each by ~100ms
        for (row in 0 until minOf(recentShots.size, 3)) {
            delay(100)
            val rowSteps = 10
            for (i in 1..rowSteps) {
                val p = i.toFloat() / rowSteps
                shotAlphas = shotAlphas.toMutableList().also { it[row] = p }
                shotOffsetsY = shotOffsetsY.toMutableList().also { it[row] = 16f * (1f - p) }
                delay(15)
            }
            shotAlphas = shotAlphas.toMutableList().also { it[row] = 1f }
            shotOffsetsY = shotOffsetsY.toMutableList().also { it[row] = 0f }
        }
    }

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier.graphicsLayer {
                alpha = buttonAlpha
                scaleX = buttonScale
                scaleY = buttonScale
            }
        ) {
            EpicButton(
                text = "SMACK",
                enabled = true,
                pulsate = true
            ) { onStart() }
        }

        Spacer(Modifier.weight(1f))

        // Recent shots
        if (recentShots.isNotEmpty()) {
            Text(
                text = "RECENT",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = recentLabelAlpha
                    translationY = recentLabelOffsetY
                }
            )

            Spacer(Modifier.height(10.dp))

            recentShots.forEachIndexed { displayIndex, shot ->
                val actualIndex = shotHistorySize - 1 - displayIndex
                Box(
                    modifier = Modifier.graphicsLayer {
                        alpha = shotAlphas.getOrElse(displayIndex) { 1f }
                        translationY = shotOffsetsY.getOrElse(displayIndex) { 0f }
                    }
                ) {
                    RecentShotRow(
                        shot = shot,
                        settings = settings,
                        onDelete = { pendingDeleteIndex = actualIndex }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun RecentShotRow(shot: ShotResult, settings: AppSettings, onDelete: (() -> Unit)? = null) {
    val distance = if (settings.distanceUnit == DistanceUnit.YARDS) shot.distanceYards else shot.distanceMeters
    val unitLabel = if (settings.distanceUnit == DistanceUnit.YARDS) "yds" else "m"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ChipUnselectedBg)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Club chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(clubChipColor(shot.club.sortOrder))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = shot.club.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            // Distance + delete
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

@Composable
private fun ClubChip(
    club: Club,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (selected) clubChipColor(club.sortOrder) else ChipUnselectedBg
    val textColor = if (selected) Color.White else ChipUnselectedText
    val borderColor = if (selected) clubChipColor(club.sortOrder) else ChipBorder

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = club.displayName,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ── Epic Button ─────────────────────────────────────────────────────────────

@Composable
private fun EpicButton(
    text: String,
    enabled: Boolean = true,
    pulsate: Boolean = false,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val gradient = Brush.verticalGradient(
        colors = if (enabled) listOf(
            Color(0xFF2E7D32),
            Color(0xFF1B5E20),
            Color(0xFF103614)
        ) else listOf(
            Color(0xFFD5D8D0),
            Color(0xFFC2C5BD)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp),
        contentAlignment = Alignment.Center
    ) {
        if (enabled && pulsate) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(88.dp)
                    .offset(y = 8.dp)
                    .blur(24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF2E7D32).copy(alpha = glowAlpha))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(gradient)
                .clickable(enabled = enabled) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (enabled) Color.White else Color(0xFF9DA09A),
                letterSpacing = 6.sp
            )
        }
    }
}

// ── Calibrating ─────────────────────────────────────────────────────────────

@Composable
private fun CalibratingContent(label: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(56.dp),
            color = DarkGreen,
            strokeWidth = 3.dp,
            trackColor = Color(0xFFE0E2DC)
        )
        Spacer(Modifier.height(28.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Collecting GPS samples",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

// ── Walking (Live Tracking) ─────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WalkingContent(
    club: Club,
    enabledClubs: Set<Club>,
    distanceYards: Int,
    distanceMeters: Int,
    settings: AppSettings,
    onClubSelected: (Club) -> Unit,
    onEnd: () -> Unit,
    onReset: () -> Unit
) {
    val primaryDistance = if (settings.distanceUnit == DistanceUnit.YARDS) distanceYards else distanceMeters
    val primaryUnit = if (settings.distanceUnit == DistanceUnit.YARDS) "YARDS" else "METERS"
    val secondaryDistance = if (settings.distanceUnit == DistanceUnit.YARDS) "${distanceMeters}m" else "${distanceYards}yd"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))

        // Hero distance — Roboto tabular figures
        Text(
            text = "$primaryDistance",
            style = DistanceLive,
            color = TextPrimary
        )
        Text(
            text = primaryUnit,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 4.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = secondaryDistance,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(Modifier.height(20.dp))

        EpicButton(text = "TRACK", onClick = onEnd)

        Spacer(Modifier.height(16.dp))

        // Club selector — change club without losing GPS tracking
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
                        selected = c == club,
                        onClick = { onClubSelected(c) }
                    )
                }
        }

        Spacer(Modifier.height(16.dp))

        // Reset — 48dp minimum touch target
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { onReset() }
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Reset",
                style = MaterialTheme.typography.labelLarge,
                color = TextTertiary
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Result ───────────────────────────────────────────────────────────────────

@Composable
private fun ResultContent(
    result: ShotResult,
    shotHistory: List<ShotResult> = emptyList(),
    settings: AppSettings,
    onNextShot: () -> Unit,
    onWindDirectionChange: () -> Unit = {},
    onWindSpeedChange: (Double) -> Unit = {}
) {
    val primaryDistance = if (settings.distanceUnit == DistanceUnit.YARDS) result.distanceYards else result.distanceMeters
    val primaryUnit = if (settings.distanceUnit == DistanceUnit.YARDS) "YARDS" else "METERS"
    val secondaryDistance = if (settings.distanceUnit == DistanceUnit.YARDS) "${result.distanceMeters}m" else "${result.distanceYards}yd"

    val windSpeed = if (settings.windUnit == WindUnit.KMH) {
        "${result.windSpeedKmh.toInt()} km/h"
    } else {
        "${(result.windSpeedKmh * 0.621371).toInt()} mph"
    }

    val tempDisplay = if (settings.temperatureUnit == TemperatureUnit.FAHRENHEIT) {
        "${result.temperatureF}\u00B0F"
    } else {
        "${result.temperatureC}\u00B0C"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        // Result card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(1.5.dp, Color(0xFFE0E2DC), RoundedCornerShape(24.dp))
                .padding(28.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Club badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(clubChipColor(result.club.sortOrder))
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = result.club.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Distance — Roboto tabular figures, count-up animation
                AnimatedCounter(
                    targetValue = primaryDistance,
                    style = DistanceResult,
                    color = TextPrimary
                )
                Text(
                    text = primaryUnit,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 4.sp
                )
                Text(
                    text = secondaryDistance,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                // Celebration for top shots with a club — animated entrance
                val currentDistance = if (settings.distanceUnit == DistanceUnit.YARDS) result.distanceYards else result.distanceMeters
                val priorShots = shotHistory.filter { it.club == result.club && it.timestampMs != result.timestampMs }
                if (priorShots.size >= 5) {
                    val beatenCount = priorShots.count { shot ->
                        val d = if (settings.distanceUnit == DistanceUnit.YARDS) shot.distanceYards else shot.distanceMeters
                        currentDistance >= d
                    }
                    val percentile = beatenCount.toFloat() / priorShots.size.toFloat() * 100f

                    var badgeVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(700)
                        badgeVisible = true
                    }

                    when {
                        percentile >= 95f -> {
                            Spacer(Modifier.height(16.dp))
                            AnimatedVisibility(
                                visible = badgeVisible,
                                enter = fadeIn(tween(400)) + scaleIn(
                                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFFFFAB00).copy(alpha = 0.12f))
                                        .padding(horizontal = 20.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Absolutely Smacked!",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFAB00)
                                    )
                                }
                            }
                        }
                        percentile >= 80f -> {
                            Spacer(Modifier.height(16.dp))
                            AnimatedVisibility(
                                visible = badgeVisible,
                                enter = fadeIn(tween(400)) + scaleIn(
                                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(DarkGreen.copy(alpha = 0.10f))
                                        .padding(horizontal = 20.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Smacked!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkGreen
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Weather + wind arrow strip
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
                                text = tempDisplay,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = result.weatherDescription,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        // Wind with directional arrow + effect estimate
                        WindIndicator(
                            windSpeedLabel = windSpeed,
                            windCompass = result.windDirectionCompass,
                            windDegrees = result.windDirectionDegrees,
                            shotBearing = result.shotBearingDegrees,
                            windSpeedKmh = result.windSpeedKmh,
                            distanceYards = result.distanceYards,
                            trajectoryMultiplier = settings.trajectory.multiplier,
                            showEffect = true,
                            onDirectionTap = onWindDirectionChange,
                            onSpeedUp = {
                                val delta = if (settings.windUnit == WindUnit.MPH) 1.60934 else 1.0
                                onWindSpeedChange(delta)
                            },
                            onSpeedDown = {
                                val delta = if (settings.windUnit == WindUnit.MPH) -1.60934 else -1.0
                                onWindSpeedChange(delta)
                            }
                        )
                    }
                }

                // Weather-adjusted distance (wind + temperature)
                if (result.windSpeedKmh > 0 || result.temperatureF != 70) {
                    val weatherEffect = WindCalculator.analyze(
                        windSpeedKmh = result.windSpeedKmh,
                        windFromDegrees = result.windDirectionDegrees,
                        shotBearingDegrees = result.shotBearingDegrees,
                        distanceYards = result.distanceYards,
                        trajectoryMultiplier = settings.trajectory.multiplier,
                        temperatureF = result.temperatureF
                    )
                    val adjustedYards = result.distanceYards - weatherEffect.totalWeatherEffectYards
                    val adjustedMeters = (adjustedYards * 0.9144).toInt()
                    val adjustedDisplay = if (settings.distanceUnit == DistanceUnit.YARDS) {
                        "$adjustedYards"
                    } else {
                        "$adjustedMeters"
                    }
                    val unitLabel = if (settings.distanceUnit == DistanceUnit.YARDS) "yds" else "m"
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
            }
        }

        Spacer(Modifier.height(16.dp))

        // Share button
        val context = LocalContext.current
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable {
                    val bitmap = ShotCardRenderer.render(
                        context = context,
                        result = result,
                        settings = settings,
                        shotHistory = shotHistory
                    )
                    ShareUtil.shareShotCard(context, bitmap)
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

        Spacer(Modifier.height(16.dp))

        EpicButton(text = "NEXT SHOT", onClick = onNextShot)

        Spacer(Modifier.height(24.dp))
    }
}

// ── Wind Indicator ──────────────────────────────────────────────────────────

/**
 * Wind indicator: colored arrow + carry/lateral effect numbers.
 */
/**
 * Wind strength label based on speed (km/h).
 */
private fun windStrengthLabel(windSpeedKmh: Double): String = when {
    windSpeedKmh < 6   -> "None"
    windSpeedKmh < 13  -> "Very Light"
    windSpeedKmh < 20  -> "Light"
    windSpeedKmh < 36  -> "Medium"
    windSpeedKmh < 50  -> "Strong"
    windSpeedKmh < 71  -> "Very Strong"
    else               -> "Why are you even out here?!"
}

@Composable
fun WindIndicator(
    windSpeedLabel: String,
    windCompass: String,
    windDegrees: Int,
    shotBearing: Double,
    windSpeedKmh: Double = 0.0,
    distanceYards: Int = 0,
    trajectoryMultiplier: Double = 1.0,
    showEffect: Boolean = false,
    onDirectionTap: (() -> Unit)? = null,
    onSpeedUp: (() -> Unit)? = null,
    onSpeedDown: (() -> Unit)? = null
) {
    val effect = WindCalculator.analyze(
        windSpeedKmh = windSpeedKmh,
        windFromDegrees = windDegrees,
        shotBearingDegrees = shotBearing,
        distanceYards = distanceYards,
        trajectoryMultiplier = trajectoryMultiplier
    )
    val color = windCategoryColor(effect.colorCategory)
    val hasControls = onSpeedUp != null || onSpeedDown != null

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(horizontalAlignment = Alignment.End) {
            // Speed row: optional down arrow | speed | optional up arrow
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasControls && onSpeedDown != null) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Decrease wind speed",
                        tint = TextTertiary,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onSpeedDown() }
                    )
                }
                Text(
                    text = windSpeedLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (hasControls && onSpeedUp != null) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Increase wind speed",
                        tint = TextTertiary,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onSpeedUp() }
                    )
                }
            }
            // Wind strength description
            Text(
                text = windStrengthLabel(windSpeedKmh),
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
            if (showEffect && windSpeedKmh > 0 && distanceYards > 0) {
                val carryText = if (effect.carryEffectYards >= 0) {
                    "+${effect.carryEffectYards} yds"
                } else {
                    "${effect.carryEffectYards} yds"
                }
                Text(
                    text = carryText,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
                val lateralYds = kotlin.math.abs(effect.lateralDisplacementYards)
                if (lateralYds >= 0.5) {
                    val dir = if (effect.lateralDisplacementYards > 0) "\u2192" else "\u2190"
                    Text(
                        text = "$dir ${lateralYds.toInt()} yds",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Wind arrow + optional direction cycle button
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            WindArrow(
                rotationDegrees = effect.relativeAngleDeg,
                color = color,
                modifier = Modifier.size(32.dp)
            )
            if (onDirectionTap != null) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Cycle wind direction",
                    tint = TextTertiary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onDirectionTap() }
                )
            }
        }
    }
}

/**
 * Compact wind arrow for history cards and recent shots.
 */
@Composable
fun CompactWindArrow(
    windDegrees: Int,
    shotBearing: Double,
    modifier: Modifier = Modifier
) {
    val relAngle = WindCalculator.relativeWindAngle(windDegrees, shotBearing)
    val colorCat = WindCalculator.windColorCategory(relAngle)
    val color = windCategoryColor(colorCat)
    WindArrow(
        rotationDegrees = relAngle,
        color = color,
        modifier = modifier.size(16.dp)
    )
}

@Composable
private fun WindArrow(
    rotationDegrees: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.rotate(rotationDegrees)
    ) {
        val w = size.width
        val h = size.height
        val arrowPath = Path().apply {
            moveTo(w * 0.5f, h * 0.05f)
            lineTo(w * 0.2f, h * 0.45f)
            lineTo(w * 0.38f, h * 0.45f)
            lineTo(w * 0.38f, h * 0.95f)
            lineTo(w * 0.62f, h * 0.95f)
            lineTo(w * 0.62f, h * 0.45f)
            lineTo(w * 0.8f, h * 0.45f)
            close()
        }
        drawPath(arrowPath, color)
    }
}
