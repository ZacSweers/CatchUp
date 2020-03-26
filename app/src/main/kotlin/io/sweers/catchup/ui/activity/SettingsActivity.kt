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

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.commitNow
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.children
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.byteunits.BinaryByteUnit
import com.uber.autodispose.autoDispose
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import dev.zacsweers.catchup.appconfig.AppConfig
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.sweers.catchup.CatchUpPreferences
import io.sweers.catchup.R
import io.sweers.catchup.base.ui.BaseActivity
import io.sweers.catchup.base.ui.InjectingBaseActivity
import io.sweers.catchup.base.ui.updateNavBarColor
import io.sweers.catchup.data.CatchUpDatabase
import io.sweers.catchup.data.LumberYard
import io.sweers.catchup.databinding.ActivitySettingsBinding
import io.sweers.catchup.injection.ActivityModule
import io.sweers.catchup.injection.scopes.PerFragment
import io.sweers.catchup.ui.about.AboutActivity
import io.sweers.catchup.util.clearCache
import io.sweers.catchup.util.isInNightMode
import io.sweers.catchup.util.restartApp
import io.sweers.catchup.util.setLightStatusBar
import io.sweers.catchup.util.updateNightMode
import okhttp3.Cache
import java.io.File
import javax.inject.Inject

class SettingsActivity : InjectingBaseActivity() {

  companion object {
    const val SETTINGS_RESULT_DATA = 100
    const val NIGHT_MODE_UPDATED = "nightModeUpdated"
    const val NAV_COLOR_UPDATED = "navColorUpdated"
    const val SERVICE_ORDER_UPDATED = "serviceOrderUpdated"
    const val ARG_FROM_RECREATE = "fromRecreate"
  }

  /**
   * Backpress hijacks activity result codes, so store ours here in case
   */
  private val resultData = Bundle()

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
        add(R.id.container, SettingsFrag())
      }
    } else if (savedInstanceState.getBoolean(ARG_FROM_RECREATE, false)) {
      resultData.putBoolean(NIGHT_MODE_UPDATED, true)
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      if (!resultData.isEmpty) {
        setResult(SETTINGS_RESULT_DATA, Intent().putExtras(resultData))
      }
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(ARG_FROM_RECREATE, true)
  }

  override fun onBackPressed() {
    if (!resultData.isEmpty) {
      setResult(SETTINGS_RESULT_DATA, Intent().putExtras(resultData))
    }
    super.onBackPressed()
  }

  @Module
  abstract class SettingsActivityModule : ActivityModule<SettingsActivity>

  class SettingsFrag : PreferenceFragmentCompat() {

    @Inject
    lateinit var cache: dagger.Lazy<Cache>
    @Inject
    lateinit var database: CatchUpDatabase
    @Inject
    lateinit var lumberYard: LumberYard
    @Inject
    lateinit var sharedPreferences: SharedPreferences
    @Inject
    lateinit var catchUpPreferences: CatchUpPreferences
    @Inject
    lateinit var appConfig: AppConfig

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      AndroidSupportInjection.inject(this)
      addPreferencesFromResource(R.xml.prefs_general)

      // Because why on earth is the default true
      // Note categories don't work yet due to https://issuetracker.google.com/issues/111662669
      preferenceScreen.allChildren.forEach {
        it.isIconSpaceReserved = false
      }

      (findPreference(catchUpPreferences::smartlinkingGlobal.name) as? CheckBoxPreference)?.isChecked = catchUpPreferences.smartlinkingGlobal
      (findPreference(catchUpPreferences::daynightAuto.name) as? CheckBoxPreference)?.isChecked = catchUpPreferences.daynightAuto
      (findPreference(catchUpPreferences::dayNightForceNight.name) as? CheckBoxPreference)?.isChecked = catchUpPreferences.dayNightForceNight
      (findPreference(catchUpPreferences::reports.name) as? CheckBoxPreference)?.isChecked = catchUpPreferences.reports

      val themeNavBarPref = findPreference(catchUpPreferences::themeNavigationBar.name) as? CheckBoxPreference
      themeNavBarPref?.isChecked = catchUpPreferences.themeNavigationBar
    }

    @SuppressLint("SdCardPath") // False positive
    override fun onPreferenceTreeClick(preference: Preference): Boolean {
      when (preference.key) {
        catchUpPreferences::smartlinkingGlobal.name -> {
          catchUpPreferences.smartlinkingGlobal = (preference as CheckBoxPreference).isChecked
          return true
        }
        catchUpPreferences::daynightAuto.name -> {
          val isChecked = (preference as CheckBoxPreference).isChecked
          catchUpPreferences.daynightAuto = isChecked
              .apply {
                if (isChecked) {
                  // If we're enabling auto, clear out the prev daynight night-only mode
                  catchUpPreferences.dayNightForceNight = false
                  (findPreference(catchUpPreferences::dayNightForceNight.name) as? CheckBoxPreference)?.isChecked = false
                }
              }
          activity?.updateNightMode(catchUpPreferences)
          return true
        }
        catchUpPreferences::dayNightForceNight.name -> {
          catchUpPreferences.dayNightForceNight = (preference as CheckBoxPreference).isChecked
          activity?.updateNightMode(catchUpPreferences)
          return true
        }
        catchUpPreferences::themeNavigationBar.name -> {
          catchUpPreferences.themeNavigationBar = (preference as CheckBoxPreference).isChecked
          (activity as SettingsActivity).run {
            resultData.putBoolean(NAV_COLOR_UPDATED, true)
            updateNavBarColor(recreate = true, uiPreferences = catchUpPreferences, appConfig = appConfig)
          }
          return true
        }
        CatchUpPreferences.SECTION_KEY_ORDER_SERVICES -> {
          (activity as SettingsActivity).resultData.putBoolean(SERVICE_ORDER_UPDATED, true)
          startActivity(Intent(activity, OrderServicesActivity::class.java))
          return true
        }
        catchUpPreferences::reports.name -> {
          val isChecked = (preference as CheckBoxPreference).isChecked
          catchUpPreferences.reports = isChecked
          Snackbar.make(requireView(), R.string.settings_reset, Snackbar.LENGTH_LONG)
              .setAction(R.string.restart) {
                it.context.restartApp()
              }
              .show()
          return true
        }
        CatchUpPreferences.ITEM_KEY_CLEAR_CACHE -> {
          Single.fromCallable {
            val appContext = activity!!.applicationContext
            var cleanedSize = 0L
            val prefsRoot = File("/data/data/${appConfig.applicationId}/shared_prefs")
            if (prefsRoot.isDirectory) {
              for (prefFile in prefsRoot.listFiles()!!) {
                val fileSize = prefFile.length()
                if (prefFile.delete()) {
                  cleanedSize += fileSize
                }
              }
            }
            for (file in appContext.filesDir.walkBottomUp()) {
              val fileSize = file.length()
              if (file.delete()) {
                cleanedSize += fileSize
              }
            }
            cleanedSize += appContext.clearCache()
            cleanedSize += with(cache.get()) {
              val initialSize = size()
              evictAll()
              return@with initialSize - size()
            }
            for (dbName in appContext.databaseList()) {
              val dbFile = appContext.getDatabasePath(dbName)
              val initialDbSize = dbFile.length()
              if (appContext.deleteDatabase(dbName)) {
                cleanedSize += initialDbSize
              }
            }
            cleanedSize += lumberYard.cleanUp()
            return@fromCallable cleanedSize
          }.subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .autoDispose(activity as BaseActivity)
              .subscribe { cleanedAmount, throwable ->
                val message = throwable?.let {
                  getString(R.string.settings_error_cleaning_cache)
                } ?: getString(R.string.clear_cache_success, BinaryByteUnit.format(cleanedAmount))
                Snackbar.make(requireView(), message, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.restart) {
                      it.context.restartApp()
                    }
                    .show()
              }
          return true
        }
        CatchUpPreferences.ITEM_KEY_ABOUT -> {
          startActivity(Intent(activity, AboutActivity::class.java))
          return true
        }
        CatchUpPreferences.SECTION_KEY_SERVICES -> {
          (activity as SettingsActivity).resultData.putBoolean(SERVICE_ORDER_UPDATED, true)
          startActivity(Intent(activity, ServiceSettingsActivity::class.java))
          return true
        }
      }

      return super.onPreferenceTreeClick(preference)
    }

    @Module
    abstract class SettingsFragmentBindingModule {

      @PerFragment
      @ContributesAndroidInjector
      internal abstract fun settingsFragment(): SettingsFrag
    }
  }
}

/**
 * Recursively gets all children in a preference (group).
 */
val Preference.allChildren: Sequence<Preference>
  get() {
    return if (this is PreferenceGroup) {
      children + children.flatMap(Preference::allChildren)
    } else {
      sequenceOf(this)
    }
  }
