/*
 * Copyright (C) 2019. Zac Sweers
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
package catchup.service.api

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

@Immutable
data class ServiceMeta(
  val id: String,
  @StringRes val name: Int,
  @ColorRes val themeColor: Int,
  @DrawableRes val icon: Int,
  val isVisual: Boolean = false,
  val firstPageKey: Int?,
  val pagesAreNumeric: Boolean = false,
  val enabled: Boolean = true,
) {
  val enabledPreferenceKey = "service_config_${id}_enabled"
}
