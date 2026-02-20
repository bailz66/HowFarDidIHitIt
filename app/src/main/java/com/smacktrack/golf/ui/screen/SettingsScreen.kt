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
import androidx.compose.material3.HorizontalDivider
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
import com.smacktrack.golf.domain.Club
import com.smacktrack.golf.ui.AppSettings
import com.smacktrack.golf.ui.DistanceUnit
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
    modifier: Modifier = Modifier
) {
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
