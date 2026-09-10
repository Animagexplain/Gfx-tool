package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EffectsQuality
import com.example.data.model.FpsCap
import com.example.data.model.GamePackage
import com.example.data.model.GameSettings
import com.example.data.model.GraphicsApi
import com.example.data.model.GraphicsQuality
import com.example.data.model.Resolution
import com.example.data.model.ShadowQuality
import com.example.data.model.TextureQuality
import com.example.ui.components.CyberCard
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    selectedGame: GamePackage,
    settings: GameSettings,
    isLoading: Boolean,
    onUpdateResolution: (Resolution) -> Unit,
    onUpdateGraphicsQuality: (GraphicsQuality) -> Unit,
    onUpdateFpsCap: (FpsCap) -> Unit,
    onUpdateAntiAliasing: (Boolean) -> Unit,
    onUpdateShadowQuality: (ShadowQuality) -> Unit,
    onUpdateTextureQuality: (TextureQuality) -> Unit,
    onUpdateEffectsQuality: (EffectsQuality) -> Unit,
    onUpdateGraphicsApi: (GraphicsApi) -> Unit,
    onUpdateGpuOptimization: (Boolean) -> Unit,
    onApplySettings: () -> Unit,
    onRestoreDefaults: () -> Unit,
    onRestoreLatestBackup: () -> Unit
) {
    val scrollState = rememberScrollState()

    var graphicsDropdownExpanded by remember { mutableStateOf(false) }
    var shadowDropdownExpanded by remember { mutableStateOf(false) }
    var textureDropdownExpanded by remember { mutableStateOf(false) }
    var effectsDropdownExpanded by remember { mutableStateOf(false) }

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
                    text = "GRAPHICS CONFIGURATOR",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = TextPrimary
                )
                Text(
                    text = "Configuring for ${selectedGame.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonOrange
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
            ) {
                Text(
                    text = "Safe Backup On Save",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MatrixGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Resolution (Radio buttons)
        SectionHeader(
            title = "Resolution (Display Scaling)",
            subtitle = "Lower resolution increases framerates drastically",
            icon = Icons.Default.DisplaySettings
        )

        CyberCard {
            Resolution.entries.forEach { res ->
                val isSelected = settings.resolution == res
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onUpdateResolution(res) }
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                        .testTag("radio_res_${res.label}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onUpdateResolution(res) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = NeonOrange,
                            unselectedColor = TextMuted
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${res.label} (${res.width}x${res.height})",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isSelected) NeonOrange else TextPrimary
                        )
                        Text(
                            text = when (res) {
                                Resolution.RES_360P -> "Potato Mode — Lowest GPU load, maximum FPS"
                                Resolution.RES_480P -> "Performance — Smooth gameplay on weak chips"
                                Resolution.RES_720P -> "Standard HD — Balanced crispness & speed"
                                Resolution.RES_900P -> "Enhanced HD — Crisp display for 1080p screens"
                                Resolution.RES_1080P -> "Native Full HD — Flagship visuals"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Graphics Quality (Dropdown)
        SectionHeader(
            title = "Graphics Quality",
            subtitle = "In-game visual presentation preset",
            icon = Icons.Default.Tune
        )

        CyberCard {
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { graphicsDropdownExpanded = true }
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                        .testTag("dropdown_graphics_quality"),
                    color = DarkSurfaceElevated,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Selected Quality", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                text = settings.graphicsQuality.label,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = NeonOrange
                            )
                        }
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = TextPrimary)
                    }
                }

                DropdownMenu(
                    expanded = graphicsDropdownExpanded,
                    onDismissRequest = { graphicsDropdownExpanded = false },
                    modifier = Modifier
                        .background(DarkSurfaceElevated)
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                ) {
                    GraphicsQuality.entries.forEach { quality ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = quality.label,
                                    color = if (quality == settings.graphicsQuality) NeonOrange else TextPrimary,
                                    fontWeight = if (quality == settings.graphicsQuality) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onUpdateGraphicsQuality(quality)
                                graphicsDropdownExpanded = false
                            },
                            modifier = Modifier.testTag("menu_item_quality_${quality.label}")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. FPS Cap (Chips)
        SectionHeader(
            title = "FPS Cap (Frame Rate Limit)",
            subtitle = "Target frames per second limiter",
            icon = Icons.Default.Speed
        )

        CyberCard {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FpsCap.entries.forEach { cap ->
                    val isSelected = settings.fpsCap == cap
                    FilterChip(
                        selected = isSelected,
                        onClick = { onUpdateFpsCap(cap) },
                        label = {
                            Text(
                                text = cap.label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonOrange,
                            selectedLabelColor = DarkBg,
                            selectedLeadingIconColor = DarkBg,
                            containerColor = DarkSurfaceElevated,
                            labelColor = TextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) NeonOrange else DarkSurfaceBorder
                        ),
                        modifier = Modifier.testTag("fps_chip_${cap.fps}")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Anti-Aliasing (Toggle Switch)
        SectionHeader(
            title = "Anti-Aliasing (MSAA)",
            subtitle = "Reduces edge staircasing; disable for max FPS"
        )

        CyberCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (settings.antiAliasing) "Anti-Aliasing Enabled" else "Anti-Aliasing Disabled",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = if (settings.antiAliasing) MatrixGreen else TextPrimary
                    )
                    Text(
                        text = if (settings.antiAliasing) "Smooths jagged edges at minor GPU cost" else "Disabled for maximum competitive frames",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Switch(
                    checked = settings.antiAliasing,
                    onCheckedChange = onUpdateAntiAliasing,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBg,
                        checkedTrackColor = MatrixGreen,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceElevated
                    ),
                    modifier = Modifier.testTag("switch_anti_aliasing")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Shadow Quality (Dropdown)
        SectionHeader(
            title = "Shadow Quality",
            subtitle = "Dynamic shadow rendering level"
        )

        CyberCard {
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { shadowDropdownExpanded = true }
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                        .testTag("dropdown_shadow_quality"),
                    color = DarkSurfaceElevated,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Shadows", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                text = settings.shadowQuality.label,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = CyberTeal
                            )
                        }
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = TextPrimary)
                    }
                }

                DropdownMenu(
                    expanded = shadowDropdownExpanded,
                    onDismissRequest = { shadowDropdownExpanded = false },
                    modifier = Modifier
                        .background(DarkSurfaceElevated)
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                ) {
                    ShadowQuality.entries.forEach { shadow ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = shadow.label,
                                    color = if (shadow == settings.shadowQuality) CyberTeal else TextPrimary,
                                    fontWeight = if (shadow == settings.shadowQuality) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onUpdateShadowQuality(shadow)
                                shadowDropdownExpanded = false
                            },
                            modifier = Modifier.testTag("menu_item_shadow_${shadow.label}")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 6. Texture Quality (Dropdown)
        SectionHeader(
            title = "Texture Quality",
            subtitle = "Game texture detail and VRAM caching"
        )

        CyberCard {
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { textureDropdownExpanded = true }
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                        .testTag("dropdown_texture_quality"),
                    color = DarkSurfaceElevated,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Textures", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                text = settings.textureQuality.label,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = ElectricGold
                            )
                        }
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = TextPrimary)
                    }
                }

                DropdownMenu(
                    expanded = textureDropdownExpanded,
                    onDismissRequest = { textureDropdownExpanded = false },
                    modifier = Modifier
                        .background(DarkSurfaceElevated)
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                ) {
                    TextureQuality.entries.forEach { texture ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = texture.label,
                                    color = if (texture == settings.textureQuality) ElectricGold else TextPrimary,
                                    fontWeight = if (texture == settings.textureQuality) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onUpdateTextureQuality(texture)
                                textureDropdownExpanded = false
                            },
                            modifier = Modifier.testTag("menu_item_texture_${texture.label}")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 7. Effects Quality (Dropdown)
        SectionHeader(
            title = "Effects Quality",
            subtitle = "Particle effects, smoke, and combat explosions"
        )

        CyberCard {
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { effectsDropdownExpanded = true }
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                        .testTag("dropdown_effects_quality"),
                    color = DarkSurfaceElevated,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Effects", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                text = settings.effectsQuality.label,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = NeonOrange
                            )
                        }
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = TextPrimary)
                    }
                }

                DropdownMenu(
                    expanded = effectsDropdownExpanded,
                    onDismissRequest = { effectsDropdownExpanded = false },
                    modifier = Modifier
                        .background(DarkSurfaceElevated)
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                ) {
                    EffectsQuality.entries.forEach { effects ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = effects.label,
                                    color = if (effects == settings.effectsQuality) NeonOrange else TextPrimary,
                                    fontWeight = if (effects == settings.effectsQuality) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onUpdateEffectsQuality(effects)
                                effectsDropdownExpanded = false
                            },
                            modifier = Modifier.testTag("menu_item_effects_${effects.label}")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 8. Graphics API (Radio: Vulkan / OpenGL ES)
        SectionHeader(
            title = "Graphics API",
            subtitle = "Driver backend (Vulkan recommended for Android 10+)"
        )

        CyberCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GraphicsApi.entries.forEach { api ->
                    val isSelected = settings.graphicsApi == api
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onUpdateGraphicsApi(api) }
                            .border(
                                1.dp,
                                if (isSelected) CyberTeal else DarkSurfaceBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .testTag("radio_api_${api.label}"),
                        color = if (isSelected) CyberTeal.copy(alpha = 0.15f) else DarkSurfaceElevated,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onUpdateGraphicsApi(api) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = CyberTeal,
                                    unselectedColor = TextMuted
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = api.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) CyberTeal else TextPrimary
                                )
                                Text(
                                    text = if (api == GraphicsApi.VULKAN) "High efficiency" else "Legacy fallback",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 9. GPU Optimization (Toggle switch)
        SectionHeader(
            title = "GPU Optimization",
            subtitle = "Enables UE4 driver compiler and shader cache tuning"
        )

        CyberCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (settings.gpuOptimization) "GPU Optimization Active" else "GPU Optimization Off",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = if (settings.gpuOptimization) NeonOrange else TextPrimary
                    )
                    Text(
                        text = "Reduces rendering stutters and stabilizes frametimes",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Switch(
                    checked = settings.gpuOptimization,
                    onCheckedChange = onUpdateGpuOptimization,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBg,
                        checkedTrackColor = NeonOrange,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceElevated
                    ),
                    modifier = Modifier.testTag("switch_gpu_optimization")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Primary Action: Save & Apply Button
        Button(
            onClick = onApplySettings,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("apply_settings_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonOrange,
                contentColor = DarkBg
            )
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isLoading) "Writing Config & Backing Up..." else "Apply & Save to Game",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Revert & Restore defaults row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onRestoreLatestBackup,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("restore_backup_button"),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricGold.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricGold)
            ) {
                Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Restore .BAK", style = MaterialTheme.typography.bodySmall)
            }

            OutlinedButton(
                onClick = onRestoreDefaults,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("restore_defaults_button"),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
            ) {
                Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Defaults", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
