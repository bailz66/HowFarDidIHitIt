package com.smacktrack.golf.ui.screen

/**
 * Shot history screen — chronological list of all recorded shots.
 *
 * Each card shows the club name, temperature, weather description,
 * wind speed/direction with a compact wind arrow, and the shot distance.
 * Most recent shots appear at the top.
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smacktrack.golf.ui.AppSettings
import com.smacktrack.golf.ui.DistanceUnit
import com.smacktrack.golf.ui.ShotResult
import com.smacktrack.golf.ui.TemperatureUnit
import com.smacktrack.golf.ui.WindUnit
import com.smacktrack.golf.ui.theme.DarkGreen
import com.smacktrack.golf.ui.theme.LightGreenTint
import com.smacktrack.golf.ui.theme.TextTertiary

@Composable
fun HistoryScreen(
    shotHistory: List<ShotResult>,
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    if (shotHistory.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No history yet",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your recorded shots\nwill appear here",
                style = MaterialTheme.typography.bodyLarge,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "${shotHistory.size} shots recorded",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        itemsIndexed(shotHistory.reversed()) { index, shot ->
            ShotHistoryCard(shot, settings)
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ShotHistoryCard(shot: ShotResult, settings: AppSettings) {
    val distance = if (settings.distanceUnit == DistanceUnit.YARDS) shot.distanceYards else shot.distanceMeters
    val distLabel = if (settings.distanceUnit == DistanceUnit.YARDS) "yds" else "m"
    val windSpeed = if (settings.windUnit == WindUnit.KMH) {
        "${shot.windSpeedKmh.toInt()} km/h"
    } else {
        "${(shot.windSpeedKmh * 0.621371).toInt()} mph"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = LightGreenTint
                ) {
                    Text(
                        text = shot.club.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkGreen,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                val tempDisplay = if (settings.temperatureUnit == TemperatureUnit.FAHRENHEIT) {
                    "${shot.temperatureF}\u00B0F"
                } else {
                    "${shot.temperatureC}\u00B0C"
                }
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
                    Spacer(Modifier.width(6.dp))
                    CompactWindArrow(
                        windDegrees = shot.windDirectionDegrees,
                        shotBearing = shot.shotBearingDegrees
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$distance",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = distLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }
    }
}
