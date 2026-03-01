package com.smacktrack.golf.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.smacktrack.golf.domain.AchievementCategory
import com.smacktrack.golf.domain.AchievementTier
import com.smacktrack.golf.domain.UnlockedAchievement
import com.smacktrack.golf.ui.theme.TextPrimary
import com.smacktrack.golf.ui.theme.TextSecondary
import com.smacktrack.golf.ui.theme.TextTertiary

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
    modifier: Modifier = Modifier
) {
    val categories = AchievementCategory.entries
    val totalUnlocked = unlockedAchievements.size
    var selectedCategory by remember { mutableStateOf<AchievementCategory?>(null) }

    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        // Header
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
                CategoryCell(
                    category = category,
                    earnedTiers = earnedTiers,
                    onClick = { selectedCategory = category }
                )
            }
        }
    }

    // Detail dialog
    selectedCategory?.let { category ->
        CategoryDetailDialog(
            category = category,
            unlockedAchievements = unlockedAchievements,
            onDismiss = { selectedCategory = null }
        )
    }
}

@Composable
private fun CategoryCell(
    category: AchievementCategory,
    earnedTiers: List<AchievementTier>,
    onClick: () -> Unit
) {
    val highestTier = earnedTiers.maxByOrNull { it.ordinal }
    val isFullyLocked = earnedTiers.isEmpty()

    val bgColor = if (isFullyLocked) Color(0xFFF5F5F5)
    else tierColor(highestTier!!).copy(alpha = 0.08f)
    val borderColor = if (isFullyLocked) Color(0xFFE0E0E0)
    else tierColor(highestTier!!).copy(alpha = 0.3f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
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
                    text = tierLabel(highestTier),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = tierColor(highestTier)
                )
            }
        }
    }
}

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
                        // Tier dot
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
