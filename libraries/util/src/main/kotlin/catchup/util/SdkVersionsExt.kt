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
package catchup.util

import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import catchup.appconfig.AppConfig
import catchup.appconfig.EmptyAppConfig
import catchup.appconfig.sdk

@PublishedApi
internal val BUILD_APP_CONFIG =
  object : EmptyAppConfig {
    @Suppress("AnnotateVersionCheck") // False positive
    override val sdkInt: Int = Build.VERSION.SDK_INT
  }

@Deprecated("This should only be used when AppConfig is not available")
inline fun <T> Context.sdk(level: Int, func: () -> T): T? {
  // Try to dig one out of services
  val config = getSystemService<AppConfig>() ?: BUILD_APP_CONFIG
  return config.sdk(level, func)
}
