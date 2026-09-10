package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GamingDarkColorScheme = darkColorScheme(
  primary = NeonOrange,
  onPrimary = DarkBg,
  primaryContainer = DarkSurfaceElevated,
  onPrimaryContainer = NeonOrange,
  secondary = CyberTeal,
  onSecondary = DarkBg,
  secondaryContainer = DarkSurfaceElevated,
  onSecondaryContainer = CyberTeal,
  tertiary = MatrixGreen,
  onTertiary = DarkBg,
  background = DarkBg,
  onBackground = TextPrimary,
  surface = DarkSurface,
  onSurface = TextPrimary,
  surfaceVariant = DarkSurfaceElevated,
  onSurfaceVariant = TextSecondary,
  outline = DarkSurfaceBorder,
  outlineVariant = DarkSurfaceBorder.copy(alpha = 0.5f),
  error = DangerRed,
  onError = DarkBg
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = GamingDarkColorScheme,
    typography = Typography,
    content = content
  )
}
