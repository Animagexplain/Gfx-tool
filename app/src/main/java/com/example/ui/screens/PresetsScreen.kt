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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.data.model.GamePackage
import com.example.data.model.GameSettings
import com.example.data.model.PresetType
import com.example.ui.components.SectionHeader
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

@Composable
fun PresetsScreen(
    selectedGame: GamePackage,
    currentSettings: GameSettings,
    isLoading: Boolean,
    onApplyPreset: (PresetType) -> Unit
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

        // Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ONE-TAP PRESETS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = TextPrimary
                )
                Text(
                    text = "Engineered graphics tuning for ${selectedGame.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated)
                    .border(1.dp, NeonOrange.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = NeonOrange, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Presets including 120 FPS
        val presets = listOf(
            PresetType.ULTRA_EXTREME_120,
            PresetType.SMOOTH,
            PresetType.BALANCED,
            PresetType.HD,
            PresetType.POTATO
        )

        presets.forEach { preset ->
            val isCurrent = currentSettings.resolution == preset.settings.resolution &&
                    currentSettings.graphicsQuality == preset.settings.graphicsQuality &&
                    currentSettings.fpsCap == preset.settings.fpsCap

            val accentColor = when (preset) {
                PresetType.ULTRA_EXTREME_120 -> CyberTeal
                PresetType.POTATO -> ElectricGold
                PresetType.SMOOTH -> NeonOrange
                PresetType.BALANCED -> CyberTeal
                PresetType.HD -> MatrixGreen
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(
                        1.dp,
                        if (isCurrent) accentColor else DarkSurfaceBorder,
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("preset_card_${preset.name}"),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                val icon = when (preset) {
                                    PresetType.ULTRA_EXTREME_120 -> Icons.Default.Bolt
                                    PresetType.POTATO -> Icons.Default.FlashOn
                                    PresetType.SMOOTH -> Icons.Default.RocketLaunch
                                    PresetType.BALANCED -> Icons.Default.Speed
                                    PresetType.HD -> Icons.Default.Star
                                }
                                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = preset.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = preset.idealFor,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCurrent) MatrixGreen.copy(alpha = 0.2f) else DarkSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCurrent) MatrixGreen else DarkSurfaceBorder
                            )
                        ) {
                            Text(
                                text = if (isCurrent) "ACTIVE" else preset.tag,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = if (isCurrent) MatrixGreen else TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = preset.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset Spec Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PresetSpecPill(label = "Res", value = preset.settings.resolution.label, modifier = Modifier.weight(1f))
                        PresetSpecPill(label = "FPS", value = "${preset.settings.fpsCap.fps}", modifier = Modifier.weight(1f))
                        PresetSpecPill(label = "Quality", value = preset.settings.graphicsQuality.label, modifier = Modifier.weight(1f))
                        PresetSpecPill(label = "API", value = preset.settings.graphicsApi.label, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // One-Tap Apply Button
                    Button(
                        onClick = { onApplyPreset(preset) },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("apply_preset_button_${preset.name}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = DarkBg
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Apply ${preset.title} (1-Tap)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun PresetSpecPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = DarkSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextMuted)
            Text(text = value, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
        }
    }
}
