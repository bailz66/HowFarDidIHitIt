package com.smacktrack.golf.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smacktrack.golf.domain.Achievement
import com.smacktrack.golf.ui.theme.DarkGreen
import com.smacktrack.golf.ui.theme.TextPrimary
import com.smacktrack.golf.ui.theme.TextSecondary
import com.smacktrack.golf.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AchievementGallery(
    unlockedAchievements: Map<String, Long>,
    modifier: Modifier = Modifier
) {
    val allAchievements = Achievement.entries
    val unlocked = allAchievements
        .filter { it.name in unlockedAchievements }
        .sortedByDescending { unlockedAchievements[it.name] ?: 0L }
    val locked = allAchievements.filter { it.name !in unlockedAchievements }
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    LazyColumn(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header (top padding clears the close button)
        item {
            Text(
                text = "${unlocked.size} of ${Achievement.TOTAL} unlocked",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 40.dp, bottom = 8.dp)
            )
        }

        // Unlocked section
        if (unlocked.isNotEmpty()) {
            items(unlocked) { achievement ->
                val ts = unlockedAchievements[achievement.name] ?: 0L
                AchievementRow(
                    achievement = achievement,
                    dateText = dateFormat.format(Date(ts)),
                    isUnlocked = true
                )
            }
        }

        // Locked section
        if (locked.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "LOCKED",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp
                )
            }
            items(locked) { achievement ->
                AchievementRow(
                    achievement = achievement,
                    dateText = null,
                    isUnlocked = false
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun AchievementRow(
    achievement: Achievement,
    dateText: String?,
    isUnlocked: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isUnlocked) Color(0xFFFFF8E1) else Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isUnlocked) achievement.icon else "\uD83D\uDD12",
                fontSize = 28.sp
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isUnlocked) TextPrimary else TextTertiary
                )
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isUnlocked) TextSecondary else TextTertiary
                )
                if (dateText != null) {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkGreen
                    )
                }
            }
        }
    }
}
