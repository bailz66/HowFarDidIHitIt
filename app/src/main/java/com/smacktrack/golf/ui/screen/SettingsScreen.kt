package com.smacktrack.golf.ui.screen

/**
 * User settings screen for configuring measurement preferences and club bag.
 *
 * Provides toggles for:
 * - Distance unit (Yards / Meters)
 * - Wind speed unit (km/h / mph)
 * - Temperature unit (F / C)
 * - Ball flight trajectory (Low / Mid / High) — affects wind calculations
 * - Club bag customization — enable/disable individual clubs
 */

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.smacktrack.golf.domain.Club
import com.smacktrack.golf.ui.AppSettings
import com.smacktrack.golf.ui.DistanceUnit
import com.smacktrack.golf.ui.SyncStatus
import com.smacktrack.golf.ui.TemperatureUnit
import com.smacktrack.golf.ui.Trajectory
import com.smacktrack.golf.ui.WindUnit
import com.smacktrack.golf.ui.theme.ChipBorder
import com.smacktrack.golf.ui.theme.ChipUnselectedBg
import com.smacktrack.golf.ui.theme.DarkGreen
import com.smacktrack.golf.ui.theme.TextSecondary
import com.smacktrack.golf.ui.theme.TextTertiary
import com.smacktrack.golf.ui.theme.clubChipColor

private val categoryOrder = listOf(
    Club.Category.WOOD,
    Club.Category.HYBRID,
    Club.Category.IRON,
    Club.Category.WEDGE
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onDistanceUnitChanged: (DistanceUnit) -> Unit,
    onWindUnitChanged: (WindUnit) -> Unit,
    onTemperatureUnitChanged: (TemperatureUnit) -> Unit,
    onTrajectoryChanged: (Trajectory) -> Unit,
    onClubToggled: (Club) -> Unit,
    isSignedIn: Boolean = false,
    userEmail: String? = null,
    syncStatus: SyncStatus = SyncStatus.IDLE,
    signInError: String? = null,
    onSignIn: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onClearError: () -> Unit = {},
    onDonate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showHowItWorks by remember { mutableStateOf(false) }

    if (showHowItWorks) {
        AlertDialog(
            onDismissRequest = { showHowItWorks = false },
            title = {
                Text(
                    "How SmackTrack Works",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "1. Tap Smack where you hit the ball\n\n" +
                    "2. Walk to where it landed and tap Track\n\n" +
                    "3. GPS locks both spots and calculates the distance\n\n" +
                    "4. Weather and wind data adjust your carry estimate\n\n" +
                    "5. Tap Share to send your shot card\n\n" +
                    "Accuracy depends on GPS signal. " +
                    "Open sky gives the best results — trees, buildings, and " +
                    "heavy cloud cover can reduce accuracy by a few yards."
                )
            },
            confirmButton = {
                TextButton(onClick = { showHowItWorks = false }) {
                    Text("Got it", color = DarkGreen)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        // ── Distance Unit ────────────────────────────────────
        SectionHeader("DISTANCE")
        Spacer(Modifier.height(12.dp))
        ToggleRow(
            options = DistanceUnit.entries.map { it.label },
            selectedIndex = DistanceUnit.entries.indexOf(settings.distanceUnit),
            onSelected = { onDistanceUnitChanged(DistanceUnit.entries[it]) }
        )

        Spacer(Modifier.height(28.dp))
        HorizontalDivider(color = Color(0xFFE0E2DC))
        Spacer(Modifier.height(28.dp))

        // ── Wind Unit ────────────────────────────────────────
        SectionHeader("WIND SPEED")
        Spacer(Modifier.height(12.dp))
        ToggleRow(
            options = WindUnit.entries.map { it.label },
            selectedIndex = WindUnit.entries.indexOf(settings.windUnit),
            onSelected = { onWindUnitChanged(WindUnit.entries[it]) }
        )

        Spacer(Modifier.height(28.dp))
        HorizontalDivider(color = Color(0xFFE0E2DC))
        Spacer(Modifier.height(28.dp))

        // ── Temperature Unit ────────────────────────────────────
        SectionHeader("TEMPERATURE")
        Spacer(Modifier.height(12.dp))
        ToggleRow(
            options = TemperatureUnit.entries.map { it.label },
            selectedIndex = TemperatureUnit.entries.indexOf(settings.temperatureUnit),
            onSelected = { onTemperatureUnitChanged(TemperatureUnit.entries[it]) }
        )

        Spacer(Modifier.height(28.dp))
        HorizontalDivider(color = Color(0xFFE0E2DC))
        Spacer(Modifier.height(28.dp))

        // ── Ball Flight ────────────────────────────────────────
        SectionHeader("BALL FLIGHT")
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Affects wind carry and lateral estimates",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary
        )
        Spacer(Modifier.height(12.dp))
        ToggleRow(
            options = Trajectory.entries.map { it.label },
            selectedIndex = Trajectory.entries.indexOf(settings.trajectory),
            onSelected = { onTrajectoryChanged(Trajectory.entries[it]) }
        )

        Spacer(Modifier.height(28.dp))
        HorizontalDivider(color = Color(0xFFE0E2DC))
        Spacer(Modifier.height(28.dp))

        // ── Clubs ────────────────────────────────────────────
        SectionHeader("MY CLUBS")
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tap to toggle clubs in your bag",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary
        )
        Spacer(Modifier.height(16.dp))

        categoryOrder.forEach { category ->
            val clubs = Club.entries
                .filter { it.category == category }
                .sortedBy { it.sortOrder }
            if (clubs.isNotEmpty()) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    clubs.forEach { club ->
                        val enabled = club in settings.enabledClubs
                        ClubToggleChip(
                            club = club,
                            enabled = enabled,
                            onClick = { onClubToggled(club) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        HorizontalDivider(color = Color(0xFFE0E2DC))
        Spacer(Modifier.height(28.dp))

        // ── How It Works ────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ChipUnselectedBg)
                .clickable { showHowItWorks = true }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "How SmackTrack Works",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = DarkGreen
            )
        }

        Spacer(Modifier.height(28.dp))
        HorizontalDivider(color = Color(0xFFE0E2DC))
        Spacer(Modifier.height(28.dp))

        // ── Account / Cloud Sync ─────────────────────────
        SectionHeader("CLOUD SYNC")
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Sign in to sync shots across devices",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary
        )
        Spacer(Modifier.height(16.dp))

        if (isSignedIn) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ChipUnselectedBg)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = userEmail ?: "Signed in",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(Modifier.height(6.dp))
                val (statusText, statusColor) = when (syncStatus) {
                    SyncStatus.IDLE -> "Local only" to TextTertiary
                    SyncStatus.SYNCING -> "Syncing..." to DarkGreen
                    SyncStatus.SYNCED -> "Synced" to DarkGreen
                    SyncStatus.ERROR -> "Sync error" to Color(0xFFB3261E)
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE8E8E8))
                        .clickable { onSignOut() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign Out",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF4285F4))
                    .clickable { onSignIn() }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign in with Google",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }

        if (signInError != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = signInError,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB3261E)
            )
        }

        Spacer(Modifier.height(28.dp))
        HorizontalDivider(color = Color(0xFFE0E2DC))
        Spacer(Modifier.height(28.dp))

        // ── Buy Me a Coffee ─────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFFFF8E1))
                .border(1.dp, Color(0xFFFFE082), RoundedCornerShape(16.dp))
                .clickable { onDonate() }
                .padding(vertical = 20.dp, horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = Color(0xFF6D4C41),
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Buy me a coffee",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4E342E)
                    )
                    Text(
                        text = "Support SmackTrack development",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8D6E63)
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp
    )
}

@Composable
private fun ToggleRow(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ChipUnselectedBg)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) DarkGreen else Color.Transparent)
                    .clickable { onSelected(index) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) Color.White else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ClubToggleChip(
    club: Club,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (enabled) clubChipColor(club.sortOrder) else ChipUnselectedBg
    val textColor = if (enabled) Color.White else TextTertiary
    val borderColor = if (enabled) clubChipColor(club.sortOrder) else ChipBorder

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
            fontWeight = if (enabled) FontWeight.Bold else FontWeight.Normal
        )
    }
}
