package dev.zacsweers.catchup.compose

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import catchup.ui.core.R

private val LightColors =
  lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
  )

private val DarkColors =
  darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
  )

private val FONT_FAMILY by lazy {
  getGoogleFontFamily(
    name = "Nunito",
    weights =
      listOf(
        FontWeight.Bold,
        FontWeight.SemiBold,
        FontWeight.Medium,
        FontWeight.Light,
      )
  )
}

private fun getGoogleFontFamily(
  name: String,
  provider: GoogleFont.Provider = googleFontProvider,
  weights: List<FontWeight>
): FontFamily {
  return FontFamily(weights.map { Font(GoogleFont(name), provider, it) })
}

private val googleFontProvider: GoogleFont.Provider by lazy {
  GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.catchup_baseui_com_google_android_gms_fonts_certs
  )
}

@SuppressLint("NewApi") // False positive because we do check the API level.
@Composable
fun CatchUpTheme(
  useDarkTheme: Boolean = isSystemInDarkTheme(),
  isDynamicColor: Boolean = LocalDynamicTheme.current,
  content: @Composable () -> Unit
) {
  val dynamicColor = isDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
  val colors =
    when {
      dynamicColor && useDarkTheme -> {
        dynamicDarkColorScheme(LocalContext.current)
      }
      dynamicColor && !useDarkTheme -> {
        dynamicLightColorScheme(LocalContext.current)
      }
      useDarkTheme -> DarkColors
      else -> LightColors
    }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window

      window.statusBarColor = Color.Transparent.toArgb()
      window.navigationBarColor = Color.Transparent.toArgb()
      window.isNavigationBarContrastEnforced = false
    }
  }

  MaterialTheme(
    colorScheme = colors,
    typography =
      MaterialTheme.typography.copy(
        displayLarge = MaterialTheme.typography.displayLarge.copy(fontFamily = FONT_FAMILY),
        displayMedium = MaterialTheme.typography.displayMedium.copy(fontFamily = FONT_FAMILY),
        displaySmall = MaterialTheme.typography.displaySmall.copy(fontFamily = FONT_FAMILY),
        headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontFamily = FONT_FAMILY),
        headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontFamily = FONT_FAMILY),
        headlineSmall = MaterialTheme.typography.headlineSmall.copy(fontFamily = FONT_FAMILY),
        titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = FONT_FAMILY),
        titleMedium = MaterialTheme.typography.titleMedium.copy(fontFamily = FONT_FAMILY),
        titleSmall = MaterialTheme.typography.titleSmall.copy(fontFamily = FONT_FAMILY),
        bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = FONT_FAMILY),
        bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = FONT_FAMILY),
        bodySmall = MaterialTheme.typography.bodySmall.copy(fontFamily = FONT_FAMILY),
        labelLarge = MaterialTheme.typography.labelLarge.copy(fontFamily = FONT_FAMILY),
        labelMedium = MaterialTheme.typography.labelMedium.copy(fontFamily = FONT_FAMILY),
        labelSmall = MaterialTheme.typography.labelSmall.copy(fontFamily = FONT_FAMILY),
      )
  ) {
    CompositionLocalProvider(LocalDynamicTheme provides dynamicColor) { content() }
  }
}
