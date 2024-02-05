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
package catchup.app

import android.content.Context
import android.os.Build
import catchup.app.injection.DaggerSet
import catchup.appconfig.AppConfig
import catchup.appconfig.AppConfigMetadataContributor
import catchup.base.ui.versionInfo
import catchup.di.AppScope
import catchup.di.SingleIn
import catchup.util.injection.qualifiers.ApplicationContext
import dev.zacsweers.catchup.app.scaffold.BuildConfig
import javax.inject.Inject

@SingleIn(AppScope::class)
class CatchUpAppConfig
@Inject
constructor(
  @ApplicationContext private val appContext: Context,
  metadataContributors: DaggerSet<AppConfigMetadataContributor>,
) : AppConfig {
  private val versionInfo = appContext.versionInfo
  override val isDebug: Boolean = BuildConfig.DEBUG
  override val applicationId: String
    get() = appContext.packageName

  override val buildType: String = BuildConfig.BUILD_TYPE
  override val versionCode: Long = versionInfo.code
  override val versionName: String = versionInfo.name
  override val timestamp: String = versionInfo.timestamp
  override val sdkInt: Int = Build.VERSION.SDK_INT
  override val metadata: Map<Any, Any?> =
    mutableMapOf<Any, Any?>().apply { metadataContributors.forEach { putAll(it.data()) } }.toMap()
}
