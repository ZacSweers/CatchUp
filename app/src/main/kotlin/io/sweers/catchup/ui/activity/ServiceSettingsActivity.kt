/*
 * Copyright (c) 2018 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.ui.activity

import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceCategory
import android.preference.PreferenceFragment
import android.preference.SwitchPreference
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import butterknife.BindView
import dagger.Binds
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.ContributesAndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasFragmentInjector
import dagger.multibindings.Multibinds
import io.sweers.catchup.P
import io.sweers.catchup.R
import io.sweers.catchup.injection.scopes.PerActivity
import io.sweers.catchup.injection.scopes.PerFragment
import io.sweers.catchup.service.api.ServiceConfiguration.ActivityConfiguration
import io.sweers.catchup.service.api.ServiceConfiguration.PreferencesConfiguration
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.serviceregistry.ResolvedCatchUpServiceMetaRegistry
import io.sweers.catchup.ui.base.BaseActivity
import io.sweers.catchup.util.asDayContext
import io.sweers.catchup.util.isInNightMode
import io.sweers.catchup.util.setLightStatusBar
import javax.inject.Inject

private const val TARGET_PREF_RESOURCE = "catchup.servicesettings.resource"

class ServiceSettingsActivity : BaseActivity(), HasFragmentInjector {

  @Inject
  internal lateinit var dispatchingFragmentInjector: DispatchingAndroidInjector<Fragment>
  @BindView(R.id.toolbar)
  internal lateinit var toolbar: Toolbar

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val viewGroup = viewContainer.forActivity(this)
    layoutInflater.inflate(R.layout.activity_settings, viewGroup)
    ServiceSettingsActivity_ViewBinding(this)
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    if (!isInNightMode()) {
      toolbar.setLightStatusBar()
    }

    if (savedInstanceState == null) {
      fragmentManager.beginTransaction()
          .add(R.id.container, ServiceSettingsFrag().apply {
            if (intent.extras?.containsKey(TARGET_PREF_RESOURCE) == true) {
              arguments = bundleOf(
                  TARGET_PREF_RESOURCE to intent.extras.getInt(TARGET_PREF_RESOURCE))
            }
          })
          .commit()
    }
  }

  override fun fragmentInjector(): AndroidInjector<Fragment> = dispatchingFragmentInjector

  @Module
  abstract class ServiceSettingsActivityModule {
    @Binds
    @PerActivity
    abstract fun provideActivity(activity: ServiceSettingsActivity): Activity
  }

  class ServiceSettingsFrag : PreferenceFragment() {

    @Inject
    lateinit var serviceMetas: Map<String, @JvmSuppressWildcards ServiceMeta>

    @Inject
    lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
      AndroidInjection.inject(this)
      super.onCreate(savedInstanceState)
      // Replace backing sharedPreferences with ours
      preferenceManager.apply {
        sharedPreferencesName = "catchup"
        sharedPreferencesMode = Context.MODE_PRIVATE
      }

      if (arguments?.containsKey(TARGET_PREF_RESOURCE) == true) {
        addPreferencesFromResource(arguments.getInt(TARGET_PREF_RESOURCE))
      } else {
        setUpGeneralSettings()
      }
    }

    private fun setUpGeneralSettings() {
      preferenceScreen = preferenceManager.createPreferenceScreen(activity)

      val currentOrder = sharedPrefs.getString(P.ServicesOrder.KEY, null)?.split(",")
          ?: emptyList()
      serviceMetas
          .values
          .asSequence()
          .sortedBy { currentOrder.indexOf(it.id) }
          .forEach { meta ->
            meta.run {
              // Create a category
              val metaColor = ContextCompat.getColor(activity.asDayContext(), meta.themeColor)
              val category = PreferenceCategory(activity).apply {
                title = resources.getString(meta.name)
//                titleColor = metaColor
              }
              preferenceScreen.addPreference(category)

              // Create an "enabled" pref
              val enabledPref = SwitchPreference(activity).apply {
                title = resources.getString(R.string.enabled)
                key = meta.enabledKey
//                themeColor = metaColor
                setDefaultValue(true)
              }
              category.addPreference(enabledPref)

              // If there's a custom config, point to it
              meta.serviceConfiguration?.let { config ->
                when (config) {
                  is ActivityConfiguration -> {
                    category.addPreference(Preference(activity).apply {
                      dependency = meta.enabledKey
                      setOnPreferenceClickListener {
                        startActivity(Intent(activity, config.activity))
                        true
                      }
                    })
                  }
                  is PreferencesConfiguration -> {
                    category.addPreference(Preference(activity).apply {
                      dependency = meta.enabledKey
                      setOnPreferenceClickListener {
                        startActivity(Intent(activity, ServiceSettingsActivity::class.java).apply {
                          putExtra(TARGET_PREF_RESOURCE, config.preferenceResource)
                        })
                        true
                      }
                    })
                  }
                }
              }
            }
          }
    }

    @Module(includes = [ResolvedCatchUpServiceMetaRegistry::class])
    abstract class ServiceSettingsModule {

      @Multibinds
      abstract fun serviceMetas(): Map<String, ServiceMeta>

      @PerFragment
      @ContributesAndroidInjector
      internal abstract fun serviceSettingsFragment(): ServiceSettingsFrag
    }
  }
}
