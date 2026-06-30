package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private fun createDarkColorScheme(
    primary: Color,
    secondary: Color,
    onPrimary: Color
) = darkColorScheme(
    primary = primary,
    secondary = secondary,
    tertiary = GlobalTertiary,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = onPrimary,
    onBackground = OnDarkBackground,
    onSurface = OnDarkSurface,
    outline = primary, // Set outline to match primary color
    outlineVariant = primary.copy(alpha = 0.5f)
)

private val GreenColorScheme = createDarkColorScheme(GreenPrimary, GreenSecondary, OnGreenPrimary)
private val BlueColorScheme = createDarkColorScheme(BluePrimary, BlueSecondary, OnBluePrimary)
private val RedColorScheme = createDarkColorScheme(RedPrimary, RedSecondary, OnRedPrimary)
private val YellowColorScheme = createDarkColorScheme(YellowPrimary, YellowSecondary, OnYellowPrimary)

@Composable
fun MyApplicationTheme(
  colorTheme: String = "blue",
  dynamicColor: Boolean = false, // disabled to enforce premium brand styling
  content: @Composable () -> Unit,
) {
  val colorScheme = when (colorTheme) {
      "green" -> GreenColorScheme
      "red" -> RedColorScheme
      "yellow" -> YellowColorScheme
      else -> BlueColorScheme // default
  }
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
