/*
 * Copyright (C) 2020. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
