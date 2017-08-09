/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import android.support.design.widget.Snackbar
import android.support.v7.widget.Toolbar
import android.widget.Toast
import butterknife.BindView
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.uber.autodispose.kotlin.autoDisposeWith
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.ContributesAndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasFragmentInjector
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.sweers.catchup.P
import io.sweers.catchup.R
import io.sweers.catchup.data.CatchUpDatabase
import io.sweers.catchup.data.RemoteConfigKeys
import io.sweers.catchup.injection.scopes.PerFragment
import io.sweers.catchup.ui.base.BaseActivity
import io.sweers.catchup.util.UiUtil
import io.sweers.catchup.util.clearCache
import io.sweers.catchup.util.format
import io.sweers.catchup.util.isInNightMode
import io.sweers.catchup.util.setLightStatusBar
import java.io.File
import javax.inject.Inject


class SettingsActivity : BaseActivity(), HasFragmentInjector {

  companion object {
    const val NIGHT_MODE_UPDATED = 100
    const val ARG_FROM_RECREATE = "fromRecreate"
  }

  @Inject lateinit internal var dispatchingFragmentInjector: DispatchingAndroidInjector<Fragment>
  @BindView(R.id.toolbar) lateinit var toolbar: Toolbar

  /**
   * Backpress hijacks activity result codes, so store ours here in case
   */
  private var _resultMirror: Int = Activity.RESULT_CANCELED
  private var resultMirror: Int
    set(value) {
      setResult(value)
      _resultMirror = value
    }
    get() = _resultMirror

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_settings)
    SettingsActivity_ViewBinding(this)
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    if (!isInNightMode()) {
      toolbar.setLightStatusBar()
    }

    if (savedInstanceState == null) {
      fragmentManager.beginTransaction()
          .add(R.id.container, SettingsFrag())
          .commit()
    } else if (savedInstanceState.getBoolean(ARG_FROM_RECREATE, false)) {
      resultMirror = NIGHT_MODE_UPDATED
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(ARG_FROM_RECREATE, true)
  }

  override fun onBackPressed() {
    setResult(resultMirror)
    super.onBackPressed()
  }

  override fun fragmentInjector(): AndroidInjector<Fragment> = dispatchingFragmentInjector

  class SettingsFrag : PreferenceFragment() {

    @Inject lateinit var remoteConfig: FirebaseRemoteConfig
    @Inject lateinit var database: CatchUpDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
      AndroidInjection.inject(this)
      super.onCreate(savedInstanceState)
      addPreferencesFromResource(R.xml.prefs_general)

      (findPreference(
          P.SmartlinkingGlobal.KEY) as CheckBoxPreference).isChecked = P.SmartlinkingGlobal.get()
      (findPreference(P.DaynightAuto.KEY) as CheckBoxPreference).isChecked = P.DaynightAuto.get()
      (findPreference(P.DaynightNight.KEY) as CheckBoxPreference).isChecked = P.DaynightNight.get()
      (findPreference(P.reports.KEY) as CheckBoxPreference).isChecked = P.reports.get()

      val themeNavBarPref = findPreference(P.ThemeNavigationBar.KEY) as CheckBoxPreference
      if (remoteConfig.getBoolean(RemoteConfigKeys.THEME_NAV_BAR_ENABLED)) {
        themeNavBarPref.isChecked = P.ThemeNavigationBar.get()
      } else {
        preferenceScreen.removePreference(themeNavBarPref)
      }
    }

    override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen,
        preference: Preference): Boolean {
      when (preference.key) {
        P.SmartlinkingGlobal.KEY -> {
          P.SmartlinkingGlobal.put((preference as CheckBoxPreference).isChecked).apply()
          return true
        }
        P.DaynightAuto.KEY -> {
          val isChecked = (preference as CheckBoxPreference).isChecked
          P.DaynightAuto.put(isChecked)
              .apply {
                if (isChecked) {
                  // If we're enabling auto, clear out the prev daynight night-only mode
                  putBoolean(P.DaynightNight.KEY, false)
                  (findPreference(P.DaynightNight.KEY) as CheckBoxPreference).isChecked = false
                }
              }
              .apply()
          UiUtil.updateNightMode(activity)
          return true
        }
        P.DaynightNight.KEY -> {
          P.DaynightNight.put((preference as CheckBoxPreference).isChecked).apply()
          UiUtil.updateNightMode(activity)
          return true
        }
        P.ThemeNavigationBar.KEY -> {
          P.ThemeNavigationBar.put((preference as CheckBoxPreference).isChecked).apply()
          return true
        }
        P.reports.KEY -> {
          val isChecked = (preference as CheckBoxPreference).isChecked
          P.reports.put(isChecked).apply()
          Snackbar.make(view, "Will take effect on next app restart", Snackbar.LENGTH_SHORT)
              .setAction("Undo") {
                P.reports.put(!isChecked).apply()
                preference.isChecked = !isChecked
              }
              .show()
          return true
        }
        P.ClearCache.KEY -> {
          Single.fromCallable {
            val cacheCleaned = activity.applicationContext.clearCache()
            val dbFile = File(database.openHelper.readableDatabase.path)
            val initialDbSize = dbFile.length()
            with(database.serviceDao()) {
              nukeItems()
              nukePages()
            }
            with(database.smmryDao()) {
              nukeItems()
            }
            val deletedFromDb = initialDbSize - dbFile.length()
            return@fromCallable cacheCleaned + deletedFromDb
          }.subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .autoDisposeWith(activity as BaseActivity)
              .subscribe { cleanedAmount, throwable ->
                val errorMessage = throwable?.let {
                  "There was an error cleaning cache"
                } ?: getString(R.string.clear_cache_success, cleanedAmount.format())
                Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT).show()
              }
          return true
        }
        P.licenses.KEY -> {
          Toast.makeText(activity, "TODO", Toast.LENGTH_SHORT).show()
          return true
        }
        P.about.KEY -> {
          Toast.makeText(activity, "TODO", Toast.LENGTH_SHORT).show()
          return true
        }
      }

      return super.onPreferenceTreeClick(preferenceScreen, preference)
    }

    @Module
    abstract class SettingsFragmentBindingModule {

      @PerFragment
      @ContributesAndroidInjector
      internal abstract fun settingsFragment(): SettingsFrag
    }
  }
}
