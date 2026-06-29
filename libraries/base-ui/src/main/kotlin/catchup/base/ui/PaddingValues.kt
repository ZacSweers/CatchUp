/*
 * Copyright (C) 2026. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
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
