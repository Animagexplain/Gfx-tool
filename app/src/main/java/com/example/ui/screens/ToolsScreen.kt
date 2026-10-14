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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.data.model.BackupItem
import com.example.data.model.DeviceInfo
import com.example.data.model.GamePackage
import com.example.data.model.PresetType
import com.example.shizuku.ShizukuStatus
import com.example.ui.components.CyberCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.ShizukuConnectionStatusCard
import com.example.ui.components.ShizukuStatusBadge
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.DangerRed
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
fun ToolsScreen(
    deviceInfo: DeviceInfo,
    recommendedPreset: PresetType,
    shizukuStatus: ShizukuStatus,
    selectedGame: GamePackage,
    backups: List<BackupItem>,
    ramCleanResult: String?,
    isLoading: Boolean,
    onCleanRam: () -> Unit,
    onApplyRecommendedPreset: () -> Unit,
    onRestoreBackup: (BackupItem) -> Unit,
    onDeleteBackup: (BackupItem) -> Unit,
    onRestoreDefaultConfig: () -> Unit,
    onRefreshBackups: () -> Unit,
    onCleanShaderCache: () -> Unit = {}
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

        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "OPTIMIZATION TOOLS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = TextPrimary
                )
                Text(
                    text = "RAM cleanup, hardware profiling & config snapshots",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            ShizukuStatusBadge(status = shizukuStatus)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Shizuku Elevated Connection Status Card
        ShizukuConnectionStatusCard(
            shizukuStatus = shizukuStatus,
            showExpandableDetails = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 1. RAM Cleaner Card
        SectionHeader(
            title = "RAM Memory Cleaner",
            subtitle = "Terminates heavy background tasks via elevated am kill-all",
            icon = Icons.Default.CleaningServices
        )

        CyberCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "System Memory Pressure",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "${deviceInfo.totalRamMb - deviceInfo.availRamMb} MB Used / ${deviceInfo.totalRamMb} MB Total (${deviceInfo.usedRamPercent}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Text(
                    text = "${deviceInfo.availRamMb} MB Free",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (deviceInfo.availRamMb > 1500) MatrixGreen else ElectricGold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { deviceInfo.usedRamPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (deviceInfo.usedRamPercent > 80) DangerRed else NeonOrange,
                trackColor = DarkSurfaceElevated,
            )

            if (ramCleanResult != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MatrixGreen.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MatrixGreen.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = ramCleanResult,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MatrixGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onCleanRam,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("clean_ram_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberTeal,
                    contentColor = DarkBg
                )
            ) {
                Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clean Background RAM (am kill-all)", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onCleanShaderCache,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("clean_shader_cache_button"),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonOrange.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonOrange)
            ) {
                Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clean ${selectedGame.badge} Shaders & Cache", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Device Info Panel
        SectionHeader(
            title = "Hardware Specifications",
            subtitle = "Detected system chipset and operating environment",
            icon = Icons.Default.Devices
        )

        CyberCard {
            InfoSpecRow(label = "Device Model", value = "${deviceInfo.manufacturer} ${deviceInfo.deviceModel}")
            InfoSpecRow(label = "Processor / SoC", value = deviceInfo.cpuModel)
            InfoSpecRow(label = "Installed RAM", value = "${String.format("%.1f", deviceInfo.totalRamGb)} GB")
            InfoSpecRow(label = "Android Version", value = "Android ${deviceInfo.androidRelease} (API ${deviceInfo.apiLevel})")
            InfoSpecRow(label = "Shizuku Engine", value = when (shizukuStatus) {
                ShizukuStatus.RUNNING_AND_GRANTED -> "Active (ADB Elevated Mode)"
                ShizukuStatus.TEST_MODE -> "Active (Simulation Test Mode)"
                else -> "Inactive"
            })
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Recommended Settings Card
        SectionHeader(
            title = "Recommended Settings Engine",
            subtitle = "Hardware-tailored config calculation",
            icon = Icons.Default.AutoAwesome
        )

        CyberCard(borderColor = MatrixGreen.copy(alpha = 0.4f)) {
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
                        text = "Suggested Mode: ${recommendedPreset.title}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MatrixGreen
                    )
                    Text(
                        text = "Based on ${String.format("%.1f", deviceInfo.totalRamGb)} GB RAM and ${deviceInfo.cpuModel}",
                        style = MaterialTheme.typography.labelSmall,
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
                onClick = onApplyRecommendedPreset,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("apply_recommended_settings_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen, contentColor = DarkBg)
            ) {
                Text("Apply Recommended Preset (${recommendedPreset.title})", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Backup Manager Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(
                title = "Backup & Restore Manager",
                subtitle = "Saved configurations for ${selectedGame.displayName}",
                icon = Icons.Default.FolderZip,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onRefreshBackups,
                modifier = Modifier.testTag("refresh_backups_button")
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Backups", tint = CyberTeal)
            }
        }

        if (backups.isEmpty()) {
            CyberCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.FolderZip, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No Backups Created Yet",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Automatic timestamped backups will be generated before any write.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                backups.forEach { backup ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                            .testTag("backup_item_${backup.fileName}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = backup.fileName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${backup.formattedDate} • ${if (backup.isGameDir) "Game Folder" else "Internal Vault"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(
                                    onClick = { onRestoreBackup(backup) },
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricGold.copy(alpha = 0.6f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricGold),
                                    modifier = Modifier.testTag("restore_backup_${backup.fileName}")
                                ) {
                                    Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Restore", style = MaterialTheme.typography.labelSmall)
                                }

                                IconButton(
                                    onClick = { onDeleteBackup(backup) },
                                    modifier = Modifier.testTag("delete_backup_${backup.fileName}")
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Backup", tint = TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Reset to Factory Default Config Button
        OutlinedButton(
            onClick = onRestoreDefaultConfig,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("restore_factory_default_button"),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
        ) {
            Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Restore Factory Default Game Config")
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun InfoSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
    }
}
