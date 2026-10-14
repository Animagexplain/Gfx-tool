package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shizuku.ShizukuStatus
import com.example.ui.GfxViewModel
import com.example.ui.feedback.AppFeedbackSnackbar
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

enum class NavTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    SETTINGS("Settings", Icons.Default.Tune),
    PRESETS("Presets", Icons.Default.Bolt),
    TOOLS("Tools", Icons.Default.Build)
}

@Composable
fun MainScreen(viewModel: GfxViewModel) {
    val isDisclaimerAccepted by viewModel.isDisclaimerAccepted.collectAsState()
    val shizukuStatus by viewModel.shizukuStatus.collectAsState()
    val selectedGame by viewModel.selectedGame.collectAsState()
    val installedGames by viewModel.installedGames.collectAsState()
    val currentSettings by viewModel.currentSettings.collectAsState()
    val backups by viewModel.backups.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val recommendedPreset by viewModel.recommendedPreset.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val snackbarVisuals by viewModel.snackbarVisuals.collectAsState()
    val ramCleanResult by viewModel.ramCleanResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTab by rememberSaveable { mutableIntStateOf(NavTab.DASHBOARD.ordinal) }
    var showPermissionScreenManually by rememberSaveable { mutableStateOf(false) }
    var showBackupManagerSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Handle structured AppSnackbarVisuals feedback events
    LaunchedEffect(snackbarVisuals) {
        snackbarVisuals?.let { visuals ->
            val result = snackbarHostState.showSnackbar(visuals)
            if (result == SnackbarResult.ActionPerformed) {
                visuals.onAction?.invoke()
            }
            viewModel.clearSnackbarVisuals()
        }
    }

    // 1. Mandatory First Launch Disclaimer Dialog
    if (!isDisclaimerAccepted) {
        DisclaimerDialog(
            onAccept = { viewModel.acceptDisclaimer() }
        )
    }

    // 2. If Shizuku is not running & granted, and not in test mode, and not dismissed manually
    val isShizukuReady = shizukuStatus == ShizukuStatus.RUNNING_AND_GRANTED ||
            shizukuStatus == ShizukuStatus.TEST_MODE

    if ((!isShizukuReady || showPermissionScreenManually) && isDisclaimerAccepted) {
        SplashPermissionScreen(
            onPermissionGranted = {
                viewModel.refreshShizukuStatus()
                showPermissionScreenManually = false
            },
            onEnableTestMode = {
                viewModel.enableTestMode(true)
                showPermissionScreenManually = false
            }
        )
        return
    }

    // 3. Main Dashboard & App Structure
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_screen_scaffold"),
        containerColor = DarkBg,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.testTag("app_snackbar_host")
            ) { data ->
                AppFeedbackSnackbar(snackbarData = data)
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                NavTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab.ordinal
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab.ordinal },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DarkBg,
                            selectedTextColor = NeonOrange,
                            indicatorColor = NeonOrange,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_item_${tab.title.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = selectedTab, label = "TabCrossfade") { tabIndex ->
                when (NavTab.entries[tabIndex]) {
                    NavTab.DASHBOARD -> DashboardScreen(
                        selectedGame = selectedGame,
                        installedGames = installedGames,
                        settings = currentSettings,
                        shizukuStatus = shizukuStatus,
                        deviceInfo = deviceInfo,
                        recommendedPreset = recommendedPreset,
                        onSelectGame = { viewModel.selectGame(it) },
                        onNavigateToSettings = { selectedTab = NavTab.SETTINGS.ordinal },
                        onNavigateToPresets = { selectedTab = NavTab.PRESETS.ordinal },
                        onApplyPreset = { viewModel.applyPreset(it) },
                        onQuickRamClean = { viewModel.cleanRam() },
                        onRestoreLatestBackup = { viewModel.restoreLatestBackup() },
                        onOpenBackupManager = { showBackupManagerSheet = true },
                        onOpenShizukuStatus = { showPermissionScreenManually = true },
                        onLaunchGame = { viewModel.launchGame() }
                    )

                    NavTab.SETTINGS -> SettingsScreen(
                        selectedGame = selectedGame,
                        settings = currentSettings,
                        isLoading = isLoading,
                        onUpdateResolution = { viewModel.updateResolution(it) },
                        onUpdateGraphicsQuality = { viewModel.updateGraphicsQuality(it) },
                        onUpdateFpsCap = { viewModel.updateFpsCap(it) },
                        onUpdateAntiAliasing = { viewModel.updateAntiAliasing(it) },
                        onUpdateShadowQuality = { viewModel.updateShadowQuality(it) },
                        onUpdateTextureQuality = { viewModel.updateTextureQuality(it) },
                        onUpdateEffectsQuality = { viewModel.updateEffectsQuality(it) },
                        onUpdateGraphicsApi = { viewModel.updateGraphicsApi(it) },
                        onUpdateGpuOptimization = { viewModel.updateGpuOptimization(it) },
                        onApplySettings = { viewModel.applySettings() },
                        onApplyAndLaunch = { viewModel.applySettings(andLaunch = true) },
                        onRestoreDefaults = { viewModel.restoreDefaultConfig() },
                        onRestoreLatestBackup = { viewModel.restoreLatestBackup() }
                    )

                    NavTab.PRESETS -> PresetsScreen(
                        selectedGame = selectedGame,
                        currentSettings = currentSettings,
                        isLoading = isLoading,
                        onApplyPreset = { viewModel.applyPreset(it) }
                    )

                    NavTab.TOOLS -> ToolsScreen(
                        deviceInfo = deviceInfo,
                        recommendedPreset = recommendedPreset,
                        shizukuStatus = shizukuStatus,
                        selectedGame = selectedGame,
                        backups = backups,
                        ramCleanResult = ramCleanResult,
                        isLoading = isLoading,
                        onCleanRam = { viewModel.cleanRam() },
                        onApplyRecommendedPreset = { viewModel.applyPreset(recommendedPreset) },
                        onRestoreBackup = { viewModel.restoreBackup(it) },
                        onDeleteBackup = { viewModel.deleteBackup(it) },
                        onRestoreDefaultConfig = { viewModel.restoreDefaultConfig() },
                        onRefreshBackups = { viewModel.refreshBackups() },
                        onCleanShaderCache = { viewModel.cleanGameShaderCache() }
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBg.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeonOrange)
                }
            }
        }

        if (showBackupManagerSheet) {
            BackupManagerSheet(
                game = selectedGame,
                backups = backups,
                onDismiss = { showBackupManagerSheet = false },
                onRestoreBackup = { viewModel.restoreBackup(it) },
                onDeleteBackup = { viewModel.deleteBackup(it) }
            )
        }
    }
}
