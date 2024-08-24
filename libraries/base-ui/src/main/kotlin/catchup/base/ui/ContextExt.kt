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
package catchup.base.ui

import android.content.Context
import android.content.pm.PackageManager
import catchup.util.sdk

// BuildConfig.VERSION_NAME/CODE is not reliable here because we replace this dynamically in the
// application manifest.
@Suppress("DEPRECATION")
val Context.versionInfo: VersionInfo
  get() {
    val metadataBundle =
      packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData
    val timestamp = metadataBundle.getString("buildTimestamp") ?: "Missing timestamp!"
    return with(packageManager.getPackageInfo(packageName, 0)) {
      VersionInfo(
        code = sdk(28) { longVersionCode } ?: versionCode.toLong(),
        name = versionName!!,
        timestamp = timestamp,
      )
    }
  }

data class VersionInfo(val code: Long, val name: String, val timestamp: String)
