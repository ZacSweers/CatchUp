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
package io.sweers.catchup.app

import android.content.Context
import android.os.Build
import dev.zacsweers.catchup.appconfig.AppConfig
import dev.zacsweers.catchup.appconfig.AppConfigMetadataContributor
import io.sweers.catchup.base.ui.versionInfo
import io.sweers.catchup.util.injection.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatchUpAppConfig @Inject constructor(
  @ApplicationContext appContext: Context,
  metadataContributors: Set<@JvmSuppressWildcards AppConfigMetadataContributor>
) : AppConfig {
  private val versionInfo = appContext.versionInfo
  override val isDebug: Boolean = io.sweers.catchup.BuildConfig.DEBUG
  override val applicationId: String = io.sweers.catchup.BuildConfig.APPLICATION_ID
  override val buildType: String = io.sweers.catchup.BuildConfig.BUILD_TYPE
  override val flavor: String = io.sweers.catchup.BuildConfig.FLAVOR
  override val versionCode: Long = versionInfo.code
  override val versionName: String = versionInfo.name
  override val timestamp: String = versionInfo.timestamp
  override val sdkInt: Int = Build.VERSION.SDK_INT
  override val metadata: Map<Any, Any?> = mutableMapOf<Any, Any?>()
      .apply { metadataContributors.forEach { putAll(it.data()) } }
      .toMap()
}
