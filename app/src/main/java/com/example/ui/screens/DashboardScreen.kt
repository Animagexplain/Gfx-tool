package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceInfo
import com.example.data.model.GamePackage
import com.example.data.model.GameSettings
import com.example.data.model.PresetType
import com.example.shizuku.ShizukuStatus
import com.example.ui.components.CyberCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SettingSummaryPill
import com.example.ui.components.ShizukuStatusBadge
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.DeviceProfiler

@Composable
fun DashboardScreen(
    selectedGame: GamePackage,
    installedGames: List<GamePackage>,
    settings: GameSettings,
    shizukuStatus: ShizukuStatus,
    deviceInfo: DeviceInfo,
    recommendedPreset: PresetType,
    onSelectGame: (GamePackage) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPresets: () -> Unit,
    onApplyPreset: (PresetType) -> Unit,
    onQuickRamClean: () -> Unit,
    onRestoreLatestBackup: () -> Unit,
    onOpenBackupManager: () -> Unit,
    onOpenShizukuStatus: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GFX PRO BOOSTER",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = TextPrimary
                )
                Text(
                    text = "PUBG & BGMI Performance Engine",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Box(modifier = Modifier.clickable { onOpenShizukuStatus() }) {
                ShizukuStatusBadge(status = shizukuStatus)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Game Selector Chips
        SectionHeader(
            title = "Target Game Version",
            subtitle = "Select your installed game client",
            icon = Icons.Default.SportsEsports
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(GamePackage.entries) { game ->
                val isSelected = game == selectedGame
                val isInstalled = installedGames.contains(game)

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectGame(game) }
                        .testTag("game_chip_${game.badge}"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) NeonOrange.copy(alpha = 0.2f) else DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) NeonOrange else DarkSurfaceBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = game.badge,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) NeonOrange else TextPrimary
                                )
                                if (isInstalled) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MatrixGreen)
                                    )
                                }
                            }
                            Text(
                                text = if (isInstalled) "Detected" else "Config Target",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (isInstalled) MatrixGreen else TextMuted
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Settings Overview Card
        CyberCard(
            borderColor = NeonOrange.copy(alpha = 0.4f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ACTIVE CONFIGURATION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = NeonOrange
                    )
                    Text(
                        text = selectedGame.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                OutlinedButton(
                    onClick = onNavigateToSettings,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTeal),
                    modifier = Modifier.testTag("dashboard_edit_settings_button")
                ) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Modify", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Grid of setting summary pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingSummaryPill(
                    label = "Resolution",
                    value = settings.resolution.label,
                    accentColor = CyberTeal,
                    modifier = Modifier.weight(1f)
                )
                SettingSummaryPill(
                    label = "Quality",
                    value = settings.graphicsQuality.label,
                    accentColor = NeonOrange,
                    modifier = Modifier.weight(1f)
                )
                SettingSummaryPill(
                    label = "FPS Cap",
                    value = "${settings.fpsCap.fps} FPS",
                    accentColor = MatrixGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingSummaryPill(
                    label = "Anti-Aliasing",
                    value = if (settings.antiAliasing) "Enabled" else "Disabled",
                    accentColor = if (settings.antiAliasing) MatrixGreen else TextMuted,
                    modifier = Modifier.weight(1f)
                )
                SettingSummaryPill(
                    label = "Shadows",
                    value = settings.shadowQuality.label,
                    accentColor = ElectricGold,
                    modifier = Modifier.weight(1f)
                )
                SettingSummaryPill(
                    label = "Graphics API",
                    value = settings.graphicsApi.label,
                    accentColor = CyberTeal,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recommendation Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MatrixGreen.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .testTag("recommended_preset_banner"),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MatrixGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MatrixGreen, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AI Recommendation: ${recommendedPreset.title}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MatrixGreen
                        )
                        Text(
                            text = "${deviceInfo.manufacturer} ${deviceInfo.deviceModel} (${String.format("%.1f", deviceInfo.totalRamGb)} GB RAM)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = DeviceProfiler.getRecommendationExplanation(deviceInfo, recommendedPreset),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onApplyPreset(recommendedPreset) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("apply_recommended_preset_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen, contentColor = DarkBg)
                ) {
                    Icon(imageVector = Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Apply ${recommendedPreset.title} Now", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Actions Row
        SectionHeader(
            title = "Quick Utility Actions",
            subtitle = "Elevated system tasks and backup safety",
            icon = Icons.Default.Dashboard
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Quick RAM Clean
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onQuickRamClean() }
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                    .testTag("quick_ram_clean_card"),
                color = DarkSurface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, tint = CyberTeal, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Clean RAM", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    Text(text = "Run elevated am kill-all", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            // Restore Backup
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onRestoreLatestBackup() }
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                    .testTag("quick_restore_backup_card"),
                color = DarkSurface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null, tint = ElectricGold, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Restore .BAK", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    Text(text = "Revert to last safe state", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Backup Manager Button
        OutlinedButton(
            onClick = onOpenBackupManager,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("open_backup_manager_button"),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
        ) {
            Icon(imageVector = Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp), tint = CyberTeal)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Backup Manager & Saved Versions")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
