package com.smacktrack.golf

/**
 * Single-activity entry point for the app.
 *
 * Hosts a [Scaffold] with a top app bar ("SmackTrack"), bottom navigation
 * (Tracker / Stats / History), and a settings overlay toggled via the gear icon.
 * All screens share a single [ShotTrackerViewModel] instance for consistent state.
 *
 * Handles runtime location permission request on first launch.
 */

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
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
    private val viewModel by viewModels<ShotTrackerViewModel>()

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

private const val DONATE_URL = "https://ko-fi.com/smacktrack"

private val locationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
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
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onPermissionResult(granted)
        permissionRequested = true
    }

    // Check permission on first composition; request once if not granted
    LaunchedEffect(Unit) {
        val alreadyGranted = locationPermissions.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (alreadyGranted) {
            viewModel.onPermissionResult(true)
        } else {
            permissionLauncher.launch(locationPermissions)
        }
    }

    // Re-check permission on each resume (user may have granted via Settings)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val granted = locationPermissions.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (granted) {
            viewModel.onPermissionResult(true)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (showSettings) "Settings" else "SmackTrack",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            showSettings = !showSettings
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (showSettings) DarkGreen else TextTertiary,
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
            if (!showSettings) {
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
        if (showSettings) {
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
                    onWindDirectionChange = { viewModel.adjustWindDirection(45) },
                    onWindSpeedChange = viewModel::adjustWindSpeed,
                    onDonate = {
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(DONATE_URL))
                            )
                        } catch (_: android.content.ActivityNotFoundException) {
                            // No browser installed — silently ignore
                        }
                    },
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
