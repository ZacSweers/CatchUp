package io.sweers.catchup.service.api

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalServiceThemeColor = compositionLocalOf<Color> { error("No theme color provided!") }
