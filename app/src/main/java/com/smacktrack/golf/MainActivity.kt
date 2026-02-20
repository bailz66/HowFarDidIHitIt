package com.smacktrack.golf

/**
 * Single-activity entry point for the app.
 *
 * Hosts a [Scaffold] with a top app bar ("SmackTrack"), bottom navigation
 * (Tracker / Stats / History), and a settings overlay toggled via the gear icon.
 * All screens share a single [ShotTrackerViewModel] instance for consistent state.
 */

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smacktrack.golf.ui.ShotTrackerViewModel
import com.smacktrack.golf.ui.screen.AnalyticsScreen
import com.smacktrack.golf.ui.screen.HistoryScreen
import com.smacktrack.golf.ui.screen.SettingsScreen
import com.smacktrack.golf.ui.screen.ShotTrackerScreen
import com.smacktrack.golf.ui.theme.DarkGreen
import com.smacktrack.golf.ui.theme.SmackTrackTheme
import com.smacktrack.golf.ui.theme.LightGreenTint
import com.smacktrack.golf.ui.theme.TextTertiary

class MainActivity : ComponentActivity() {
    private val viewModel = ShotTrackerViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmackTrackTheme {
                SmackTrackApp(viewModel)
            }
        }
    }
}

private data class NavItem(
    val label: String,
    val icon: ImageVector
)

private val navItems = listOf(
    NavItem("Tracker", Icons.Default.Place),
    NavItem("Stats", Icons.Default.Star),
    NavItem("History", Icons.AutoMirrored.Default.List)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmackTrackApp(viewModel: ShotTrackerViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    // 3 = settings (not in bottom nav, opened via icon)
    var showSettings by remember { mutableIntStateOf(0) } // 0 = normal, 1 = settings

    val currentlyShowingSettings = showSettings == 1

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (currentlyShowingSettings) "Settings" else "SmackTrack",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            showSettings = if (currentlyShowingSettings) 0 else 1
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (currentlyShowingSettings) DarkGreen else TextTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            if (!currentlyShowingSettings) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    navItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DarkGreen,
                                selectedTextColor = DarkGreen,
                                indicatorColor = LightGreenTint,
                                unselectedIconColor = TextTertiary,
                                unselectedTextColor = TextTertiary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (currentlyShowingSettings) {
            SettingsScreen(
                settings = uiState.settings,
                onDistanceUnitChanged = viewModel::updateDistanceUnit,
                onWindUnitChanged = viewModel::updateWindUnit,
                onTemperatureUnitChanged = viewModel::updateTemperatureUnit,
                onTrajectoryChanged = viewModel::updateTrajectory,
                onClubToggled = viewModel::toggleClub,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            when (selectedTab) {
                0 -> ShotTrackerScreen(
                    uiState = uiState,
                    onClubSelected = viewModel::selectClub,
                    onMarkStart = viewModel::markStart,
                    onMarkEnd = viewModel::markEnd,
                    onNextShot = viewModel::nextShot,
                    onReset = viewModel::reset,
                    modifier = Modifier.padding(innerPadding)
                )
                1 -> AnalyticsScreen(
                    shotHistory = uiState.shotHistory,
                    settings = uiState.settings,
                    modifier = Modifier.padding(innerPadding)
                )
                2 -> HistoryScreen(
                    shotHistory = uiState.shotHistory,
                    settings = uiState.settings,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
