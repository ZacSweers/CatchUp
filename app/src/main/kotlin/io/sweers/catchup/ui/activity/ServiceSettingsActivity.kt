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
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.transaction
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import dagger.multibindings.Multibinds
import io.sweers.catchup.P
import io.sweers.catchup.R
import io.sweers.catchup.injection.scopes.PerActivity
import io.sweers.catchup.injection.scopes.PerFragment
import io.sweers.catchup.service.api.ServiceConfiguration.ActivityConfiguration
import io.sweers.catchup.service.api.ServiceConfiguration.PreferencesConfiguration
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.serviceregistry.ResolvedCatchUpServiceMetaRegistry
import io.sweers.catchup.ui.base.InjectingBaseActivity
import io.sweers.catchup.util.asDayContext
import io.sweers.catchup.util.isInNightMode
import io.sweers.catchup.util.setLightStatusBar
import kotterknife.bindView
import javax.inject.Inject

private const val TARGET_PREF_RESOURCE = "catchup.servicesettings.resource"

class ServiceSettingsActivity : InjectingBaseActivity() {

  private val toolbar by bindView<Toolbar>(R.id.toolbar)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val viewGroup = viewContainer.forActivity(this)
    layoutInflater.inflate(R.layout.activity_settings, viewGroup)
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    if (!isInNightMode()) {
      toolbar.setLightStatusBar()
    }

    if (savedInstanceState == null) {
      supportFragmentManager.transaction {
        add(R.id.container, ServiceSettingsFrag().apply {
          if (intent.extras?.containsKey(TARGET_PREF_RESOURCE) == true) {
            arguments = bundleOf(
                TARGET_PREF_RESOURCE to intent.extras!!.getInt(TARGET_PREF_RESOURCE))
          }
        })
      }
    }
  }

  @Module
  abstract class ServiceSettingsActivityModule {
    @Binds
    @PerActivity
    abstract fun provideActivity(activity: ServiceSettingsActivity): Activity
  }

  class ServiceSettingsFrag : PreferenceFragmentCompat() {

    @Inject
    lateinit var serviceMetas: Map<String, @JvmSuppressWildcards ServiceMeta>

    @Inject
    lateinit var sharedPrefs: SharedPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      AndroidSupportInjection.inject(this)
      // Replace backing sharedPreferences with ours
      preferenceManager.apply {
        sharedPreferencesName = "catchup"
        sharedPreferencesMode = Context.MODE_PRIVATE
      }

      val args = arguments
      if (args?.containsKey(TARGET_PREF_RESOURCE) == true) {
        addPreferencesFromResource(args.getInt(TARGET_PREF_RESOURCE))
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
              val metaColor = ContextCompat.getColor(activity!!.asDayContext(), meta.themeColor)
              val category = PreferenceCategory(activity).apply {
                title = resources.getString(meta.name)
//                titleColor = metaColor
              }
              preferenceScreen.addPreference(category)

              // Create an "enabled" pref
              val enabledPref = SwitchPreference(activity).apply {
                title = resources.getString(R.string.enabled)
                key = meta.enabledPreferenceKey
//                themeColor = metaColor
                setDefaultValue(true)
              }
              category.addPreference(enabledPref)

              // If there's a custom config, point to it
              meta.serviceConfiguration?.let { config ->
                when (config) {
                  is ActivityConfiguration -> {
                    category.addPreference(Preference(activity).apply {
                      dependency = meta.enabledPreferenceKey
                      setOnPreferenceClickListener {
                        startActivity(Intent(activity, config.activity))
                        true
                      }
                    })
                  }
                  is PreferencesConfiguration -> {
                    category.addPreference(Preference(activity).apply {
                      dependency = meta.enabledPreferenceKey
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
