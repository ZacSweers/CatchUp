package dev.zacsweers.catchup.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.text.font.fontFamily

@Composable
fun CatchUpTheme(
  content: @Composable () -> Unit
) {
  val colors = if (isSystemInDarkTheme()) {
    darkColors(
      primary = Color(0xff343434),
      primaryVariant = Color(0xffc10000),
      secondary = Color(0xffFC4D29),
      onPrimary = Color.White
    )
  } else {
    lightColors(
      primary = Color(0xfff9f9f9),
      primaryVariant = Color(0xff343434),
      secondary = Color(0xffFC4D29),
      secondaryVariant = Color(0xffc10000),
      onPrimary = Color.Black
    )
  }
  val fontFamily = fontFamily(fonts = listOf(ResourceFont(R.font.nunito)))
  val typography = Typography(defaultFontFamily = fontFamily)
  MaterialTheme(
    colors = colors,
    typography = typography,
    content = content
  )
}