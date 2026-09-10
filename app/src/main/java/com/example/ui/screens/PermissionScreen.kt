package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsSystemDaydream
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuStatus
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

@Composable
fun PermissionScreen(
    status: ShizukuStatus,
    onRequestPermission: () -> Unit,
    onRefreshStatus: () -> Unit,
    onEnableTestMode: () -> Unit,
    onEnterDashboard: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        // Hero Graphic & Title
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurfaceElevated)
                .border(2.dp, NeonOrange, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = NeonOrange,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "GFX PRO BOOSTER",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            ),
            color = TextPrimary
        )

        Text(
            text = "Rootless Elevated Storage Access via Shizuku API",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        ShizukuStatusBadge(status = status)

        Spacer(modifier = Modifier.height(24.dp))

        // Status Feedback Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusIcon = when (status) {
                        ShizukuStatus.RUNNING_AND_GRANTED, ShizukuStatus.TEST_MODE -> Icons.Default.CheckCircle
                        ShizukuStatus.RUNNING_NEEDS_PERMISSION -> Icons.Default.Security
                        else -> Icons.Default.Terminal
                    }
                    val statusColor = when (status) {
                        ShizukuStatus.RUNNING_AND_GRANTED, ShizukuStatus.TEST_MODE -> MatrixGreen
                        ShizukuStatus.RUNNING_NEEDS_PERMISSION -> ElectricGold
                        else -> DangerRed
                    }

                    Icon(imageVector = statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = when (status) {
                            ShizukuStatus.RUNNING_AND_GRANTED -> "Access Granted & Ready"
                            ShizukuStatus.RUNNING_NEEDS_PERMISSION -> "Shizuku Running — Permission Needed"
                            ShizukuStatus.SERVICE_STOPPED -> "Shizuku Service is Inactive"
                            ShizukuStatus.NOT_INSTALLED -> "Shizuku App Not Detected"
                            ShizukuStatus.TEST_MODE -> "Simulation / Sandbox Mode Active"
                            ShizukuStatus.CHECKING -> "Checking Status..."
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (status) {
                        ShizukuStatus.RUNNING_AND_GRANTED ->
                            "Elevated file permissions are active. You can now tweak GameUserSettings.ini directly without needing root."
                        ShizukuStatus.RUNNING_NEEDS_PERMISSION ->
                            "Shizuku is running in ADB mode! Tap the button below to grant permission to GFX Pro Booster."
                        ShizukuStatus.SERVICE_STOPPED ->
                            "Shizuku is installed, but its ADB service has not been started yet. Start it via Wireless Debugging in the Shizuku app."
                        ShizukuStatus.NOT_INSTALLED ->
                            "On Android 11+, Android/data is restricted. Shizuku enables direct rootless file editing using Android's built-in wireless ADB."
                        ShizukuStatus.TEST_MODE ->
                            "Running in simulation test mode. All config parsing, validation, backups, and UI features are fully functional."
                        ShizukuStatus.CHECKING -> "Detecting elevated binder interface..."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons based on status
        when (status) {
            ShizukuStatus.RUNNING_AND_GRANTED, ShizukuStatus.TEST_MODE -> {
                Button(
                    onClick = onEnterDashboard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("enter_dashboard_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MatrixGreen,
                        contentColor = DarkBg
                    )
                ) {
                    Icon(imageVector = Icons.Default.Speed, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Launch Dashboard", fontWeight = FontWeight.Bold)
                }
            }

            ShizukuStatus.RUNNING_NEEDS_PERMISSION -> {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("grant_permission_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonOrange,
                        contentColor = DarkBg
                    )
                ) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Grant Shizuku Permission", fontWeight = FontWeight.Bold)
                }
            }

            else -> {
                Button(
                    onClick = {
                        val intent = context.packageManager.getLaunchIntentForPackage(ShizukuManager.SHIZUKU_PACKAGE_NAME)
                        if (intent != null) {
                            context.startActivity(intent)
                        } else {
                            onRefreshStatus()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("open_shizuku_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonOrange,
                        contentColor = DarkBg
                    )
                ) {
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Shizuku App", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Secondary action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onRefreshStatus,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("refresh_shizuku_status_button"),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Refresh", style = MaterialTheme.typography.bodyMedium)
            }

            OutlinedButton(
                onClick = onEnableTestMode,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("test_mode_button"),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberTeal.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTeal)
            ) {
                Icon(imageVector = Icons.Default.SettingsSystemDaydream, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Mode", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Instructional Setup Guide Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Rootless Shizuku Setup Guide",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = NeonOrange
                )
                Spacer(modifier = Modifier.height(12.dp))

                SetupStepItem(
                    number = "1",
                    title = "Install Shizuku",
                    desc = "Download Shizuku from GitHub or Google Play Store (free, open source)."
                )
                Spacer(modifier = Modifier.height(12.dp))
                SetupStepItem(
                    number = "2",
                    title = "Enable Wireless Debugging",
                    desc = "Go to Phone Settings -> Developer Options -> Turn on 'Wireless Debugging'."
                )
                Spacer(modifier = Modifier.height(12.dp))
                SetupStepItem(
                    number = "3",
                    title = "Start Shizuku",
                    desc = "Open Shizuku, tap 'Pairing' using the notification pairing code, then tap 'Start'."
                )
                Spacer(modifier = Modifier.height(12.dp))
                SetupStepItem(
                    number = "4",
                    title = "Authorize GFX Pro Booster",
                    desc = "Return here and tap 'Grant Shizuku Permission' to unlock config editing."
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun SetupStepItem(
    number: String,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(DarkSurfaceElevated)
                .border(1.dp, CyberTeal.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = CyberTeal
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}
