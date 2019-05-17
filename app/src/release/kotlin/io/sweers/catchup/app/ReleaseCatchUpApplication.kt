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

import com.bugsnag.android.Bugsnag
import io.sweers.catchup.BuildConfig
import timber.log.Timber

class ReleaseCatchUpApplication : CatchUpApplication() {

  override fun inject() {
    DaggerApplicationComponent.builder()
        .application(this)
        .build()
        .inject(this)
  }

  override fun initVariant() {
    CatchUpApplication.refWatcher = CatchUpRefWatcher.None
    Bugsnag.init(this, BuildConfig.BUGSNAG_KEY)

    BugsnagTree().also {
      Bugsnag.getClient()
          .beforeNotify { error ->
            it.update(error)
            true
          }

      Timber.plant(it)
    }
  }
}
