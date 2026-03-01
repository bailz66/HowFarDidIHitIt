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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.smacktrack.golf.data.AuthManager
import com.smacktrack.golf.domain.AchievementCategory
import com.smacktrack.golf.ui.AppSettings
import com.smacktrack.golf.ui.ShotResult
import com.smacktrack.golf.ui.ShotTrackerViewModel
import com.smacktrack.golf.ui.SyncStatus
import com.smacktrack.golf.ui.percentileAmongClub
import com.smacktrack.golf.ui.primaryDistance
import com.smacktrack.golf.ui.primaryUnitLabel
import com.smacktrack.golf.ui.secondaryDistance
import com.smacktrack.golf.ui.screen.AchievementGallery
import com.smacktrack.golf.ui.screen.AnalyticsScreen
import com.smacktrack.golf.ui.screen.AnimatedCounter
import com.smacktrack.golf.ui.screen.ClubBadge
import com.smacktrack.golf.ui.screen.DistanceResult
import com.smacktrack.golf.ui.screen.HistoryScreen
import com.smacktrack.golf.ui.screen.SettingsScreen
import com.smacktrack.golf.ui.screen.ShareShotButton
import com.smacktrack.golf.ui.screen.ShotTrackerScreen
import com.smacktrack.golf.ui.screen.WeatherAdjustedDistance
import com.smacktrack.golf.ui.screen.WeatherWindStrip
import com.smacktrack.golf.ui.theme.DarkGreen
import com.smacktrack.golf.ui.theme.PoppinsFamily
import com.smacktrack.golf.ui.theme.SmackTrackTheme
import com.smacktrack.golf.ui.theme.LightGreenTint
import com.smacktrack.golf.ui.theme.TextPrimary
import com.smacktrack.golf.ui.theme.TextSecondary
import com.smacktrack.golf.ui.theme.TextTertiary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<ShotTrackerViewModel>()
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authManager = AuthManager(this)
        viewModel.authManager = authManager
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

private val allPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.POST_NOTIFICATIONS
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
    var splashFinished by remember { mutableStateOf(false) }
    var detailShot by remember { mutableStateOf<ShotResult?>(null) }
    var showAchievements by remember { mutableStateOf(false) }

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
            permissionLauncher.launch(allPermissions)
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
                    if (uiState.isSignedIn && uiState.syncStatus != SyncStatus.IDLE) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (uiState.syncStatus) {
                                        SyncStatus.SYNCED -> DarkGreen
                                        SyncStatus.SYNCING -> Color(0xFFFFA000)
                                        SyncStatus.ERROR -> Color(0xFFB3261E)
                                        else -> Color.Transparent
                                    }
                                )
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    IconButton(
                        onClick = {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(DONATE_URL))
                                )
                            } catch (_: android.content.ActivityNotFoundException) { }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = "Buy me a coffee",
                            tint = TextTertiary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = { showAchievements = true }) {
                        Box {
                            Text(
                                text = "\uD83C\uDFC6",
                                fontSize = 18.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            if (uiState.unlockedAchievements.isNotEmpty()) {
                                val count = uiState.unlockedAchievements.size
                                val badgeSize = if (count >= 10) 20.dp else 16.dp
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(badgeSize)
                                        .clip(CircleShape)
                                        .background(DarkGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = if (count >= 10) 8.sp else 9.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
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
            // The web client ID is auto-generated from google-services.json
            val webClientId = context.getString(
                context.resources.getIdentifier(
                    "default_web_client_id", "string", context.packageName
                )
            )
            SettingsScreen(
                settings = uiState.settings,
                onDistanceUnitChanged = viewModel::updateDistanceUnit,
                onWindUnitChanged = viewModel::updateWindUnit,
                onTemperatureUnitChanged = viewModel::updateTemperatureUnit,
                onTrajectoryChanged = viewModel::updateTrajectory,
                onClubToggled = viewModel::toggleClub,
                isSignedIn = uiState.isSignedIn,
                userEmail = uiState.userEmail,
                syncStatus = uiState.syncStatus,
                signInError = uiState.signInError,
                onSignIn = { viewModel.signInWithGoogle(webClientId) },
                onSignOut = { viewModel.signOut() },
                onClearError = { viewModel.clearSignInError() },
                onDonate = {
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(DONATE_URL))
                        )
                    } catch (_: android.content.ActivityNotFoundException) { }
                },
                achievementCount = uiState.unlockedAchievements.size,
                totalAchievements = AchievementCategory.TOTAL,
                onOpenAchievements = { showAchievements = true },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val slideDir = if (targetState > initialState) 1 else -1
                    (fadeIn(tween(250)) + slideInHorizontally(tween(300)) { it / 6 * slideDir })
                        .togetherWith(
                            fadeOut(tween(250)) + slideOutHorizontally(tween(300)) { -it / 6 * slideDir }
                        ) using SizeTransform(clip = false)
                },
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                label = "tabTransition"
            ) { tab ->
                when (tab) {
                    0 -> ShotTrackerScreen(
                        uiState = uiState,
                        onClubSelected = viewModel::selectClub,
                        onMarkStart = viewModel::markStart,
                        onMarkEnd = viewModel::markEnd,
                        onNextShot = viewModel::nextShot,
                        onReset = viewModel::reset,
                        onWindDirectionChange = { viewModel.adjustWindDirection(45) },
                        onWindSpeedChange = viewModel::adjustWindSpeed,
                        onDeleteShot = viewModel::deleteShot,
                        onShotClicked = { detailShot = it },
                        animateEntrance = splashFinished,
                        newlyUnlockedAchievements = uiState.newlyUnlockedAchievements,
                        onAchievementsSeen = viewModel::clearNewAchievements
                    )
                    1 -> AnalyticsScreen(
                        shotHistory = uiState.shotHistory,
                        settings = uiState.settings,
                        onDeleteShot = viewModel::deleteShot,
                        onShotClicked = { detailShot = it }
                    )
                    else -> HistoryScreen(
                        shotHistory = uiState.shotHistory,
                        settings = uiState.settings,
                        onDeleteShot = viewModel::deleteShot,
                        onShotClicked = { detailShot = it }
                    )
                }
            }
        }
    }

    // Shot detail overlay
    detailShot?.let { shot ->
        ShotDetailOverlay(
            shot = shot,
            shotHistory = uiState.shotHistory,
            settings = uiState.settings,
            onDismiss = { detailShot = null }
        )
    }

    // Achievement gallery dialog
    if (showAchievements) {
        Dialog(onDismissRequest = { showAchievements = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
            ) {
                AchievementGallery(
                    unlockedAchievements = uiState.unlockedAchievements
                )
                IconButton(
                    onClick = { showAchievements = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextTertiary
                    )
                }
            }
        }
    }

    // Splash overlay on top of everything
    SplashOverlay(onFinished = { splashFinished = true })
}

// ── Shot Detail Overlay ─────────────────────────────────────────────────────

@Composable
private fun ShotDetailOverlay(
    shot: ShotResult,
    shotHistory: List<ShotResult>,
    settings: AppSettings,
    onDismiss: () -> Unit
) {
    val dateText = remember(shot.timestampMs) {
        SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
            .format(Date(shot.timestampMs))
    }
    val percentile = remember(shot, shotHistory, settings.distanceUnit) {
        shot.percentileAmongClub(shotHistory, settings.distanceUnit)
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
        ) {
            // Scrollable content
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Spacer for close button clearance
                Spacer(Modifier.height(20.dp))

                ClubBadge(shot.club)
                Spacer(Modifier.height(24.dp))

                AnimatedCounter(
                    targetValue = shot.primaryDistance(settings.distanceUnit),
                    style = DistanceResult,
                    color = TextPrimary
                )
                Text(
                    text = shot.primaryUnitLabel(settings.distanceUnit),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 4.sp
                )
                Text(
                    text = shot.secondaryDistance(settings.distanceUnit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                // Celebration badge
                CelebrationBadge(percentile)

                Spacer(Modifier.height(24.dp))
                WeatherWindStrip(shot = shot, settings = settings)
                WeatherAdjustedDistance(shot = shot, settings = settings)
                Spacer(Modifier.height(16.dp))

                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextTertiary
                )

                Spacer(Modifier.height(20.dp))
                ShareShotButton(shot = shot, settings = settings, shotHistory = shotHistory)
            }

            // Close button pinned outside scroll
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = TextTertiary
                )
            }
        }
    }
}

@Composable
private fun CelebrationBadge(percentile: Float?) {
    if (percentile == null) return
    when {
        percentile >= 95f -> {
            Spacer(Modifier.height(16.dp))
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
        percentile >= 80f -> {
            Spacer(Modifier.height(16.dp))
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

// ── Splash Overlay ──────────────────────────────────────────────────────────

@Composable
private fun SplashOverlay(onFinished: () -> Unit) {
    var logoAlpha by remember { mutableStateOf(0f) }
    var logoScale by remember { mutableStateOf(0.7f) }
    var textAlpha by remember { mutableStateOf(0f) }
    var textOffsetY by remember { mutableStateOf(20f) }
    var overlayAlpha by remember { mutableStateOf(1f) }
    var overlayScale by remember { mutableStateOf(1f) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Phase 1: Logo fades in + spring-scales (0-400ms)
        val logoSteps = 16
        for (i in 1..logoSteps) {
            val progress = i.toFloat() / logoSteps
            logoAlpha = progress
            logoScale = 0.7f + 0.3f * progress + 0.05f * kotlin.math.sin(progress * Math.PI.toFloat())
            delay(25)
        }
        logoScale = 1f
        logoAlpha = 1f

        // Phase 2: Text slides up + fades in (400-800ms)
        val textSteps = 16
        for (i in 1..textSteps) {
            val progress = i.toFloat() / textSteps
            textAlpha = progress
            textOffsetY = 20f * (1f - progress)
            delay(25)
        }
        textAlpha = 1f
        textOffsetY = 0f

        // Phase 3: Hold for brand recognition (800-1000ms)
        delay(200)

        // Phase 4: Overlay fades out + subtle scale-up (1000-1400ms)
        val exitSteps = 16
        for (i in 1..exitSteps) {
            val progress = i.toFloat() / exitSteps
            overlayAlpha = 1f - progress
            overlayScale = 1f + 0.03f * progress
            delay(25)
        }

        finished = true
        onFinished()
    }

    if (!finished) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = overlayAlpha
                    scaleX = overlayScale
                    scaleY = overlayScale
                }
                .background(DarkGreen),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "SmackTrack logo",
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer {
                            alpha = logoAlpha
                            scaleX = logoScale
                            scaleY = logoScale
                        }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "SmackTrack",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PoppinsFamily,
                    color = Color.White,
                    modifier = Modifier.graphicsLayer {
                        alpha = textAlpha
                        translationY = textOffsetY
                    }
                )
            }
        }
    }
}
