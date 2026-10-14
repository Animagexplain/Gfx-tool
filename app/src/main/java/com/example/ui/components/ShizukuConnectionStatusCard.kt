package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuStatus
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

/**
 * Composable that verifies whether Shizuku service is running and permission is granted,
 * and displays an interactive, high-fidelity connection status card with live diagnostics,
 * verification action buttons, and elevated shell verification testing.
 */
@Composable
fun ShizukuConnectionStatusCard(
    modifier: Modifier = Modifier,
    shizukuStatus: ShizukuStatus? = null,
    onRequestPermission: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    onEnableTestMode: ((Boolean) -> Unit)? = null,
    showExpandableDetails: Boolean = true
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Observe global state from ShizukuManager if not explicitly passed
    val observedStatus by ShizukuManager.status.collectAsState()
    val currentStatus = shizukuStatus ?: observedStatus

    // Local dynamic verification telemetry
    var isVerifying by remember { mutableStateOf(false) }
    var refreshRotation by remember { mutableFloatStateOf(0f) }
    var isExpanded by remember { mutableStateOf(true) }

    // Elevated command test state
    var isTestingShell by remember { mutableStateOf(false) }
    var shellTestOutput by remember { mutableStateOf<String?>(null) }
    var shellTestSuccess by remember { mutableStateOf<Boolean?>(null) }

    // Live state checks
    val isRunning = ShizukuManager.isShizukuRunning()
    val isGranted = ShizukuManager.isAuthorized()
    val isInstalled = ShizukuManager.isShizukuInstalled()
    val apiVersion = ShizukuManager.getShizukuVersion()
    val isTestMode = ShizukuManager.isTestMode()

    // Unified verification trigger
    val runVerification = {
        coroutineScope.launch {
            isVerifying = true
            refreshRotation += 360f
            ShizukuManager.refreshStatus()
            onRefresh?.invoke()
            delay(350)
            isVerifying = false
        }
    }

    // Attach Shizuku runtime listeners for binder connectivity & permission grant callbacks
    DisposableEffect(Unit) {
        val binderReceivedListener = Shizuku.OnBinderReceivedListener {
            ShizukuManager.refreshStatus()
        }
        val binderDeadListener = Shizuku.OnBinderDeadListener {
            ShizukuManager.refreshStatus()
        }
        val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == ShizukuManager.REQUEST_CODE_PERMISSION) {
                ShizukuManager.refreshStatus()
            }
        }

        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        } catch (_: Throwable) {}

        onDispose {
            try {
                Shizuku.removeBinderReceivedListener(binderReceivedListener)
                Shizuku.removeBinderDeadListener(binderDeadListener)
                Shizuku.removeRequestPermissionResultListener(permissionResultListener)
            } catch (_: Throwable) {}
        }
    }

    // Initial check on mount
    LaunchedEffect(Unit) {
        ShizukuManager.refreshStatus()
    }

    // Determine colors and visual indicators based on status
    val isFullyReady = (currentStatus == ShizukuStatus.RUNNING_AND_GRANTED) || isTestMode
    val needsPermission = currentStatus == ShizukuStatus.RUNNING_NEEDS_PERMISSION
    val isStopped = currentStatus == ShizukuStatus.SERVICE_STOPPED
    val isNotInstalled = currentStatus == ShizukuStatus.NOT_INSTALLED

    val accentColor by animateColorAsState(
        targetValue = when {
            isFullyReady && isTestMode -> CyberTeal
            isFullyReady -> MatrixGreen
            needsPermission -> ElectricGold
            isStopped -> DangerRed
            isNotInstalled -> TextMuted
            else -> NeonOrange
        },
        label = "statusAccentColor"
    )

    // Pulsing animation for the status indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val animatedRotation by animateFloatAsState(
        targetValue = refreshRotation,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "refreshAnim"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .testTag("shizuku_connection_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Status Icon, Title, Badge, and Refresh/Expand controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Status Icon Box
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.12f))
                            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val headerIcon = when {
                            isFullyReady && isTestMode -> Icons.Default.Science
                            isFullyReady -> Icons.Default.CheckCircle
                            needsPermission -> Icons.Default.Security
                            isStopped -> Icons.Default.Terminal
                            isNotInstalled -> Icons.Default.Download
                            else -> Icons.Default.Dns
                        }
                        Icon(
                            imageVector = headerIcon,
                            contentDescription = "Shizuku Status Icon",
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Shizuku Service",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Pulsing live indicator
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = pulseAlpha))
                            )
                        }

                        Text(
                            text = when (currentStatus) {
                                ShizukuStatus.RUNNING_AND_GRANTED -> "Connected & Authorized (ADB)"
                                ShizukuStatus.RUNNING_NEEDS_PERMISSION -> "Running — Permission Required"
                                ShizukuStatus.SERVICE_STOPPED -> "Service Inactive (Not Started)"
                                ShizukuStatus.NOT_INSTALLED -> "Shizuku App Not Detected"
                                ShizukuStatus.TEST_MODE -> "Simulation / Sandbox Mode"
                                ShizukuStatus.CHECKING -> "Verifying Binder Protocol..."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = accentColor
                        )
                    }
                }

                // Header Action: Refresh verification
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { runVerification() },
                        enabled = !isVerifying,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("shizuku_refresh_button")
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = accentColor
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Verify Connection",
                                tint = TextSecondary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(animatedRotation)
                            )
                        }
                    }

                    if (showExpandableDetails) {
                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Explanatory Subtitle
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = when (currentStatus) {
                    ShizukuStatus.RUNNING_AND_GRANTED ->
                        "Elevated ADB permission is active. Direct rootless read/write to GameUserSettings.ini in /Android/data is operational."
                    ShizukuStatus.RUNNING_NEEDS_PERMISSION ->
                        "Shizuku daemon is running on device, but authorization has not been granted. Grant permission to enable direct config file writes."
                    ShizukuStatus.SERVICE_STOPPED ->
                        "Shizuku app is installed, but the ADB service daemon is stopped. Start Shizuku via Wireless Debugging or use Simulation Mode."
                    ShizukuStatus.NOT_INSTALLED ->
                        "Android 11+ restricts access to /Android/data. Shizuku uses Android Wireless Debugging to safely unlock config modifications without root."
                    ShizukuStatus.TEST_MODE ->
                        "Running in standalone simulation sandbox. Config files are parsed, validated, and backed up in app sandbox storage."
                    ShizukuStatus.CHECKING ->
                        "Querying Shizuku IPC binder service..."
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            // Diagnostic Verification Badges (Expandable)
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))

                    // 4-Quadrant Diagnostic Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Service Binder Check
                        DiagnosticCheckItem(
                            modifier = Modifier.weight(1f),
                            title = "Service Binder",
                            value = when {
                                isTestMode -> "Simulated"
                                isRunning -> "Active (OK)"
                                else -> "Unreachable"
                            },
                            isPositive = isRunning || isTestMode,
                            icon = Icons.Default.Dns
                        )

                        // 2. Permission Check
                        DiagnosticCheckItem(
                            modifier = Modifier.weight(1f),
                            title = "Storage Token",
                            value = when {
                                isTestMode -> "Granted"
                                isGranted -> "Granted (UID 2000)"
                                isRunning -> "Pending Auth"
                                else -> "Not Granted"
                            },
                            isPositive = isGranted || isTestMode,
                            icon = Icons.Default.Key
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 3. API Version
                        DiagnosticCheckItem(
                            modifier = Modifier.weight(1f),
                            title = "Shizuku API",
                            value = if (apiVersion > 0) "v$apiVersion (Supported)" else if (isTestMode) "v13 (Emulated)" else "Offline",
                            isPositive = apiVersion >= 11 || isTestMode,
                            icon = Icons.Default.Code
                        )

                        // 4. File Target Access
                        DiagnosticCheckItem(
                            modifier = Modifier.weight(1f),
                            title = "Android/data",
                            value = if (isFullyReady) "Read / Write" else "Restricted",
                            isPositive = isFullyReady,
                            icon = Icons.Default.FolderSpecial
                        )
                    }

                    // Elevated Command Verification Output Box (if tested)
                    if (shellTestOutput != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkBg,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (shellTestSuccess == true) MatrixGreen.copy(alpha = 0.5f) else DangerRed.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$ whoami && id",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberTeal
                                    )
                                    Text(
                                        text = if (shellTestSuccess == true) "PASSED" else "FAILED",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (shellTestSuccess == true) MatrixGreen else DangerRed
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = shellTestOutput ?: "",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = TextPrimary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Context-Aware Action Buttons
            when {
                // State 1: Running but needs permission -> Big action to request permission
                needsPermission -> {
                    Button(
                        onClick = {
                            if (onRequestPermission != null) {
                                onRequestPermission()
                            } else {
                                ShizukuManager.requestPermission()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("shizuku_grant_permission_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricGold,
                            contentColor = DarkBg
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Grant Shizuku Permission",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // State 2: Service is stopped -> Launch Shizuku app or switch to Test Mode
                isStopped -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { launchShizukuApp(context) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("shizuku_open_app_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonOrange,
                                contentColor = DarkBg
                            )
                        ) {
                            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Shizuku", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                if (onEnableTestMode != null) {
                                    onEnableTestMode(true)
                                } else {
                                    ShizukuManager.enableTestMode(true)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("shizuku_test_mode_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberTeal.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTeal)
                        ) {
                            Icon(imageVector = Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Mode", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }

                // State 3: Not installed -> Install Shizuku or switch to Test Mode
                isNotInstalled -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { openShizukuStore(context) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("shizuku_install_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonOrange,
                                contentColor = DarkBg
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Get Shizuku", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                if (onEnableTestMode != null) {
                                    onEnableTestMode(true)
                                } else {
                                    ShizukuManager.enableTestMode(true)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("shizuku_test_mode_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberTeal.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTeal)
                        ) {
                            Icon(imageVector = Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Mode", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }

                // State 4: Fully Ready & Authorized -> Options to run Elevated Verification Test
                isFullyReady -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    isTestingShell = true
                                    try {
                                        if (isTestMode) {
                                            delay(300)
                                            shellTestOutput = "uid=2000(shell) gid=2000(shell) [Test Sandbox Active]"
                                            shellTestSuccess = true
                                        } else {
                                            val result = ShizukuManager.executeShellCommand("id")
                                            if (result.isSuccess) {
                                                shellTestOutput = result.stdout.ifEmpty { "uid=2000(shell) groups=2000(shell)" }
                                                shellTestSuccess = true
                                            } else {
                                                shellTestOutput = "Error (${result.exitCode}): ${result.stderr}"
                                                shellTestSuccess = false
                                            }
                                        }
                                    } catch (e: Throwable) {
                                        shellTestOutput = "Failed: ${e.localizedMessage}"
                                        shellTestSuccess = false
                                    } finally {
                                        isTestingShell = false
                                    }
                                }
                            },
                            enabled = !isTestingShell,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(42.dp)
                                .testTag("shizuku_test_command_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MatrixGreen.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MatrixGreen)
                        ) {
                            if (isTestingShell) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MatrixGreen
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pinging...", fontSize = 12.sp)
                            } else {
                                Icon(imageVector = Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Verify Shell (id)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        }

                        if (isTestMode) {
                            OutlinedButton(
                                onClick = {
                                    if (onEnableTestMode != null) {
                                        onEnableTestMode(false)
                                    } else {
                                        ShizukuManager.enableTestMode(false)
                                    }
                                },
                                modifier = Modifier
                                    .weight(0.8f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                            ) {
                                Text("Exit Sandbox", fontSize = 11.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { runVerification() },
                                modifier = Modifier
                                    .weight(0.8f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Re-verify", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Diagnostic status metric item displayed inside ShizukuConnectionStatusCard.
 */
@Composable
private fun DiagnosticCheckItem(
    title: String,
    value: String,
    isPositive: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        color = DarkSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPositive) MatrixGreen.copy(alpha = 0.25f) else DarkSurfaceBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isPositive) MatrixGreen.copy(alpha = 0.15f) else DarkBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isPositive) MatrixGreen else TextMuted,
                    modifier = Modifier.size(13.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isPositive) MatrixGreen else TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Helper to launch Shizuku app if installed.
 */
private fun launchShizukuApp(context: Context) {
    try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(ShizukuManager.SHIZUKU_PACKAGE_NAME)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        } else {
            openShizukuStore(context)
        }
    } catch (_: Throwable) {
        openShizukuStore(context)
    }
}

/**
 * Helper to open Shizuku Play Store or GitHub page.
 */
private fun openShizukuStore(context: Context) {
    try {
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=${ShizukuManager.SHIZUKU_PACKAGE_NAME}")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(marketIntent)
    } catch (_: Throwable) {
        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://shizuku.rikka.app/")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(browserIntent)
    }
}
