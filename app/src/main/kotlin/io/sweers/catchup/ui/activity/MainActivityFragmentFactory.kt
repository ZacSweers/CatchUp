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
package io.sweers.catchup.ui.activity

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import io.sweers.catchup.injection.scopes.PerActivity
import javax.inject.Inject
import javax.inject.Provider

@PerActivity
class MainActivityFragmentFactory @Inject constructor(
  private val providers: Map<Class<out Fragment>, @JvmSuppressWildcards Provider<Fragment>>
) : FragmentFactory() {
  override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
    val fragmentClass = classLoader.loadClass(className)
    return providers[fragmentClass]?.get() ?: super.instantiate(classLoader, className)
  }
}
