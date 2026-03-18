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
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import com.smacktrack.golf.domain.Club
import com.smacktrack.golf.domain.UnlockedAchievement
import com.smacktrack.golf.location.WindCalculator
import com.smacktrack.golf.ui.AppSettings
import com.smacktrack.golf.ui.DistanceUnit
import com.smacktrack.golf.ui.ShotPhase
import com.smacktrack.golf.ui.ShotResult
import com.smacktrack.golf.ui.ShotTrackerUiState
import com.smacktrack.golf.ui.WindUnit
import com.smacktrack.golf.ui.percentileAmongClub
import com.smacktrack.golf.ui.primaryDistance
import com.smacktrack.golf.ui.primaryUnitLabel
import com.smacktrack.golf.ui.secondaryDistance
import com.smacktrack.golf.ui.shortUnitLabel
import com.smacktrack.golf.ui.windStrengthLabel
import com.smacktrack.golf.ui.theme.ChipBorder
import com.smacktrack.golf.ui.theme.ChipUnselectedBg
import com.smacktrack.golf.ui.theme.ChipUnselectedText
import com.smacktrack.golf.ui.theme.DarkGreen
import com.smacktrack.golf.ui.theme.LightGray
import com.smacktrack.golf.ui.theme.MidGray
import com.smacktrack.golf.ui.theme.Red40
import com.smacktrack.golf.ui.theme.RobotoFamily
import com.smacktrack.golf.ui.theme.TextPrimary
import com.smacktrack.golf.ui.theme.TextSecondary
import com.smacktrack.golf.ui.theme.TextTertiary
import com.smacktrack.golf.ui.theme.clubChipColor
import com.smacktrack.golf.ui.theme.windCategoryColor

// Distance number styles — Roboto with tabular figures so digits don't jump
private val DistanceLive = TextStyle(
    fontFamily = RobotoFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 96.sp,
    lineHeight = 96.sp,
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
    onClubChanged: (Club) -> Unit = {},
    onDeleteShot: (Long) -> Unit = {},
    onShotClicked: (ShotResult) -> Unit = {},
    animateEntrance: Boolean = false,
    newlyUnlockedAchievements: List<UnlockedAchievement> = emptyList(),
    onAchievementsSeen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings

    // Keep screen on during active tracking phases
    val keepScreenOn = uiState.phase in setOf(
        ShotPhase.CALIBRATING_START, ShotPhase.WALKING, ShotPhase.CALIBRATING_END
    )
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
        targetState = uiState.phase,
        transitionSpec = {
            val isForward = targetState.ordinal > initialState.ordinal
            if (isForward) {
                // Forward: new content scales up from 95% + fades in, old shrinks + fades out
                (fadeIn(tween(400)) + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(400)
                ) + slideInVertically(tween(400)) { it / 12 }) togetherWith
                    (fadeOut(tween(250)) + slideOutVertically(tween(250)) { -it / 10 })
            } else {
                // Backward (reset/cancel): reverse direction
                (fadeIn(tween(300)) + slideInVertically(tween(300)) { -it / 10 }) togetherWith
                    (fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 8 })
            }
        },
        modifier = modifier.fillMaxSize(),
        label = "phase"
    ) { phase ->
        when (phase) {
            ShotPhase.CLUB_SELECT -> ClubSelectContent(
                recentShots = uiState.shotHistory.takeLast(3).reversed(),
                shotHistory = uiState.shotHistory,
                settings = settings,
                onStart = onMarkStart,
                onDeleteShot = onDeleteShot,
                onShotClicked = onShotClicked,
                animateEntrance = animateEntrance
            )
            ShotPhase.CALIBRATING_START -> CalibratingContent(
                label = "Marking start position",
                realAccuracyMeters = uiState.calibrationAccuracyMeters,
                realProgress = uiState.calibrationProgress,
                onCancel = onReset
            )
            ShotPhase.WALKING -> {
                val club = uiState.selectedClub ?: return@AnimatedContent
                WalkingContent(
                    club = club,
                    enabledClubs = settings.enabledClubs,
                    distanceYards = uiState.liveDistanceYards,
                    distanceMeters = uiState.liveDistanceMeters,
                    settings = settings,
                    shotHistory = uiState.shotHistory,
                    onClubSelected = onClubSelected,
                    onEnd = onMarkEnd,
                    onReset = onReset
                )
            }
            ShotPhase.CALIBRATING_END -> CalibratingContent(
                label = "Marking end position",
                realAccuracyMeters = null,
                realProgress = null,
                onCancel = onReset
            )
            ShotPhase.RESULT -> {
                val result = uiState.shotResult ?: return@AnimatedContent
                ResultContent(
                    result = result,
                    shotHistory = uiState.shotHistory,
                    settings = settings,
                    gpsAccuracyMeters = uiState.gpsAccuracyMeters,
                    onNextShot = onNextShot,
                    onWindDirectionChange = onWindDirectionChange,
                    onWindSpeedChange = onWindSpeedChange,
                    onClubChanged = onClubChanged,
                    newlyUnlockedAchievements = newlyUnlockedAchievements,
                    onAchievementsSeen = onAchievementsSeen
                )
            }
        }
    }
}

// ── Club Selection ──────────────────────────────────────────────────────────

@Composable
private fun ClubSelectContent(
    recentShots: List<ShotResult>,
    shotHistory: List<ShotResult>,
    settings: AppSettings,
    onStart: () -> Unit,
    onDeleteShot: (Long) -> Unit = {},
    onShotClicked: (ShotResult) -> Unit = {},
    animateEntrance: Boolean = false
) {
    var pendingDeleteTimestamp by remember { mutableStateOf<Long?>(null) }

    // Single smooth entrance: animated progress drives both alpha and scale
    val entranceProgress = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(animateEntrance) {
        if (animateEntrance) {
            entranceProgress.animateTo(1f, tween(500, easing = androidx.compose.animation.core.EaseOut))
        }
    }

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
    // Session summary for current active session
    val sessionSummary = remember(shotHistory, settings.distanceUnit) {
        val activeSession = currentActiveSession(shotHistory)
        if (activeSession != null) computeSessionSummary(activeSession.shots, settings.distanceUnit) else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = entranceProgress.value
                alpha = p
                scaleX = 0.96f + 0.04f * p
                scaleY = 0.96f + 0.04f * p
            }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        EpicButton(
            text = "SMACK",
            enabled = true,
            pulsate = true
        ) { onStart() }

        Spacer(Modifier.weight(1f))

        // Session summary card
        if (sessionSummary != null) {
            val unitLabel = if (settings.distanceUnit == DistanceUnit.YARDS) "yds" else "m"
            SessionSummaryCard(summary = sessionSummary, unitLabel = unitLabel)
            Spacer(Modifier.height(16.dp))
        }

        // Recent shots
        if (recentShots.isNotEmpty()) {
            Text(
                text = "RECENT",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )

            Spacer(Modifier.height(10.dp))

            recentShots.forEach { shot ->
                RecentShotRow(
                    shot = shot,
                    settings = settings,
                    onDelete = { pendingDeleteTimestamp = shot.timestampMs },
                    onClick = { onShotClicked(shot) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun RecentShotRow(shot: ShotResult, settings: AppSettings, onDelete: (() -> Unit)? = null, onClick: () -> Unit = {}) {
    val distance = shot.primaryDistance(settings.distanceUnit)
    val unitLabel = shot.shortUnitLabel(settings.distanceUnit)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ChipUnselectedBg)
            .clickable(onClick = onClick)
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
                    .clip(RoundedCornerShape(4.dp))
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
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(4.dp))
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
    var lastClickTime by remember { mutableLongStateOf(0L) }
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
                .clickable(enabled = enabled) {
                    val now = System.currentTimeMillis()
                    if (now - lastClickTime > 500) {
                        lastClickTime = now
                        onClick()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (enabled) Color.White else TextTertiary,
                letterSpacing = 6.sp
            )
        }
    }
}

// ── Calibrating — GPS Lock-On Experience ────────────────────────────────────

private data class SignalParticle(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var alpha: Float, var radius: Float,
    var life: Float, val decay: Float
)

@Composable
private fun CalibratingContent(
    label: String,
    realAccuracyMeters: Double?,
    realProgress: Float?,
    onCancel: () -> Unit
) {
    val isStart = "start" in label.lowercase()
    val useRealData = isStart && realProgress != null
    val durationMs = if (isStart) 7000f else 2000f  // Max duration for time-based fallback

    var frame by remember { mutableStateOf(0) }
    var elapsedMs by remember { mutableStateOf(0f) }
    var statusText by remember { mutableStateOf("Acquiring signal") }
    val particles = remember { mutableListOf<SignalParticle>() }
    // Burst particles — emitted at lock-on
    val burstParticles = remember { mutableListOf<SignalParticle>() }
    var burstTriggered by remember { mutableStateOf(false) }

    // Animate frame counter
    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            frame++
            elapsedMs += 16f
            if (!useRealData) {
                // End calibration: time-based progress (unchanged)
                val progress = (elapsedMs / durationMs).coerceIn(0f, 1f)
                statusText = when {
                    progress < 0.3f -> "Acquiring signal"
                    progress < 0.65f -> "Locking position"
                    progress < 0.9f -> "Refining accuracy"
                    progress < 1f -> "Almost there"
                    else -> "Locked"
                }
                if (progress >= 1f && !burstTriggered) {
                    burstTriggered = true
                    for (i in 0..35) {
                        val a = i * (6.2832f / 36f) + kotlin.random.Random.nextFloat() * 0.15f
                        val spd = 4f + kotlin.random.Random.nextFloat() * 8f
                        burstParticles.add(SignalParticle(
                            x = 0f, y = 0f,
                            vx = kotlin.math.cos(a) * spd,
                            vy = kotlin.math.sin(a) * spd,
                            alpha = 1f, radius = 2f + kotlin.random.Random.nextFloat() * 4f,
                            life = 1f, decay = 0.018f + kotlin.random.Random.nextFloat() * 0.012f
                        ))
                    }
                }
            }
        }
    }

    // Start calibration: update status from real GPS data
    if (useRealData) {
        val rp = realProgress ?: 0f
        val acc = realAccuracyMeters
        statusText = when {
            rp >= 1f -> "Locked"
            acc == null || acc > 15.0 -> "Acquiring signal"
            acc > 10.0 -> "Locking position"
            acc > 6.0 -> "Refining accuracy"
            acc > 4.0 -> "Almost there"
            else -> "Locked"
        }
        if (rp >= 1f && !burstTriggered) {
            burstTriggered = true
            for (i in 0..35) {
                val a = i * (6.2832f / 36f) + kotlin.random.Random.nextFloat() * 0.15f
                val spd = 4f + kotlin.random.Random.nextFloat() * 8f
                burstParticles.add(SignalParticle(
                    x = 0f, y = 0f,
                    vx = kotlin.math.cos(a) * spd,
                    vy = kotlin.math.sin(a) * spd,
                    alpha = 1f, radius = 2f + kotlin.random.Random.nextFloat() * 4f,
                    life = 1f, decay = 0.018f + kotlin.random.Random.nextFloat() * 0.012f
                ))
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h * 0.38f
            val time = frame * 0.016f
            val progress = if (useRealData) (realProgress ?: 0f) else (elapsedMs / durationMs).coerceIn(0f, 1f)

            // Background radial glow — intensifies with progress
            val glowAlpha = 0.14f + progress * 0.10f
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1B5E20).copy(alpha = glowAlpha),
                        Color(0xFF1B5E20).copy(alpha = glowAlpha * 0.4f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = w * 0.9f
                )
            )

            // ── Radar sweep beam — rotates continuously ──
            val sweepAngle = time * 1.8f // ~100° per second
            val sweepLen = w * 0.48f
            val sweepX = cx + kotlin.math.cos(sweepAngle) * sweepLen
            val sweepY = cy + kotlin.math.sin(sweepAngle) * sweepLen * 0.7f
            // Fade trail (wider, translucent)
            for (trail in 1..8) {
                val trailAngle = sweepAngle - trail * 0.06f
                val trailX = cx + kotlin.math.cos(trailAngle) * sweepLen
                val trailY = cy + kotlin.math.sin(trailAngle) * sweepLen * 0.7f
                val trailAlpha = (0.08f - trail * 0.009f).coerceAtLeast(0f) * (0.6f + progress * 0.4f)
                drawLine(
                    Color(0xFF4CAF50).copy(alpha = trailAlpha),
                    Offset(cx, cy),
                    Offset(trailX.toFloat(), trailY.toFloat()),
                    strokeWidth = 3f
                )
            }
            // Main sweep line
            drawLine(
                Color(0xFF66BB6A).copy(alpha = 0.18f + progress * 0.12f),
                Offset(cx, cy),
                Offset(sweepX.toFloat(), sweepY.toFloat()),
                strokeWidth = 2f
            )

            // ── Radar rings expanding outward ──
            for (ring in 0..4) {
                val ringPhase = ((time * 0.5f + ring * 0.2f) % 1f)
                val ringRadius = ringPhase * w * 0.55f
                val ringAlpha = (1f - ringPhase) * 0.25f * (0.5f + progress * 0.5f)
                if (ringAlpha > 0.01f) {
                    drawCircle(
                        color = Color(0xFF2E7D32).copy(alpha = ringAlpha * 0.3f),
                        radius = ringRadius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 6f)
                    )
                    drawCircle(
                        color = Color(0xFF4CAF50).copy(alpha = ringAlpha),
                        radius = ringRadius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2f)
                    )
                }
            }

            // ── Grid crosshair lines ──
            val crossAlpha = 0.10f + progress * 0.10f
            val crossLen = w * 0.35f
            drawLine(Color(0xFF2E7D32).copy(alpha = crossAlpha),
                Offset(cx - crossLen, cy), Offset(cx + crossLen, cy), strokeWidth = 1.5f)
            drawLine(Color(0xFF2E7D32).copy(alpha = crossAlpha),
                Offset(cx, cy - crossLen), Offset(cx, cy + crossLen), strokeWidth = 1.5f)
            // Tick marks
            for (tick in 1..3) {
                val tickDist = crossLen * tick / 3f
                val tickSize = 6f
                val tickAlpha = crossAlpha * 0.7f
                drawLine(Color(0xFF2E7D32).copy(alpha = tickAlpha),
                    Offset(cx - tickDist, cy - tickSize), Offset(cx - tickDist, cy + tickSize), 1f)
                drawLine(Color(0xFF2E7D32).copy(alpha = tickAlpha),
                    Offset(cx + tickDist, cy - tickSize), Offset(cx + tickDist, cy + tickSize), 1f)
                drawLine(Color(0xFF2E7D32).copy(alpha = tickAlpha),
                    Offset(cx - tickSize, cy - tickDist), Offset(cx + tickSize, cy - tickDist), 1f)
                drawLine(Color(0xFF2E7D32).copy(alpha = tickAlpha),
                    Offset(cx - tickSize, cy + tickDist), Offset(cx + tickSize, cy + tickDist), 1f)
            }

            // ── Orbiting satellite dots — converge toward center as progress increases ──
            for (sat in 0..5) {
                val baseOrbit = w * (0.14f + sat * 0.04f)
                val orbitRadius = baseOrbit * (1f - progress * 0.4f) // shrink orbits as lock-on
                val speed = 0.7f + sat * 0.25f
                val angle = time * speed + sat * 1.047f
                val sx = cx + kotlin.math.cos(angle) * orbitRadius
                val sy = cy + kotlin.math.sin(angle) * orbitRadius * 0.7f
                val satAlpha = 0.5f + progress * 0.4f
                // Connection line to center
                drawLine(
                    Color(0xFF4CAF50).copy(alpha = satAlpha * 0.18f),
                    Offset(cx, cy), Offset(sx.toFloat(), sy.toFloat()), strokeWidth = 1f
                )
                // Glow halo
                drawCircle(
                    color = Color(0xFF81C784).copy(alpha = satAlpha * 0.2f),
                    radius = 14f, center = Offset(sx.toFloat(), sy.toFloat())
                )
                drawCircle(
                    color = Color(0xFF81C784).copy(alpha = satAlpha * 0.5f),
                    radius = 8f, center = Offset(sx.toFloat(), sy.toFloat())
                )
                // Core
                drawCircle(
                    color = Color(0xFF4CAF50).copy(alpha = satAlpha),
                    radius = 4f, center = Offset(sx.toFloat(), sy.toFloat())
                )
            }

            // ── Converging signal particles ──
            if (frame % 2 == 0 && progress < 0.95f) {
                val pAngle = kotlin.random.Random.nextFloat() * 6.2832f
                val dist = w * (0.3f + kotlin.random.Random.nextFloat() * 0.25f)
                particles.add(SignalParticle(
                    x = cx + kotlin.math.cos(pAngle) * dist,
                    y = cy + kotlin.math.sin(pAngle) * dist * 0.7f,
                    vx = 0f, vy = 0f,
                    alpha = 0.9f, radius = 2.5f + kotlin.random.Random.nextFloat() * 3f,
                    life = 1f, decay = 0.010f + kotlin.random.Random.nextFloat() * 0.008f
                ))
            }
            val pIter = particles.iterator()
            while (pIter.hasNext()) {
                val p = pIter.next()
                val dx = cx - p.x; val dy = cy - p.y
                val dist = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                p.vx += dx / dist * 1.5f
                p.vy += dy / dist * 1.5f
                p.vx *= 0.95f; p.vy *= 0.95f
                p.x += p.vx; p.y += p.vy
                p.life -= p.decay
                p.alpha = (p.life * 0.9f).coerceIn(0f, 0.9f)
                if (p.life <= 0 || dist < 10f) {
                    pIter.remove()
                } else {
                    drawCircle(Color(0xFF81C784).copy(alpha = p.alpha * 0.35f),
                        p.radius + 5f, Offset(p.x, p.y))
                    drawCircle(Color(0xFFA5D6A7).copy(alpha = p.alpha),
                        p.radius, Offset(p.x, p.y))
                    drawCircle(Color.White.copy(alpha = p.alpha * 0.4f),
                        p.radius * 0.4f, Offset(p.x, p.y))
                }
            }

            // ── Lock-on burst particles — explode outward at completion ──
            if (burstTriggered) {
                val bIter = burstParticles.iterator()
                while (bIter.hasNext()) {
                    val bp = bIter.next()
                    bp.x += bp.vx; bp.y += bp.vy
                    bp.vx *= 0.96f; bp.vy *= 0.96f
                    bp.life -= bp.decay
                    bp.alpha = bp.life.coerceIn(0f, 1f)
                    if (bp.life <= 0) {
                        bIter.remove()
                    } else {
                        val bx = cx + bp.x
                        val by = cy + bp.y
                        drawCircle(Color(0xFF66BB6A).copy(alpha = bp.alpha * 0.5f),
                            bp.radius + 6f, Offset(bx, by))
                        drawCircle(Color(0xFFA5D6A7).copy(alpha = bp.alpha),
                            bp.radius, Offset(bx, by))
                        drawCircle(Color.White.copy(alpha = bp.alpha * 0.7f),
                            bp.radius * 0.5f, Offset(bx, by))
                    }
                }

                // Lock-on flash — bright ring that expands and fades
                val flashAge = (elapsedMs - durationMs).coerceAtLeast(0f)
                val flashProgress = (flashAge / 500f).coerceIn(0f, 1f) // 500ms flash
                val flashRadius = 60f + flashProgress * w * 0.35f
                val flashAlpha = (1f - flashProgress) * 0.5f
                if (flashAlpha > 0.01f) {
                    drawCircle(
                        color = Color(0xFF4CAF50).copy(alpha = flashAlpha),
                        radius = flashRadius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 4f * (1f - flashProgress) + 1f)
                    )
                    // Inner glow fill
                    drawCircle(
                        color = Color(0xFF81C784).copy(alpha = flashAlpha * 0.3f),
                        radius = flashRadius * 0.6f,
                        center = Offset(cx, cy)
                    )
                }
            }

            // ── Center target — progress arc ──
            val targetRadius = 48f
            // Outer subtle ring
            drawCircle(
                color = Color(0xFF2E7D32).copy(alpha = 0.08f),
                radius = targetRadius + 12f,
                center = Offset(cx, cy),
                style = Stroke(width = 1f)
            )
            // Track ring
            drawCircle(
                color = MidGray,
                radius = targetRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 4f)
            )
            // Progress arc
            if (progress > 0f) {
                // Glow behind arc
                drawArc(
                    color = Color(0xFF4CAF50).copy(alpha = 0.25f),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(cx - targetRadius, cy - targetRadius),
                    size = androidx.compose.ui.geometry.Size(targetRadius * 2, targetRadius * 2),
                    style = Stroke(width = 8f, cap = StrokeCap.Round)
                )
                // Main arc
                drawArc(
                    color = Color(0xFF2E7D32),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(cx - targetRadius, cy - targetRadius),
                    size = androidx.compose.ui.geometry.Size(targetRadius * 2, targetRadius * 2),
                    style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                )
            }

            // Center dot — pulses faster as lock-on approaches, freezes at completion
            val pulseSpeed = 3f + progress * 5f
            val pulseAmt = if (burstTriggered) 0f else 0.25f * (1f - progress)
            val pulseScale = 1f + kotlin.math.sin(time * pulseSpeed) * pulseAmt
            val dotRadius = (if (burstTriggered) 10f else 8f) * pulseScale
            // Wide outer glow
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        Color(0xFF4CAF50).copy(alpha = 0.25f + progress * 0.2f),
                        Color(0xFF4CAF50).copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = dotRadius * 6f
                ),
                radius = dotRadius * 6f, center = Offset(cx, cy)
            )
            // Core dot — turns bright green on lock
            val coreColor = if (burstTriggered) Color(0xFF4CAF50) else Color(0xFF2E7D32)
            drawCircle(color = coreColor, radius = dotRadius, center = Offset(cx, cy))
            drawCircle(color = Color.White, radius = dotRadius * 0.45f, center = Offset(cx, cy))

            // ── Signal strength bars ──
            val barCount = 5
            val barWidth = 6f
            val barSpacing = 5f
            val barsWidth = barCount * barWidth + (barCount - 1) * barSpacing
            val barStartX = cx - barsWidth / 2f
            val barBaseY = cy + targetRadius + 28f
            for (i in 0 until barCount) {
                val barThreshold = (i + 1).toFloat() / barCount
                val barActive = progress >= barThreshold * 0.85f
                val barHeight = 8f + i * 6f
                val barColor = if (barActive) Color(0xFF4CAF50) else Color(0xFFD5D8D0)
                val bx = barStartX + i * (barWidth + barSpacing)
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(bx, barBaseY - barHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f)
                )
            }
        }

        // Text overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.65f))

            val dotCount = (frame / 20) % 4
            val dots = if (burstTriggered) "" else ".".repeat(dotCount)

            Text(
                text = "$statusText$dots",
                style = MaterialTheme.typography.titleLarge,
                color = if (burstTriggered) DarkGreen else TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))

            // Accuracy readout — real data for start, simulated for end
            val accuracy = if (useRealData) {
                val acc = realAccuracyMeters
                when {
                    burstTriggered && acc != null -> "\u00B1${acc.toInt()}m"
                    acc == null -> "\u00B1--m"
                    else -> "\u00B1${acc.toInt()}m"
                }
            } else {
                when {
                    burstTriggered -> "\u00B13m"
                    elapsedMs < durationMs * 0.3f -> "\u00B115m"
                    elapsedMs < durationMs * 0.5f -> "\u00B110m"
                    elapsedMs < durationMs * 0.7f -> "\u00B17m"
                    elapsedMs < durationMs * 0.85f -> "\u00B15m"
                    else -> "\u00B14m"
                }
            }
            Text(
                text = if (isStart) "Hold still at your ball" else "Hold still at landing spot",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Accuracy: $accuracy",
                style = MaterialTheme.typography.labelSmall,
                color = if (burstTriggered) DarkGreen else TextTertiary,
                fontWeight = if (burstTriggered) FontWeight.Bold else FontWeight.Normal
            )

            Spacer(Modifier.weight(0.35f))

            // Cancel button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onCancel() }
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextTertiary
                )
            }
            Spacer(Modifier.height(32.dp))
        }
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
    shotHistory: List<ShotResult> = emptyList(),
    onClubSelected: (Club) -> Unit,
    onEnd: () -> Unit,
    onReset: () -> Unit
) {
    var showResetConfirm by remember { mutableStateOf(false) }
    val useYards = settings.distanceUnit == DistanceUnit.YARDS
    val primaryDistance = if (useYards) distanceYards else distanceMeters
    val primaryUnit = if (useYards) "YARDS" else "METERS"
    val secondaryDistance = if (useYards) "${distanceMeters}m" else "${distanceYards}yd"

    // Compute average distance for the selected club
    val clubAvgDistance = remember(club, shotHistory, useYards) {
        val clubShots = shotHistory.filter { it.club == club }
        if (clubShots.isEmpty()) 0
        else {
            val distances = clubShots.map { if (useYards) it.distanceYards else it.distanceMeters }
            distances.average().toInt()
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Shot?") },
            text = { Text("Your current tracking progress will be lost.") },
            confirmButton = {
                TextButton(onClick = { showResetConfirm = false; onReset() }) {
                    Text("Reset", color = Red40)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
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

        Spacer(Modifier.height(16.dp))

        // Animated walking strip — distance-scaled with flag at club average
        WalkingStrip(
            primaryDistance = primaryDistance,
            clubAvgDistance = clubAvgDistance,
            unitLabel = if (useYards) "yd" else "m"
        )
        if (clubAvgDistance > 0) {
            Text(
                text = "avg ${clubAvgDistance}${if (useYards) "yd" else "m"}",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

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

        // Reset — 48dp minimum touch target, with confirmation
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { showResetConfirm = true }
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

// ── Walking animation strip ─────────────────────────────────────────────────

@Composable
private fun WalkingStrip(
    primaryDistance: Int,
    clubAvgDistance: Int,
    unitLabel: String
) {
    var frame by remember { mutableStateOf(0) }
    var prevDist by remember { mutableStateOf(primaryDistance) }
    var speed by remember { mutableStateOf(0f) }

    // Smooth the walking man position
    val manProgress = remember { Animatable(0f) }

    // Scale: flag at clubAvg, right edge at clubAvg * 1.2 (or fallback 250 if no history)
    val scaleMax = if (clubAvgDistance > 0) (clubAvgDistance * 1.2f).coerceAtLeast(20f) else 250f
    val flagFrac = if (clubAvgDistance > 0) clubAvgDistance / scaleMax else 0f
    val targetManFrac = (primaryDistance / scaleMax).coerceIn(0f, 1f)

    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            frame++
            speed = (speed * 0.97f).coerceAtLeast(if (primaryDistance > 0) 0.3f else 0.1f)
        }
    }

    LaunchedEffect(primaryDistance) {
        if (primaryDistance != prevDist) {
            speed = (speed + kotlin.math.abs(primaryDistance - prevDist) * 0.4f).coerceAtMost(3f)
            prevDist = primaryDistance
        }
    }

    // Animate man position smoothly
    LaunchedEffect(targetManFrac) {
        manProgress.animateTo(targetManFrac, tween(600))
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(LightGray)
    ) {
        val w = size.width
        val h = size.height
        val time = frame * 0.016f
        val cy = h / 2f

        val padLeft = 40f
        val padRight = 24f
        val trackLen = w - padLeft - padRight

        // ── Scrolling fairway dashes ──
        val dashSpacing = 48f
        val dashCount = (w / dashSpacing).toInt() + 2
        val scrollOffset = (time * 40f * speed) % dashSpacing
        val manX = padLeft + trackLen * manProgress.value

        for (i in 0 until dashCount) {
            val dx = i * dashSpacing + scrollOffset - dashSpacing
            val dashW = 18f + speed * 4f
            val dashH = 4f
            val fadeDist = kotlin.math.abs(dx - manX) / (w / 2f)
            val dashAlpha = (1f - fadeDist * fadeDist) * (0.18f + speed * 0.1f)
            if (dashAlpha > 0.01f) {
                drawRoundRect(
                    color = Color(0xFF4CAF50).copy(alpha = dashAlpha.coerceAtMost(0.4f)),
                    topLeft = Offset(dx - dashW / 2f, cy - dashH / 2f - 7f),
                    size = androidx.compose.ui.geometry.Size(dashW, dashH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f)
                )
                drawRoundRect(
                    color = Color(0xFF4CAF50).copy(alpha = dashAlpha.coerceAtMost(0.4f)),
                    topLeft = Offset(dx - dashW / 2f + dashSpacing * 0.4f, cy - dashH / 2f + 7f),
                    size = androidx.compose.ui.geometry.Size(dashW * 0.8f, dashH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f)
                )
            }
        }

        // ── Flag at club average distance ──
        val flagX = if (flagFrac > 0f) padLeft + trackLen * flagFrac else w - 50f
        val flagPoleBot = cy + 16f
        val flagPoleTop = cy - 20f
        drawLine(Color(0xFF1B5E20), Offset(flagX, flagPoleBot), Offset(flagX, flagPoleTop), 2f)
        val wave = kotlin.math.sin(time * 4f) * 3f
        val flagPath = Path().apply {
            moveTo(flagX, flagPoleTop)
            lineTo(flagX - 16f + wave, flagPoleTop + 7f)
            lineTo(flagX, flagPoleTop + 14f)
            close()
        }
        drawPath(flagPath, Color(0xFFE53935).copy(alpha = 0.85f))

        // Small tick mark under flag showing average
        if (clubAvgDistance > 0) {
            drawLine(Color(0xFF757872).copy(alpha = 0.5f),
                Offset(flagX, flagPoleBot + 2f), Offset(flagX, flagPoleBot + 6f), 1f)
        }

        // ── Start tee marker at left ──
        val teeX = padLeft
        drawLine(Color(0xFF1B5E20).copy(alpha = 0.4f),
            Offset(teeX, cy + 16f), Offset(teeX, cy - 6f), 2f)
        drawCircle(Color(0xFFFFFFFF), 5f, Offset(teeX, cy - 9f))
        drawCircle(MidGray, 5f, Offset(teeX, cy - 9f), style = Stroke(1.5f))

        // ── Progress track line ──
        val trackY = cy + 22f
        val trackEnd = padLeft + trackLen
        drawLine(MidGray, Offset(teeX, trackY), Offset(trackEnd, trackY), 3f, cap = StrokeCap.Round)

        // Filled progress (green line up to man's position)
        if (manProgress.value > 0f) {
            val progEnd = padLeft + trackLen * manProgress.value
            drawLine(Color(0xFF4CAF50), Offset(teeX, trackY), Offset(progEnd, trackY), 3.5f, cap = StrokeCap.Round)
            drawCircle(Color(0xFF2E7D32), 5.5f, Offset(progEnd, trackY))
        }

        // ── Walking figure — animated stick figure at man's position ──
        val figX = manX
        val figY = cy
        val isMoving = speed > 0.4f
        val stride = if (isMoving) kotlin.math.sin(time * (3f + speed * 2f)) else 0f
        val bounce = if (isMoving) kotlin.math.abs(kotlin.math.sin(time * (6f + speed * 4f))) * 3f else 0f

        val bodyTop = figY - 16f - bounce
        val bodyBot = figY + 6f - bounce
        drawLine(Color(0xFF2E7D32), Offset(figX, bodyTop), Offset(figX, bodyBot), 3.5f, cap = StrokeCap.Round)

        drawCircle(Color(0xFF2E7D32), 6.5f, Offset(figX, bodyTop - 6.5f))

        val legLen = 12f
        val legSpread = stride * 8f
        drawLine(Color(0xFF2E7D32),
            Offset(figX, bodyBot), Offset(figX + legSpread, bodyBot + legLen),
            3f, cap = StrokeCap.Round)
        drawLine(Color(0xFF2E7D32),
            Offset(figX, bodyBot), Offset(figX - legSpread, bodyBot + legLen),
            3f, cap = StrokeCap.Round)

        val armY = bodyTop + 8f
        val armLen = 10f
        drawLine(Color(0xFF2E7D32),
            Offset(figX, armY), Offset(figX - legSpread * 0.8f, armY + armLen),
            2.5f, cap = StrokeCap.Round)
        drawLine(Color(0xFF2E7D32),
            Offset(figX, armY), Offset(figX + legSpread * 0.8f, armY + armLen),
            2.5f, cap = StrokeCap.Round)
    }
}

// ── Result ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultContent(
    result: ShotResult,
    shotHistory: List<ShotResult> = emptyList(),
    settings: AppSettings,
    gpsAccuracyMeters: Double? = null,
    onNextShot: () -> Unit,
    onWindDirectionChange: () -> Unit = {},
    onWindSpeedChange: (Double) -> Unit = {},
    onClubChanged: (Club) -> Unit = {},
    newlyUnlockedAchievements: List<UnlockedAchievement> = emptyList(),
    onAchievementsSeen: () -> Unit = {}
) {
    val percentile = remember(result, shotHistory, settings.distanceUnit) {
        result.percentileAmongClub(shotHistory, settings.distanceUnit)
    }
    val isPersonalBest = percentile != null && percentile >= 100f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        // Result card — gold border + glow for PB
        val cardBorder = if (isPersonalBest)
            Modifier.border(
                2.5.dp,
                Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFAB00), Color(0xFFFFD54F))),
                RoundedCornerShape(24.dp)
            )
        else
            Modifier.border(1.5.dp, MidGray, RoundedCornerShape(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .then(cardBorder)
                .padding(28.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // PB trophy icon — animated entrance
                if (isPersonalBest) {
                    var trophyVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(400)
                        trophyVisible = true
                    }
                    AnimatedVisibility(
                        visible = trophyVisible,
                        enter = fadeIn(tween(500)) + scaleIn(
                            animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f)
                        )
                    ) {
                        Text(
                            text = "\uD83C\uDFC6",
                            fontSize = 36.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                var showClubPicker by remember { mutableStateOf(false) }
                Box(modifier = Modifier.clickable { showClubPicker = true }) {
                    ClubBadge(result.club)
                }
                if (showClubPicker) {
                    AlertDialog(
                        onDismissRequest = { showClubPicker = false },
                        confirmButton = {},
                        title = { Text("Change Club") },
                        text = {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Club.entries
                                    .filter { it in settings.enabledClubs }
                                    .sortedBy { it.sortOrder }
                                    .forEach { c ->
                                        ClubChip(
                                            club = c,
                                            selected = c == result.club,
                                            onClick = {
                                                if (c != result.club) onClubChanged(c)
                                                showClubPicker = false
                                            }
                                        )
                                    }
                            }
                        }
                    )
                }
                Spacer(Modifier.height(24.dp))

                AnimatedCounter(
                    targetValue = result.primaryDistance(settings.distanceUnit),
                    style = DistanceResult,
                    color = TextPrimary
                )
                Text(
                    text = result.primaryUnitLabel(settings.distanceUnit),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 4.sp
                )
                Text(
                    text = result.secondaryDistance(settings.distanceUnit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                // GPS accuracy indicator
                if (gpsAccuracyMeters != null) {
                    Spacer(Modifier.height(4.dp))
                    val accuracyDisplay = if (settings.distanceUnit == DistanceUnit.YARDS)
                        "${(gpsAccuracyMeters * 1.09361).toInt()} yd"
                    else
                        "${gpsAccuracyMeters.toInt()} m"
                    val accuracyColor = if (gpsAccuracyMeters > 15.0) Color(0xFFE65100) else TextTertiary
                    Text(
                        text = "\u00B1$accuracyDisplay accuracy",
                        style = MaterialTheme.typography.labelSmall,
                        color = accuracyColor
                    )
                }

                // Celebration for top shots — animated entrance
                if (percentile != null && percentile >= 80f) {
                    var badgeVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(if (isPersonalBest) 1000 else 700)
                        badgeVisible = true
                    }
                    when {
                        isPersonalBest -> {
                            Spacer(Modifier.height(20.dp))
                            AnimatedVisibility(
                                visible = badgeVisible,
                                enter = fadeIn(tween(600)) + scaleIn(
                                    animationSpec = spring(dampingRatio = 0.4f, stiffness = 180f)
                                )
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        Color(0xFFFFD54F).copy(alpha = 0.2f),
                                                        Color(0xFFFFAB00).copy(alpha = 0.15f),
                                                        Color(0xFFFF8F00).copy(alpha = 0.2f)
                                                    )
                                                )
                                            )
                                            .border(
                                                1.dp,
                                                Color(0xFFFFAB00).copy(alpha = 0.3f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(horizontal = 28.dp, vertical = 12.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "NEW PERSONAL BEST",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFF8F00),
                                                letterSpacing = 3.sp
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = "Absolutely Smacked!",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFFAB00)
                                            )
                                        }
                                    }
                                }
                            }
                        }
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
                                        .clip(RoundedCornerShape(12.dp))
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
                        else -> {
                            Spacer(Modifier.height(16.dp))
                            AnimatedVisibility(
                                visible = badgeVisible,
                                enter = fadeIn(tween(400)) + scaleIn(
                                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
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

                // Only show weather strip if weather data was actually fetched
                if (result.weatherDescription.isNotBlank()) {
                    Spacer(Modifier.height(24.dp))

                    WeatherWindStrip(
                        shot = result,
                        settings = settings,
                        editable = true,
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

                    WeatherAdjustedDistance(shot = result, settings = settings)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        ShareShotButton(shot = result, settings = settings, shotHistory = shotHistory)

        // Achievement unlock banners — delayed so user sees distance first
        if (newlyUnlockedAchievements.isNotEmpty()) {
            var showBanners by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(1500) // Let user read distance before showing celebrations
                showBanners = true
            }
            if (showBanners) {
                Spacer(Modifier.height(16.dp))
                newlyUnlockedAchievements.forEach { ua ->
                    val tColor = tierColor(ua.tier)
                    val tLabel = tierLabel(ua.tier).uppercase()
                    val tierDesc = ua.category.tiers.getOrNull(ua.tier.ordinal)?.description ?: ""
                    AchievementUnlockBanner(
                        emoji = ua.category.icon,
                        title = ua.category.displayName,
                        description = tierDesc,
                        tierLabel = tLabel,
                        tierColor = tColor
                    )
                    Spacer(Modifier.height(8.dp))
                }
                // Auto-clear after 8s, or immediately if user navigates away
                DisposableEffect(newlyUnlockedAchievements) {
                    onDispose { onAchievementsSeen() }
                }
                LaunchedEffect(newlyUnlockedAchievements) {
                    delay(8000)
                    onAchievementsSeen()
                }
            }
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
@Composable
fun WindIndicator(
    windSpeedLabel: String,
    windDegrees: Int,
    shotBearing: Double,
    windSpeedKmh: Double = 0.0,
    distanceYards: Int = 0,
    trajectoryMultiplier: Double = 1.0,
    showEffect: Boolean = false,
    distanceUnit: DistanceUnit = DistanceUnit.YARDS,
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
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MidGray)
                            .clickable { onSpeedDown() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Decrease wind speed",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = windSpeedLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.width(4.dp))
                if (hasControls && onSpeedUp != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MidGray)
                            .clickable { onSpeedUp() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Increase wind speed",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            // Wind strength description
            Text(
                text = windStrengthLabel(windSpeedKmh),
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
            if (showEffect && windSpeedKmh > 0 && distanceYards > 0) {
                val useYards = distanceUnit == DistanceUnit.YARDS
                val unitStr = if (useYards) "yds" else "m"
                val carryVal = if (useYards) effect.carryEffectYards
                    else (effect.carryEffectYards * 0.9144).roundToInt()
                val carryText = if (carryVal >= 0) "+$carryVal $unitStr" else "$carryVal $unitStr"
                Text(
                    text = carryText,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
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
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MidGray)
                        .clickable { onDirectionTap() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Cycle wind direction",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
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
