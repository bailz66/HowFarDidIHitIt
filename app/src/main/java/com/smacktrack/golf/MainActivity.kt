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
import android.provider.Settings
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
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
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
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
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
import com.smacktrack.golf.ui.ShotPhase
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var splashFinished by remember { mutableStateOf(false) }
    var detailShot by remember { mutableStateOf<ShotResult?>(null) }
    var showAchievements by remember { mutableStateOf(false) }

    // Back button closes settings/achievements overlays instead of exiting app
    BackHandler(enabled = showSettings || showAchievements) {
        when {
            showAchievements -> showAchievements = false
            showSettings -> showSettings = false
        }
    }

    var showPermissionRationale by remember { mutableStateOf(false) }
    var showPermissionSettings by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onPermissionResult(granted)
        if (!granted) {
            // Check if we can still show rationale (user didn't check "Don't ask again")
            val activity = context as? ComponentActivity
            val canAskAgain = activity?.shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) ?: false
            if (canAskAgain) {
                showPermissionRationale = true
            } else {
                showPermissionSettings = true
            }
        }
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
            showPermissionRationale = false
            showPermissionSettings = false
        }
    }

    // Rationale dialog — user denied but can still be asked again
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Location Required") },
            text = {
                Text(
                    "SmackTrack uses GPS to measure the distance between where you " +
                    "hit the ball and where it landed. Without location permission, " +
                    "shot tracking won't work."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionRationale = false
                    permissionLauncher.launch(allPermissions)
                }) { Text("Allow Location") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) { Text("Not Now") }
            }
        )
    }

    // Permanently denied dialog — direct user to Settings app
    if (showPermissionSettings) {
        AlertDialog(
            onDismissRequest = { showPermissionSettings = false },
            title = { Text("Location Permission Needed") },
            text = {
                Text(
                    "Location permission has been permanently denied. " +
                    "To use shot tracking, please enable location in your device Settings."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionSettings = false
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    })
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionSettings = false }) { Text("Cancel") }
            }
        )
    }

    // Show toast when account is deleted
    LaunchedEffect(uiState.accountDeleted) {
        if (uiState.accountDeleted) {
            android.widget.Toast.makeText(context, "Account deleted", android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearAccountDeletedFlag()
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
                    val shotActive = uiState.phase != ShotPhase.CLUB_SELECT && uiState.phase != ShotPhase.RESULT
                    navItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            enabled = index == 0 || !shotActive,
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
                onDeleteAccount = { viewModel.deleteAccount() },
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
                    (fadeIn(tween(300, delayMillis = 50)) +
                        slideInHorizontally(tween(350)) { it / 8 * slideDir })
                        .togetherWith(
                            fadeOut(tween(200)) +
                            slideOutHorizontally(tween(300)) { -it / 8 * slideDir }
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
                        onClubChanged = viewModel::changeResultClub,
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
        Dialog(onDismissRequest = {
            showAchievements = false
            viewModel.clearNewAchievements()
        }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
            ) {
                AchievementGallery(
                    unlockedAchievements = uiState.unlockedAchievements,
                    newlyUnlocked = uiState.newlyUnlockedAchievements
                )
                IconButton(
                    onClick = {
                        showAchievements = false
                        viewModel.clearNewAchievements()
                    },
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

private data class SplashParticle(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var radius: Float, var alpha: Float,
    val color: Color, var life: Float, val decay: Float
)

private data class SplashStar(
    val nx: Float, val ny: Float,
    val baseRadius: Float, val phase: Float
)

@Composable
private fun SplashOverlay(onFinished: () -> Unit) {
    var finished by remember { mutableStateOf(false) }
    var screenSize by remember { mutableStateOf(IntSize(1080, 2400)) }
    var frame by remember { mutableIntStateOf(0) }

    // Ball arc progress (0-1, then >1 = hidden after impact)
    var ballT by remember { mutableStateOf(-0.01f) }

    // Impact effects
    var flashAlpha by remember { mutableStateOf(0f) }
    var ringProgress by remember { mutableStateOf(0f) }
    var ringAlpha by remember { mutableStateOf(0f) }
    var ring2Progress by remember { mutableStateOf(0f) }
    var ring2Alpha by remember { mutableStateOf(0f) }
    var ring3Progress by remember { mutableStateOf(0f) }
    var ring3Alpha by remember { mutableStateOf(0f) }

    // Screen shake
    var shakeX by remember { mutableStateOf(0f) }
    var shakeY by remember { mutableStateOf(0f) }

    // Logo + glow
    var logoAlpha by remember { mutableStateOf(0f) }
    var logoScale by remember { mutableStateOf(0f) }
    var glowAlpha by remember { mutableStateOf(0f) }

    // Title + shimmer
    var titleAlpha by remember { mutableStateOf(0f) }
    var titleScale by remember { mutableStateOf(0.3f) }
    var shimmerProgress by remember { mutableStateOf(-1f) }

    // Subtitle
    var subAlpha by remember { mutableStateOf(0f) }
    var subOffsetY by remember { mutableStateOf(30f) }

    // Exit
    var overlayAlpha by remember { mutableStateOf(1f) }
    var overlayScale by remember { mutableStateOf(1f) }

    // Particle systems (mutated in coroutine, read in Canvas)
    val particles = remember { mutableListOf<SplashParticle>() }
    val embers = remember { mutableListOf<SplashParticle>() }
    val trail = remember { mutableListOf<Pair<Float, Float>>() }

    // Ambient stars — 5 bright feature stars + 40 smaller ones
    val stars = remember {
        List(45) {
            SplashStar(
                nx = kotlin.random.Random.nextFloat(),
                ny = kotlin.random.Random.nextFloat(),
                baseRadius = if (it < 5) kotlin.random.Random.nextFloat() * 2f + 1.5f
                             else kotlin.random.Random.nextFloat() * 1.5f + 0.5f,
                phase = kotlin.random.Random.nextFloat() * 6.28f
            )
        }
    }

    // Easing: cubic out (fast start, gentle landing)
    fun easeOut(t: Float): Float { val inv = 1f - t; return 1f - inv * inv * inv }
    // Easing: cubic in (gentle start, fast exit)
    fun easeIn(t: Float): Float = t * t * t

    // Quadratic bezier for the golf ball drive arc (normalized 0-1 coords)
    fun ballPos(t: Float): Pair<Float, Float> {
        val e = easeOut(t)
        // Start: bottom-left, Control: high above center, End: center
        val x = (1 - e) * (1 - e) * 0.12f + 2 * (1 - e) * e * 0.45f + e * e * 0.5f
        val y = (1 - e) * (1 - e) * 1.15f + 2 * (1 - e) * e * (-0.08f) + e * e * 0.43f
        return x to y
    }

    LaunchedEffect(Unit) {
        val dt = 16L
        var ms = 0L
        var impacted = false
        var embersSpawned = false

        while (ms < 2800) {
            ms += dt
            frame++

            // ── Phase 1: Golf ball drive arc (0→600ms) ──
            if (ms <= 600) {
                ballT = (ms / 600f).coerceIn(0f, 1f)
                if (frame % 2 == 0 && ballT > 0.03f) {
                    val (bx, by) = ballPos(ballT)
                    trail.add(bx to by)
                    if (trail.size > 30) trail.removeFirst()
                }
            }

            // ── Phase 2: Impact burst (600→1100ms) ──
            if (ms in 600..620 && !impacted) {
                impacted = true
                ballT = 2f // hide the ball
                flashAlpha = 1f
                val w = screenSize.width.toFloat()
                val h = screenSize.height.toFloat()
                val cx = w * 0.5f
                val cy = h * 0.43f
                val burstColors = listOf(
                    Color(0xFFFFD700), Color(0xFFFFC107), Color(0xFF81C784),
                    Color(0xFFFFFFFF), Color(0xFFA5D6A7), Color(0xFFFFECB3),
                    Color(0xFFE8F5E9), Color(0xFFFFE082)
                )
                repeat(80) {
                    val angle = kotlin.random.Random.nextFloat() * 6.2832f
                    val speed = kotlin.random.Random.nextFloat() * 9f + 2f
                    particles.add(
                        SplashParticle(
                            x = cx, y = cy,
                            vx = kotlin.math.cos(angle) * speed,
                            vy = kotlin.math.sin(angle) * speed - 2f,
                            radius = kotlin.random.Random.nextFloat() * 5f + 1f,
                            alpha = 1f,
                            color = burstColors.random(),
                            life = 1f,
                            decay = kotlin.random.Random.nextFloat() * 0.01f + 0.006f
                        )
                    )
                }
            }

            // Screen shake on impact (600→720ms)
            if (ms in 600..720) {
                val intensity = (720 - ms) / 120f
                shakeX = (kotlin.random.Random.nextFloat() - 0.5f) * 14f * intensity
                shakeY = (kotlin.random.Random.nextFloat() - 0.5f) * 14f * intensity
            } else {
                shakeX = 0f; shakeY = 0f
            }

            if (ms > 600) {
                flashAlpha = (flashAlpha - 0.05f).coerceAtLeast(0f)
                // Ring 1: fast gold (600→1000ms)
                if (ms <= 1000) {
                    val rt = ((ms - 600) / 400f).coerceIn(0f, 1f)
                    ringProgress = easeOut(rt)
                    ringAlpha = (1f - rt) * 0.6f
                }
                // Ring 2: medium white (650→1100ms)
                if (ms in 650..1100) {
                    val rt = ((ms - 650) / 450f).coerceIn(0f, 1f)
                    ring2Progress = easeOut(rt)
                    ring2Alpha = (1f - rt) * 0.4f
                }
                // Ring 3: slow green (700→1200ms)
                if (ms in 700..1200) {
                    val rt = ((ms - 700) / 500f).coerceIn(0f, 1f)
                    ring3Progress = easeOut(rt)
                    ring3Alpha = (1f - rt) * 0.25f
                }
                // Update burst particles (physics)
                val iter = particles.iterator()
                while (iter.hasNext()) {
                    val p = iter.next()
                    p.x += p.vx; p.y += p.vy
                    p.vy += 0.1f  // gravity
                    p.vx *= 0.99f // air resistance
                    p.life -= p.decay
                    p.alpha = (p.life * p.life).coerceIn(0f, 1f)
                    if (p.life <= 0) iter.remove()
                }
            }

            // ── Floating embers — golden fireflies (spawned at 800ms) ──
            if (ms in 800..820 && !embersSpawned) {
                embersSpawned = true
                val w = screenSize.width.toFloat()
                val h = screenSize.height.toFloat()
                val cx = w * 0.5f
                val cy = h * 0.43f
                val emberColors = listOf(
                    Color(0xFFFFD700), Color(0xFFFFC107), Color(0xFFFFECB3)
                )
                repeat(25) {
                    embers.add(
                        SplashParticle(
                            x = cx + (kotlin.random.Random.nextFloat() - 0.5f) * w * 0.5f,
                            y = cy + (kotlin.random.Random.nextFloat() - 0.5f) * h * 0.15f,
                            vx = (kotlin.random.Random.nextFloat() - 0.5f) * 0.4f,
                            vy = -(kotlin.random.Random.nextFloat() * 1f + 0.3f),
                            radius = kotlin.random.Random.nextFloat() * 2.5f + 1f,
                            alpha = 0f,
                            color = emberColors.random(),
                            life = 1f,
                            decay = kotlin.random.Random.nextFloat() * 0.003f + 0.002f
                        )
                    )
                }
            }
            // Update embers
            if (embers.isNotEmpty()) {
                val eIter = embers.iterator()
                while (eIter.hasNext()) {
                    val p = eIter.next()
                    p.x += p.vx + kotlin.math.sin(frame * 0.03f + p.y * 0.01f) * 0.3f
                    p.y += p.vy
                    p.life -= p.decay
                    p.alpha = if (p.life > 0.85f) ((1f - p.life) / 0.15f * 0.6f)
                              else (p.life * 0.6f).coerceIn(0f, 0.6f)
                    if (p.life <= 0) eIter.remove()
                }
            }

            // ── Logo glow (800→2300ms) ──
            if (ms in 800..2300) {
                glowAlpha = when {
                    ms < 1100 -> easeOut(((ms - 800) / 300f).coerceIn(0f, 1f))
                    ms > 2100 -> 1f - easeIn(((ms - 2100) / 200f).coerceIn(0f, 1f))
                    else -> 1f
                }
            }

            // ── Phase 3: Logo reveal with spring overshoot (700→1200ms) ──
            if (ms in 700..1200) {
                val lt = ((ms - 700) / 500f).coerceIn(0f, 1f)
                logoAlpha = easeOut(lt)
                logoScale = if (lt < 0.55f) {
                    1.12f * easeOut(lt / 0.55f)
                } else {
                    1.12f - 0.12f * easeOut((lt - 0.55f) / 0.45f)
                }
            }

            // ── Phase 4: Title springs in (1200→1550ms) ──
            if (ms in 1200..1550) {
                val tt = ((ms - 1200) / 350f).coerceIn(0f, 1f)
                titleAlpha = easeOut(tt)
                titleScale = if (tt < 0.6f) {
                    1.06f * easeOut(tt / 0.6f)
                } else {
                    1.06f - 0.06f * easeOut((tt - 0.6f) / 0.4f)
                }
            }

            // ── Phase 5: Tagline floats up (1450→1700ms) ──
            if (ms in 1450..1700) {
                val st = ((ms - 1450) / 250f).coerceIn(0f, 1f)
                subAlpha = easeOut(st)
                subOffsetY = 30f * (1f - easeOut(st))
            }

            // ── Phase 6: Golden shimmer sweeps across title (1600→2000ms) ──
            if (ms in 1600..2000) {
                shimmerProgress = ((ms - 1600) / 400f).coerceIn(0f, 1f)
            }

            // ── Phase 7: Exit — smooth fade out (2300→2800ms) ──
            if (ms in 2300..2800) {
                val et = ((ms - 2300) / 500f).coerceIn(0f, 1f)
                overlayAlpha = 1f - easeOut(et)
                overlayScale = 1f + 0.03f * easeOut(et)
                if (ms == 2300L) trail.clear()
            }

            delay(dt)
        }

        finished = true
        onFinished()
    }

    if (!finished) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { screenSize = it }
                .graphicsLayer {
                    alpha = overlayAlpha
                    scaleX = overlayScale
                    scaleY = overlayScale
                    translationX = shakeX
                    translationY = shakeY
                },
            contentAlignment = Alignment.Center
        ) {
            // Canvas layer: gradient sky, stars, nebula, ball, particles, embers
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val time = frame * 0.016f

                // Deep green gradient background (night fairway)
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color(0xFF071E0D),
                        0.3f to Color(0xFF0F3318),
                        0.55f to Color(0xFF1B5E20),
                        0.8f to Color(0xFF0F3318),
                        1f to Color(0xFF071E0D)
                    )
                )

                // Nebula-like depth clouds (subtle atmosphere)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF2E7D32).copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(w * 0.25f, h * 0.2f), radius = w * 0.45f
                    ),
                    radius = w * 0.45f, center = Offset(w * 0.25f, h * 0.2f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF1B5E20).copy(alpha = 0.07f), Color.Transparent),
                        center = Offset(w * 0.75f, h * 0.65f), radius = w * 0.35f
                    ),
                    radius = w * 0.35f, center = Offset(w * 0.75f, h * 0.65f)
                )

                // Ambient stars twinkling
                stars.forEach { s ->
                    val twinkle = (kotlin.math.sin(time * 1.8f + s.phase) + 1f) / 2f
                    val r = s.baseRadius * (0.4f + 0.6f * twinkle)
                    val a = (0.15f + 0.55f * twinkle) * overlayAlpha
                    drawCircle(Color.White.copy(alpha = a), r, Offset(s.nx * w, s.ny * h))
                }

                // Subtle ground glow at bottom
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.7f to Color(0xFF2E7D32).copy(alpha = 0.15f),
                        1f to Color(0xFF2E7D32).copy(alpha = 0.3f)
                    )
                )

                // Ball trail (dual-color: white outer glow + golden core)
                if (trail.isNotEmpty()) {
                    trail.forEachIndexed { i, (nx, ny) ->
                        val freshness = i.toFloat() / trail.size
                        val a = freshness * 0.5f
                        val dotR = freshness * 6f + 1.5f
                        drawCircle(Color.White.copy(alpha = a * 0.4f), dotR + 4f, Offset(nx * w, ny * h))
                        drawCircle(Color(0xFFFFD700).copy(alpha = a * 0.7f), dotR * 0.5f, Offset(nx * w, ny * h))
                    }
                }

                // Golf ball (during arc phase) — enhanced glow
                if (ballT in 0f..1.01f) {
                    val (bx, by) = ballPos(ballT.coerceIn(0f, 1f))
                    val px = bx * w
                    val py = by * h
                    // Wide golden haze
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.2f), Color(0xFFFFD700).copy(alpha = 0.08f), Color.Transparent),
                            center = Offset(px, py), radius = 70f
                        ),
                        radius = 70f, center = Offset(px, py)
                    )
                    // Inner white glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.5f), Color.Transparent),
                            center = Offset(px, py), radius = 30f
                        ),
                        radius = 30f, center = Offset(px, py)
                    )
                    // Ball
                    drawCircle(Color.White, 12f, Offset(px, py))
                }

                // Impact flash (white-gold burst)
                if (flashAlpha > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                Color.White.copy(alpha = flashAlpha),
                                Color(0xFFFFD700).copy(alpha = flashAlpha * 0.3f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.5f, h * 0.43f), radius = w * 0.4f
                        ),
                        radius = w * 0.4f, center = Offset(w * 0.5f, h * 0.43f)
                    )
                }

                // Triple expanding rings (gold → white → green)
                if (ringAlpha > 0f) {
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = ringAlpha),
                        radius = ringProgress * w * 0.35f,
                        center = Offset(w * 0.5f, h * 0.43f),
                        style = Stroke(width = 2.5f)
                    )
                }
                if (ring2Alpha > 0f) {
                    drawCircle(
                        color = Color.White.copy(alpha = ring2Alpha),
                        radius = ring2Progress * w * 0.45f,
                        center = Offset(w * 0.5f, h * 0.43f),
                        style = Stroke(width = 1.5f)
                    )
                }
                if (ring3Alpha > 0f) {
                    drawCircle(
                        color = Color(0xFF81C784).copy(alpha = ring3Alpha),
                        radius = ring3Progress * w * 0.55f,
                        center = Offset(w * 0.5f, h * 0.43f),
                        style = Stroke(width = 1f)
                    )
                }

                // Burst particles with glow halos
                particles.forEach { p ->
                    if (p.alpha > 0.01f) {
                        drawCircle(p.color.copy(alpha = p.alpha * 0.3f), p.radius + 3f, Offset(p.x, p.y))
                        drawCircle(p.color.copy(alpha = p.alpha), p.radius, Offset(p.x, p.y))
                    }
                }

                // Logo backlight glow (pulsing gold-green)
                if (glowAlpha > 0f) {
                    val pulse = kotlin.math.sin(time * 2.5f) * 0.15f + 0.85f
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                Color(0xFFFFD700).copy(alpha = glowAlpha * 0.15f * pulse),
                                Color(0xFF4CAF50).copy(alpha = glowAlpha * 0.08f * pulse),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.5f, h * 0.46f), radius = w * 0.3f
                        ),
                        radius = w * 0.3f, center = Offset(w * 0.5f, h * 0.46f)
                    )
                }

                // Floating embers (golden fireflies drifting upward)
                embers.forEach { p ->
                    if (p.alpha > 0.01f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(p.color.copy(alpha = p.alpha), Color.Transparent),
                                center = Offset(p.x, p.y), radius = p.radius * 4f
                            ),
                            radius = p.radius * 4f, center = Offset(p.x, p.y)
                        )
                        drawCircle(p.color.copy(alpha = p.alpha), p.radius, Offset(p.x, p.y))
                    }
                }
            }

            // Logo + text layer (composable content centered in Box)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "SmackTrack logo",
                    modifier = Modifier
                        .size(140.dp)
                        .graphicsLayer {
                            alpha = logoAlpha
                            scaleX = logoScale
                            scaleY = logoScale
                        }
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "SmackTrack",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PoppinsFamily,
                    color = Color.White,
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = titleAlpha
                            scaleX = titleScale
                            scaleY = titleScale
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithContent {
                            drawContent()
                            if (shimmerProgress in 0f..1f) {
                                val bandWidth = size.width * 0.35f
                                val totalTravel = size.width + bandWidth * 2
                                val pos = shimmerProgress * totalTravel - bandWidth
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0xFFFFD700).copy(alpha = 0.7f),
                                            Color.White.copy(alpha = 0.5f),
                                            Color(0xFFFFD700).copy(alpha = 0.7f),
                                            Color.Transparent
                                        ),
                                        startX = pos - bandWidth / 2,
                                        endX = pos + bandWidth / 2
                                    ),
                                    blendMode = BlendMode.SrcAtop
                                )
                            }
                        }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "KNOW YOUR DISTANCE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = PoppinsFamily,
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 4.sp,
                    modifier = Modifier.graphicsLayer {
                        alpha = subAlpha
                        translationY = subOffsetY
                    }
                )
            }
        }
    }
}
