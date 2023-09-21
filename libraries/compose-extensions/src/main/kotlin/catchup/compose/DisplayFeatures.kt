package catchup.compose

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.window.layout.DisplayFeature

val LocalDisplayFeatures = staticCompositionLocalOf<List<DisplayFeature>> { emptyList() }
