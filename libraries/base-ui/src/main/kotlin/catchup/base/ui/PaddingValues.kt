package catchup.base.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.LayoutDirection

operator fun PaddingValues.plus(plus: PaddingValues): PaddingValues =
  PaddingValues(
    start =
      calculateStartPadding(LayoutDirection.Ltr) + plus.calculateStartPadding(LayoutDirection.Ltr),
    top = calculateTopPadding() + plus.calculateTopPadding(),
    end = calculateEndPadding(LayoutDirection.Ltr) + plus.calculateEndPadding(LayoutDirection.Ltr),
    bottom = calculateBottomPadding() + plus.calculateBottomPadding(),
  )
