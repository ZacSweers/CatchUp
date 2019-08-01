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
package io.sweers.catchup.ui.base

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import dagger.android.AndroidInjection
import io.sweers.catchup.app.CatchUpObjectWatcher
import javax.inject.Inject

abstract class InjectableBaseActivity : BaseActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    AndroidInjection.inject(this)
    super.onCreate(savedInstanceState)
  }

  @Inject
  internal fun watchForLeaks(objectWatcher: CatchUpObjectWatcher) {
    val callbacks = object : FragmentLifecycleCallbacks() {
      override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
        objectWatcher.watch(f)
      }
    }
    supportFragmentManager.registerFragmentLifecycleCallbacks(callbacks, true)
  }
}
