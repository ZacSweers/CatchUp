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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.multibindings.Multibinds
import dev.zacsweers.catchup.di.AppScope
import dev.zacsweers.catchup.di.android.ActivityKey
import dev.zacsweers.catchup.di.android.FragmentKey
import io.sweers.catchup.CatchUpPreferences
import io.sweers.catchup.R
import io.sweers.catchup.base.ui.InjectingBaseActivity
import io.sweers.catchup.databinding.ActivitySettingsBinding
import io.sweers.catchup.service.api.ServiceConfiguration.ActivityConfiguration
import io.sweers.catchup.service.api.ServiceConfiguration.PreferencesConfiguration
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.util.asDayContext
import io.sweers.catchup.util.isInNightMode
import io.sweers.catchup.util.setLightStatusBar
import javax.inject.Inject
import javax.inject.Provider

private const val TARGET_PREF_RESOURCE = "catchup.servicesettings.resource"

@ActivityKey(ServiceSettingsActivity::class)
@ContributesMultibinding(AppScope::class, boundType = Activity::class)
class ServiceSettingsActivity
@Inject
constructor(private val serviceSettingsFragProvider: Provider<ServiceSettingsFrag>) :
  InjectingBaseActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val binding = viewContainer.inflateBinding(ActivitySettingsBinding::inflate)
    setSupportActionBar(binding.toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    if (!isInNightMode()) {
      binding.toolbar.setLightStatusBar(appConfig)
    }

    if (savedInstanceState == null) {
      supportFragmentManager.commitNow {
        add(
          R.id.container,
          serviceSettingsFragProvider.get().apply {
            if (intent.extras?.containsKey(TARGET_PREF_RESOURCE) == true) {
              arguments =
                bundleOf(TARGET_PREF_RESOURCE to intent.extras!!.getInt(TARGET_PREF_RESOURCE))
            }
          }
        )
      }
    }
  }

  @FragmentKey(ServiceSettingsFrag::class)
  @ContributesMultibinding(AppScope::class, boundType = Fragment::class)
  class ServiceSettingsFrag
  @Inject
  constructor(
    private val serviceMetas: Map<String, ServiceMeta>,
    private val catchUpPreferences: CatchUpPreferences,
  ) : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
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
      preferenceScreen = preferenceManager.createPreferenceScreen(requireActivity())

      //      val currentOrder = catchUpPreferences.servicesOrder?.split(",") ?: emptyList()
      val currentOrder = emptyList<String>()
      serviceMetas.values
        .asSequence()
        .sortedBy { currentOrder.indexOf(it.id) }
        .forEach { meta ->
          meta.run {
            // Create a category
            val metaColor =
              ContextCompat.getColor(requireActivity().asDayContext(), meta.themeColor)
            val category =
              PreferenceCategory(requireActivity()).apply {
                title = resources.getString(meta.name)
                //                titleColor = metaColor
                icon =
                  AppCompatResources.getDrawable(context, meta.icon)!!.apply { setTint(metaColor) }
              }
            preferenceScreen.addPreference(category)

            // Create an "enabled" pref
            val enabledPref =
              SwitchPreference(requireActivity()).apply {
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
                  category.addPreference(
                    Preference(requireActivity()).apply {
                      dependency = meta.enabledPreferenceKey
                      setOnPreferenceClickListener {
                        startActivity(Intent(activity, config.activity))
                        true
                      }
                    }
                  )
                }
                is PreferencesConfiguration -> {
                  category.addPreference(
                    Preference(requireActivity()).apply {
                      dependency = meta.enabledPreferenceKey
                      setOnPreferenceClickListener {
                        startActivity(
                          Intent(activity, ServiceSettingsActivity::class.java).apply {
                            putExtra(TARGET_PREF_RESOURCE, config.preferenceResource)
                          }
                        )
                        true
                      }
                    }
                  )
                }
              }
            }
          }
        }
    }

    @ContributesTo(AppScope::class)
    @Module
    abstract class ServiceSettingsModule {

      @Multibinds abstract fun serviceMetas(): Map<String, ServiceMeta>
    }
  }
}
