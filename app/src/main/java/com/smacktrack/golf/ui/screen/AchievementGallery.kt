package com.smacktrack.golf.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.smacktrack.golf.domain.AchievementCategory
import com.smacktrack.golf.domain.AchievementTier
import com.smacktrack.golf.domain.UnlockedAchievement
import com.smacktrack.golf.ui.theme.TextPrimary
import com.smacktrack.golf.ui.theme.TextSecondary
import com.smacktrack.golf.ui.theme.TextTertiary
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Tier colors
private val BronzeColor = Color(0xFFCD7F32)
private val SilverColor = Color(0xFF9E9E9E)
private val GoldColor = Color(0xFFFFC107)
private val PlatinumColor = Color(0xFFB0BEC5)
private val DiamondColor = Color(0xFF42A5F5)

fun tierColor(tier: AchievementTier): Color = when (tier) {
    AchievementTier.BRONZE -> BronzeColor
    AchievementTier.SILVER -> SilverColor
    AchievementTier.GOLD -> GoldColor
    AchievementTier.PLATINUM -> PlatinumColor
    AchievementTier.DIAMOND -> DiamondColor
}

fun tierLabel(tier: AchievementTier): String = when (tier) {
    AchievementTier.BRONZE -> "Bronze"
    AchievementTier.SILVER -> "Silver"
    AchievementTier.GOLD -> "Gold"
    AchievementTier.PLATINUM -> "Platinum"
    AchievementTier.DIAMOND -> "Diamond"
}

@Composable
fun AchievementGallery(
    unlockedAchievements: Map<String, Long>,
    newlyUnlocked: List<UnlockedAchievement> = emptyList(),
    modifier: Modifier = Modifier
) {
    val categories = AchievementCategory.entries
    val totalUnlocked = unlockedAchievements.size
    var selectedCategory by remember { mutableStateOf<AchievementCategory?>(null) }

    // Track celebration state — only celebrate once per gallery open
    var showCelebration by remember { mutableStateOf(newlyUnlocked.isNotEmpty()) }
    var celebrationIndex by remember { mutableIntStateOf(0) }

    Box(modifier = modifier) {
        // Gallery content (always rendered behind)
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = "$totalUnlocked of ${AchievementCategory.TOTAL} unlocked",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 40.dp, bottom = 12.dp, start = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categories.toList()) { category ->
                    val earnedTiers = AchievementTier.entries.filter { tier ->
                        UnlockedAchievement(category, tier).storageKey in unlockedAchievements
                    }
                    val hasNew = newlyUnlocked.any { it.category == category }
                    CategoryCell(
                        category = category,
                        earnedTiers = earnedTiers,
                        isNew = hasNew && !showCelebration,
                        onClick = { selectedCategory = category }
                    )
                }
            }
        }

        // Celebration overlay
        if (showCelebration && newlyUnlocked.isNotEmpty()) {
            val currentAchievement = newlyUnlocked[celebrationIndex]

            AchievementCelebration(
                achievement = currentAchievement,
                onFinished = {
                    if (celebrationIndex < newlyUnlocked.size - 1) {
                        celebrationIndex++
                    } else {
                        showCelebration = false
                    }
                }
            )
        }
    }

    selectedCategory?.let { category ->
        CategoryDetailDialog(
            category = category,
            unlockedAchievements = unlockedAchievements,
            onDismiss = { selectedCategory = null }
        )
    }
}

// ── Epic Achievement Celebration ────────────────────────────────────────────

@Composable
private fun AchievementCelebration(
    achievement: UnlockedAchievement,
    onFinished: () -> Unit
) {
    val color = tierColor(achievement.tier)
    val colorLight = color.copy(alpha = 0.3f)
    val category = achievement.category
    val tier = achievement.tier
    val tierDef = category.tiers[tier.ordinal]

    // Animation states
    val bgAlpha = remember { Animatable(0f) }
    val iconScale = remember { Animatable(0f) }
    val glowRadius = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val tierTextAlpha = remember { Animatable(0f) }
    val descAlpha = remember { Animatable(0f) }
    val confettiProgress = remember { Animatable(0f) }
    val shimmerAngle = remember { Animatable(0f) }
    var tapToDismiss by remember { mutableStateOf(false) }

    // Confetti particles — generated once
    val particles = remember {
        List(60) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -0.5f - 0.1f, // start above
                vx = (Random.nextFloat() - 0.5f) * 0.3f,
                vy = Random.nextFloat() * 0.4f + 0.3f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 8f,
                size = Random.nextFloat() * 8f + 4f,
                color = when (Random.nextInt(5)) {
                    0 -> color
                    1 -> color.copy(alpha = 0.7f)
                    2 -> GoldColor
                    3 -> Color.White
                    else -> colorLight
                }
            )
        }
    }

    // Orchestrated animation sequence
    LaunchedEffect(achievement) {
        // Background fade in
        bgAlpha.animateTo(1f, tween(300))

        // Icon burst in with overshoot
        iconScale.animateTo(1.3f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessLow))

        // Glow pulse out from icon
        glowRadius.animateTo(1f, tween(600))

        // Confetti burst
        confettiProgress.animateTo(1f, tween(2500, easing = LinearEasing))

        // Shimmer rotation
        shimmerAngle.animateTo(360f, infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart))
    }

    LaunchedEffect(achievement) {
        delay(200)
        // Settle icon to final size
        iconScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
    }

    LaunchedEffect(achievement) {
        delay(500)
        textAlpha.animateTo(1f, tween(400))
        delay(200)
        tierTextAlpha.animateTo(1f, tween(400))
        delay(200)
        descAlpha.animateTo(1f, tween(400))
        delay(300)
        tapToDismiss = true
        // Auto-advance after 3 seconds
        delay(3000)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = tapToDismiss) { onFinished() },
        contentAlignment = Alignment.Center
    ) {
        // Dark overlay background with tier-colored gradient
        Canvas(modifier = Modifier.fillMaxSize()) {
            val alpha = bgAlpha.value
            // Dark base
            drawRect(Color.Black.copy(alpha = 0.85f * alpha))
            // Tier-colored radial glow
            val center = Offset(size.width / 2f, size.height * 0.4f)
            val glowR = size.width * 0.8f * glowRadius.value
            if (glowR > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.25f * alpha), Color.Transparent),
                        center = center,
                        radius = glowR
                    ),
                    radius = glowR,
                    center = center
                )
            }

            // Spinning rays behind the icon
            if (shimmerAngle.value > 0f) {
                val rayCount = 12
                val rayAlpha = 0.08f * alpha
                for (i in 0 until rayCount) {
                    val angle = (360f / rayCount) * i + shimmerAngle.value
                    rotate(angle, pivot = center) {
                        drawLine(
                            color = color.copy(alpha = rayAlpha),
                            start = center,
                            end = Offset(center.x, center.y - size.width * 0.4f),
                            strokeWidth = 6f
                        )
                    }
                }
            }

            // Confetti
            val t = confettiProgress.value
            if (t > 0f) {
                particles.forEach { p ->
                    val px = (p.x + p.vx * t) * size.width
                    val py = (p.y + p.vy * t + 0.3f * t * t) * size.height
                    val particleAlpha = (1f - t).coerceIn(0f, 1f) * alpha
                    if (py in 0f..size.height && particleAlpha > 0.01f) {
                        rotate(p.rotation + p.rotationSpeed * t * 360f, pivot = Offset(px, py)) {
                            drawRect(
                                color = p.color.copy(alpha = particleAlpha),
                                topLeft = Offset(px - p.size / 2f, py - p.size / 2f),
                                size = androidx.compose.ui.geometry.Size(p.size, p.size * 0.6f)
                            )
                        }
                    }
                }
            }
        }

        // Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Spacer(Modifier.height(40.dp))

            // "NEW ACHIEVEMENT" header
            AnimatedVisibility(
                visible = textAlpha.value > 0.5f,
                enter = fadeIn(tween(400)) + scaleIn(
                    initialScale = 0.5f,
                    animationSpec = spring(dampingRatio = 0.6f)
                )
            ) {
                Text(
                    text = "NEW ACHIEVEMENT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 4.sp,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(24.dp))

            // Big icon with glow ring
            Box(contentAlignment = Alignment.Center) {
                // Glow ring
                val ringAlpha by animateFloatAsState(
                    targetValue = if (glowRadius.value > 0.5f) 1f else 0f,
                    animationSpec = tween(500), label = "ringAlpha"
                )
                Canvas(modifier = Modifier.size(120.dp)) {
                    val ringColor = color.copy(alpha = 0.3f * ringAlpha)
                    drawCircle(ringColor, radius = size.width / 2f)
                    drawCircle(
                        color = color.copy(alpha = 0.5f * ringAlpha),
                        radius = size.width / 2f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(3f)
                    )
                }

                // Icon
                Text(
                    text = category.icon,
                    fontSize = 64.sp,
                    modifier = Modifier.scale(iconScale.value)
                )
            }

            Spacer(Modifier.height(20.dp))

            // Category name
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(Modifier.height(12.dp))

            // Tier badge
            AnimatedVisibility(
                visible = tierTextAlpha.value > 0.5f,
                enter = fadeIn(tween(300)) + scaleIn(
                    initialScale = 0.3f,
                    animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium)
                )
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.2f))
                        .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = tierLabel(tier).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        letterSpacing = 3.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Description
            Text(
                text = tierDef.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(descAlpha.value)
            )

            Spacer(Modifier.height(32.dp))

            // Tap to continue hint
            AnimatedVisibility(
                visible = tapToDismiss,
                enter = fadeIn(tween(600))
            ) {
                Text(
                    text = "Tap to continue",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}

private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val size: Float,
    val color: Color
)

// ── Category Cell ───────────────────────────────────────────────────────────

@Composable
private fun CategoryCell(
    category: AchievementCategory,
    earnedTiers: List<AchievementTier>,
    isNew: Boolean = false,
    onClick: () -> Unit
) {
    val highestTier = earnedTiers.maxByOrNull { it.ordinal }
    val isFullyLocked = earnedTiers.isEmpty()

    val bgColor = if (isFullyLocked) Color(0xFFF5F5F5)
    else tierColor(highestTier!!).copy(alpha = if (isNew) 0.15f else 0.08f)
    val borderColor = if (isFullyLocked) Color(0xFFE0E0E0)
    else tierColor(highestTier!!).copy(alpha = if (isNew) 0.6f else 0.3f)
    val borderWidth = if (isNew) 2.dp else 1.dp

    // Subtle pulse for new achievements
    val pulseScale by animateFloatAsState(
        targetValue = if (isNew) 1.03f else 1f,
        animationSpec = if (isNew) spring(dampingRatio = 0.3f) else tween(0),
        label = "newPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(pulseScale)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isFullyLocked) "\uD83D\uDD12" else category.icon,
                fontSize = 28.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isFullyLocked) TextTertiary else TextPrimary,
                maxLines = 1
            )
            Spacer(Modifier.height(6.dp))

            // 5 tier dots
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AchievementTier.entries.forEach { tier ->
                    val earned = tier in earnedTiers
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (earned) tierColor(tier)
                                else Color(0xFFE0E0E0)
                            )
                    )
                }
            }

            // Highest tier label
            if (highestTier != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isNew) "NEW!" else tierLabel(highestTier),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = tierColor(highestTier)
                )
            }
        }
    }
}

// ── Category Detail Dialog ──────────────────────────────────────────────────

@Composable
private fun CategoryDetailDialog(
    category: AchievementCategory,
    unlockedAchievements: Map<String, Long>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = category.icon, fontSize = 36.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(16.dp))

                AchievementTier.entries.forEachIndexed { index, tier ->
                    val storageKey = UnlockedAchievement(category, tier).storageKey
                    val earned = storageKey in unlockedAchievements
                    val tierDef = category.tiers[index]

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    if (earned) tierColor(tier)
                                    else Color(0xFFE0E0E0)
                                )
                        )
                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tierLabel(tier),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (earned) tierColor(tier) else TextTertiary
                            )
                            Text(
                                text = tierDef.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (earned) TextSecondary else TextTertiary
                            )
                        }

                        if (earned) {
                            Text(
                                text = "\u2713",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = tierColor(tier)
                            )
                        }
                    }
                }
            }
        }
    }
}
