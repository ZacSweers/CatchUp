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
package io.sweers.catchup.base.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import dev.zacsweers.catchup.appconfig.AppConfig
import javax.inject.Inject

abstract class InjectableBaseActivity : BaseActivity() {

  companion object {
    private const val APP_CONFIG_SERVICE_NAME = "catchup.service.appconfig"
  }

  @Inject lateinit var viewContainer: ViewContainer
  @Inject lateinit var uiPreferences: UiPreferences
  @Inject override lateinit var appConfig: AppConfig
  @Inject lateinit var fragmentFactory: FragmentFactory
  var objectWatcher: CatchUpObjectWatcher? = null
    @Inject
    set(value) {
      field = value
      val callbacks =
        object : FragmentLifecycleCallbacks() {
          override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
            value!!.watch(f)
          }
        }
      supportFragmentManager.registerFragmentLifecycleCallbacks(callbacks, true)
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    supportFragmentManager.fragmentFactory = fragmentFactory
  }

  override fun getSystemServiceName(serviceClass: Class<*>): String? {
    if (serviceClass == AppConfig::class.java) {
      return APP_CONFIG_SERVICE_NAME
    }
    return super.getSystemServiceName(serviceClass)
  }

  override fun getSystemService(name: String): Any? {
    if (name == APP_CONFIG_SERVICE_NAME) {
      return appConfig
    }
    return super.getSystemService(name)
  }

  /**
   * Exists as a hook to allow subclasses to inject a FragmentFactory before super.onCreate() is
   * called.
   */
  open fun setFragmentFactory() {}

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    updateNavBarColor(uiPreferences = uiPreferences, appConfig = appConfig)
  }
}
