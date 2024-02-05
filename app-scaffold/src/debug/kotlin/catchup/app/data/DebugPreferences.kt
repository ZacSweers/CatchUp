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
package catchup.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import catchup.app.BasePreferences
import catchup.app.CatchUpPreferences
import catchup.app.CatchUpPreferencesImpl
import catchup.app.util.BackgroundAppCoroutineScope
import catchup.di.AppScope
import catchup.di.SingleIn
import catchup.util.injection.qualifiers.ApplicationContext
import com.squareup.anvil.annotations.ContributesBinding
import javax.inject.Inject

/**
 * Debug build preferences. This is a superset of [CatchUpPreferences] and uses the same underlying
 * store.
 */
@SingleIn(AppScope::class)
@ContributesBinding(
  AppScope::class,
  boundType = CatchUpPreferences::class,
  replaces = [CatchUpPreferencesImpl::class],
)
class DebugPreferences
private constructor(
  private val catchUpPreferencesImpl: CatchUpPreferencesImpl,
  override val scope: BackgroundAppCoroutineScope,
) : CatchUpPreferences by catchUpPreferencesImpl, BasePreferences by catchUpPreferencesImpl {

  @Inject
  constructor(
    @ApplicationContext context: Context,
    scope: BackgroundAppCoroutineScope,
  ) : this(CatchUpPreferencesImpl(context, scope), scope)

  override val datastore: DataStore<Preferences> = catchUpPreferencesImpl.datastore

  val animationSpeed = preferenceStateFlow(Keys.animationSpeed, 1)
  val networkDelay = preferenceStateFlow(Keys.networkDelay, 2000L)
  val networkFailurePercent = preferenceStateFlow(Keys.networkFailurePercent, 3)
  val networkVariancePercent = preferenceStateFlow(Keys.networkVariancePercent, 40)
  val seenDebugDrawer = preferenceStateFlow(Keys.seenDebugDrawer, false)

  object Keys {
    var animationSpeed = intPreferencesKey("debug_animation_speed")
    var networkDelay = longPreferencesKey("debug_network_delay")
    var networkFailurePercent = intPreferencesKey("debug_network_failure_percent")
    var networkVariancePercent = intPreferencesKey("debug_network_variance_percent")
    var seenDebugDrawer = booleanPreferencesKey("debug_seen_debug_drawer")
  }
}
