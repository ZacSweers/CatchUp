package catchup.base.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

/*
 * This file is a slimmed down implementation of the original deprecated Accompanist
 * SystemUiController focused just on coloring system bars.
 */

private const val MIN_DARK_ICONS_LUMINANCE = 0.5f

/**
 * A class which provides easy-to-use utilities for coloring the System UI bar colors within Jetpack
 * Compose.
 */
@Stable
interface SystemBarColorController {

  /**
   * Set the status bar color.
   *
   * @param color The **desired** [Color] to set. This may require modification if running on an API
   *   level that only supports white status bar icons.
   * @param darkIcons Whether dark status bar icons would be preferable.
   * @param transformColorForLightContent A lambda which will be invoked to transform [color] if
   *   dark icons were requested but are not available. Defaults to applying a black scrim.
   * @see statusBarDarkContentEnabled
   */
  fun setStatusBarColor(
    color: Color,
    darkIcons: Boolean = color.luminance() > MIN_DARK_ICONS_LUMINANCE,
    transformColorForLightContent: (Color) -> Color = BlackScrimmed
  )

  /**
   * Set the navigation bar color.
   *
   * @param color The **desired** [Color] to set. This may require modification if running on an API
   *   level that only supports white navigation bar icons. Additionally this will be ignored and
   *   [Color.Transparent] will be used on API 29+ where gesture navigation is preferred or the
   *   system UI automatically applies background protection in other navigation modes.
   * @param darkIcons Whether dark navigation bar icons would be preferable.
   * @param navigationBarContrastEnforced Whether the system should ensure that the navigation bar
   *   has enough contrast when a fully transparent background is requested. Only supported on API
   *   29+.
   * @param transformColorForLightContent A lambda which will be invoked to transform [color] if
   *   dark icons were requested but are not available. Defaults to applying a black scrim.
   * @see navigationBarDarkContentEnabled
   * @see navigationBarContrastEnforced
   */
  fun setNavigationBarColor(
    color: Color,
    darkIcons: Boolean = color.luminance() > MIN_DARK_ICONS_LUMINANCE,
    navigationBarContrastEnforced: Boolean = true,
    transformColorForLightContent: (Color) -> Color = BlackScrimmed
  )

  /**
   * Set the status and navigation bars to [color].
   *
   * @see setStatusBarColor
   * @see setNavigationBarColor
   */
  fun setSystemBarsColor(
    color: Color,
    darkIcons: Boolean = color.luminance() > MIN_DARK_ICONS_LUMINANCE,
    isNavigationBarContrastEnforced: Boolean = true,
    transformColorForLightContent: (Color) -> Color = BlackScrimmed
  ) {
    setStatusBarColor(color, darkIcons, transformColorForLightContent)
    setNavigationBarColor(
      color,
      darkIcons,
      isNavigationBarContrastEnforced,
      transformColorForLightContent
    )
  }

  /** Property which holds whether the status bar icons + content are 'dark' or not. */
  var statusBarDarkContentEnabled: Boolean

  /** Property which holds whether the navigation bar icons + content are 'dark' or not. */
  var navigationBarDarkContentEnabled: Boolean

  /** Property which holds whether the status & navigation bar icons + content are 'dark' or not. */
  var systemBarsDarkContentEnabled: Boolean
    get() = statusBarDarkContentEnabled && navigationBarDarkContentEnabled
    set(value) {
      statusBarDarkContentEnabled = value
      navigationBarDarkContentEnabled = value
    }

  /**
   * Property which holds whether the system is ensuring that the navigation bar has enough contrast
   * when a fully transparent background is requested. Only has an affect when running on Android
   * API 29+ devices.
   */
  var isNavigationBarContrastEnforced: Boolean
}

/**
 * Remembers a [SystemBarColorController] for the given [window].
 *
 * If no [window] is provided, an attempt to find the correct [Window] is made.
 *
 * First, if the [LocalView]'s parent is a [DialogWindowProvider], then that dialog's [Window] will
 * be used.
 *
 * Second, we attempt to find [Window] for the [Activity] containing the [LocalView].
 *
 * If none of these are found (such as may happen in a preview), then the functionality of the
 * returned [SystemBarColorController] will be degraded, but won't throw an exception.
 */
@Composable
fun rememberSystemBarColorController(
  window: Window? = findWindow(),
): SystemBarColorController {
  val view = LocalView.current
  return remember(view, window) { AndroidSystemBarColorController(view, window) }
}

@Composable
private fun findWindow(): Window? =
  (LocalView.current.parent as? DialogWindowProvider)?.window
    ?: LocalView.current.context.findWindow()

private tailrec fun Context.findWindow(): Window? =
  when (this) {
    is Activity -> window
    is ContextWrapper -> baseContext.findWindow()
    else -> null
  }

/**
 * A helper class for setting the navigation and status bar colors for a [View], gracefully
 * degrading behavior based upon API level.
 *
 * Typically you would use [rememberSystemBarColorController] to remember an instance of this.
 */
internal class AndroidSystemBarColorController(
  private val view: View,
  private val window: Window?
) : SystemBarColorController {
  private val windowInsetsController = window?.let { WindowCompat.getInsetsController(it, view) }

  override fun setStatusBarColor(
    color: Color,
    darkIcons: Boolean,
    transformColorForLightContent: (Color) -> Color
  ) {
    statusBarDarkContentEnabled = darkIcons

    window?.statusBarColor =
      when {
        darkIcons && windowInsetsController?.isAppearanceLightStatusBars != true -> {
          // If we're set to use dark icons, but our windowInsetsController call didn't
          // succeed (usually due to API level), we instead transform the color to maintain
          // contrast
          transformColorForLightContent(color)
        }
        else -> color
      }.toArgb()
  }

  override fun setNavigationBarColor(
    color: Color,
    darkIcons: Boolean,
    navigationBarContrastEnforced: Boolean,
    transformColorForLightContent: (Color) -> Color
  ) {
    navigationBarDarkContentEnabled = darkIcons
    isNavigationBarContrastEnforced = navigationBarContrastEnforced

    window?.navigationBarColor =
      when {
        darkIcons && windowInsetsController?.isAppearanceLightNavigationBars != true -> {
          // If we're set to use dark icons, but our windowInsetsController call didn't
          // succeed (usually due to API level), we instead transform the color to maintain
          // contrast
          transformColorForLightContent(color)
        }
        else -> color
      }.toArgb()
  }

  override var statusBarDarkContentEnabled: Boolean
    get() = windowInsetsController?.isAppearanceLightStatusBars == true
    set(value) {
      windowInsetsController?.isAppearanceLightStatusBars = value
    }

  override var navigationBarDarkContentEnabled: Boolean
    get() = windowInsetsController?.isAppearanceLightNavigationBars == true
    set(value) {
      windowInsetsController?.isAppearanceLightNavigationBars = value
    }

  override var isNavigationBarContrastEnforced: Boolean
    get() = window?.isNavigationBarContrastEnforced == true
    set(value) {
      window?.isNavigationBarContrastEnforced = value
    }
}

@Suppress("MagicNumber") // This is a constant, detekt doesn't realize it
private val BlackScrim = Color(0f, 0f, 0f, 0.3f) // 30% opaque black
private val BlackScrimmed: (Color) -> Color = { original -> BlackScrim.compositeOver(original) }
