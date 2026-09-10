package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shizuku.ShizukuManager
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
import rikka.shizuku.Shizuku

/**
 * Splash and Permission screen that verifies Shizuku authorization using Shizuku.checkSelfPermission()
 * and provides an elevated button to request authorization via Shizuku.requestPermission()
 * with dynamic visual feedback for each state.
 */
@Composable
fun SplashPermissionScreen(
    onPermissionGranted: () -> Unit,
    onEnableTestMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var isChecking by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var isAuthorized by remember { mutableStateOf(false) }
    var isInstalled by remember { mutableStateOf(true) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var requestAttempted by remember { mutableStateOf(false) }

    // Direct check function using Shizuku.checkSelfPermission()
    val checkShizukuAuth = {
        isChecking = true
        try {
            val installed = ShizukuManager.isShizukuInstalled()
            isInstalled = installed

            val running = try {
                Shizuku.pingBinder()
            } catch (e: Throwable) {
                false
            }
            isRunning = running

            if (running) {
                val permissionCode = Shizuku.checkSelfPermission()
                val granted = permissionCode == PackageManager.PERMISSION_GRANTED
                isAuthorized = granted
                feedbackMessage = if (granted) {
                    "✓ Shizuku Authorized (Code: PERMISSION_GRANTED)"
                } else {
                    "Shizuku running, authorization required"
                }
            } else {
                isAuthorized = false
                feedbackMessage = if (installed) {
                    "Shizuku service is not running"
                } else {
                    "Shizuku Manager app not installed"
                }
            }
        } catch (e: Throwable) {
            isAuthorized = false
            feedbackMessage = "Error checking permission: ${e.localizedMessage}"
        } finally {
            isChecking = false
        }
    }

    // Register Shizuku runtime permission result listener to automatically update state
    DisposableEffect(Unit) {
        val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == ShizukuManager.REQUEST_CODE_PERMISSION) {
                val granted = grantResult == PackageManager.PERMISSION_GRANTED
                isAuthorized = granted
                feedbackMessage = if (granted) {
                    "✓ Permission Granted via Shizuku!"
                } else {
                    "Permission was denied. Please allow Shizuku access."
                }
                ShizukuManager.refreshStatus()
            }
        }

        val binderListener = Shizuku.OnBinderReceivedListener {
            checkShizukuAuth()
        }

        try {
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
            Shizuku.addBinderReceivedListenerSticky(binderListener)
        } catch (_: Throwable) {}

        onDispose {
            try {
                Shizuku.removeRequestPermissionResultListener(permissionResultListener)
                Shizuku.removeBinderReceivedListener(binderListener)
            } catch (_: Throwable) {}
        }
    }

    // Initial check
    LaunchedEffect(Unit) {
        checkShizukuAuth()
    }

    // Glowing animation for hero status
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val heroBorderColor by animateColorAsState(
        targetValue = when {
            isAuthorized -> MatrixGreen
            isRunning -> ElectricGold
            else -> NeonOrange
        },
        label = "borderColor"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState)
            .testTag("splash_permission_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(44.dp))

        // Hero Icon & Brand Display
        Box(
            modifier = Modifier
                .scale(if (!isAuthorized) pulseScale else 1f)
                .size(84.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(DarkSurfaceElevated)
                .border(2.dp, heroBorderColor, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    isAuthorized -> Icons.Default.CheckCircle
                    isRunning -> Icons.Default.VpnKey
                    else -> Icons.Default.Bolt
                },
                contentDescription = "GFX Pro Brand Icon",
                tint = heroBorderColor,
                modifier = Modifier.size(46.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "GFX PRO BOOSTER",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = TextPrimary,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )

        Text(
            text = "ELEVATED ENGINE CONFIGURATION",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = CyberTeal,
            letterSpacing = 1.5.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Status Card with dynamic visual feedback
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("permission_status_card"),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(heroBorderColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isAuthorized -> "STATUS: AUTHORIZED"
                            isRunning -> "STATUS: PERMISSION REQUIRED"
                            !isInstalled -> "STATUS: SHIZUKU MISSING"
                            else -> "STATUS: SERVICE STOPPED"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = heroBorderColor,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = when {
                        isAuthorized -> "Elevated Access Active"
                        isRunning -> "Authorization Needed"
                        !isInstalled -> "Shizuku Not Detected"
                        else -> "Shizuku Not Running"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when {
                        isAuthorized ->
                            "Shizuku permission is granted. Direct file access to PUBG/BGMI GameUserSettings.ini is unlocked without root."
                        isRunning ->
                            "The Shizuku daemon is running on this device. Tap the button below to grant permission via Shizuku.requestPermission()."
                        !isInstalled ->
                            "Shizuku is required to access restricted /Android/data folders without root. Install Shizuku or use Simulation Mode to preview."
                        else ->
                            "The Shizuku app is installed but its background service is stopped. Start it via Wireless Debugging or ADB commands."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                feedbackMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = DarkSurfaceElevated,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = msg,
                            color = if (isAuthorized) MatrixGreen else TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons with Visual Feedback
        if (isAuthorized) {
            // STATE 1: AUTHORIZED -> Continue to Dashboard
            Button(
                onClick = onPermissionGranted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("continue_dashboard_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Continue",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "ENTER GFX DASHBOARD",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
            }
        } else if (isRunning) {
            // STATE 2: RUNNING BUT NOT GRANTED -> Request Permission via Shizuku.requestPermission()
            Button(
                onClick = {
                    requestAttempted = true
                    try {
                        Shizuku.requestPermission(ShizukuManager.REQUEST_CODE_PERMISSION)
                    } catch (e: Throwable) {
                        feedbackMessage = "Request failed: ${e.message}"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("request_permission_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonOrange)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Request Permission",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "REQUEST SHIZUKU PERMISSION",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Check Status Button (Calls Shizuku.checkSelfPermission())
            OutlinedButton(
                onClick = { checkShizukuAuth() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("check_permission_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTeal)
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = CyberTeal,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Check Permission",
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RE-CHECK AUTHORIZATION",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        } else {
            // STATE 3: STOPPED OR NOT INSTALLED
            if (isInstalled) {
                Button(
                    onClick = {
                        val intent = context.packageManager.getLaunchIntentForPackage(ShizukuManager.SHIZUKU_PACKAGE_NAME)
                        if (intent != null) {
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("open_shizuku_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberTeal)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open Shizuku",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "OPEN SHIZUKU APP",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 13.sp
                    )
                }
            } else {
                Button(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/RikkaApps/Shizuku/releases")
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("install_shizuku_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricGold)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Install Shizuku",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "GET SHIZUKU (GITHUB / PLAY STORE)",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Refresh button
            OutlinedButton(
                onClick = { checkShizukuAuth() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("refresh_status_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = TextPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CHECK CONNECTION AGAIN",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bypass / Test Simulation Mode Button
        OutlinedButton(
            onClick = onEnableTestMode,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("test_mode_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = "Simulation Mode",
                modifier = Modifier.size(18.dp),
                tint = TextMuted
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "USE PREVIEW / SIMULATION MODE",
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Technical Guide Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "ADB Terminal",
                        tint = NeonOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Why Shizuku Elevated Access?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Android 11 Scoped Storage locks /Android/data/. Shizuku uses a privileged ADB binder process to safely inspect, backup, and apply 90 FPS & resolution settings directly without voiding device warranty or modifying game binaries.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}
