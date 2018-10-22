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
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.fragment.app.transaction
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.children
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.perf.FirebasePerformance
import com.uber.autodispose.autoDisposable
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.sweers.catchup.R
import io.sweers.catchup.data.CatchUpDatabase
import io.sweers.catchup.data.LumberYard
import io.sweers.catchup.injection.scopes.PerActivity
import io.sweers.catchup.injection.scopes.PerFragment
import io.sweers.catchup.preferences.PreferenceConstants.DAY_NIGHT_AUTO
import io.sweers.catchup.preferences.PreferenceConstants.DAY_NIGHT_NIGHT_ONLY
import io.sweers.catchup.preferences.PreferenceConstants.REPORTS_ENABLED
import io.sweers.catchup.preferences.PreferenceConstants.SMART_LINKING_GLOBAL_ENABLED
import io.sweers.catchup.preferences.PreferenceConstants.THEME_NAV_BAR
import io.sweers.catchup.ui.about.AboutActivity
import io.sweers.catchup.ui.base.BaseActivity
import io.sweers.catchup.ui.base.InjectingBaseActivity
import io.sweers.catchup.util.NavBarColorizer
import io.sweers.catchup.util.clearCache
import io.sweers.catchup.util.isInNightMode
import io.sweers.catchup.util.kotlin.format
import io.sweers.catchup.util.setLightStatusBar
import io.sweers.catchup.util.updateNightMode
import kotterknife.bindView
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

  private val toolbar by bindView<Toolbar>(R.id.toolbar)

  /**
   * Backpress hijacks activity result codes, so store ours here in case
   */
  private val resultData = Bundle()

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
  abstract class SettingsActivityModule {
    @Binds
    @PerActivity
    abstract fun provideActivity(activity: SettingsActivity): Activity
  }

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
    lateinit var rxSharedPreferences: RxSharedPreferences
    @Inject
    protected lateinit var navColorizer: NavBarColorizer

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      AndroidSupportInjection.inject(this)
      addPreferencesFromResource(R.xml.prefs_general)

      // Because why on earth is the default true
      // Note categories don't work yet due to https://issuetracker.google.com/issues/111662669
      preferenceScreen.allChildren.forEach {
        it.isIconSpaceReserved = false
      }

      (findPreference(
          SMART_LINKING_GLOBAL_ENABLED) as CheckBoxPreference).isChecked = sharedPreferences.getBoolean(
          SMART_LINKING_GLOBAL_ENABLED, true)
      (findPreference(
          DAY_NIGHT_AUTO) as CheckBoxPreference).isChecked = sharedPreferences.getBoolean(
          DAY_NIGHT_AUTO, true)
      (findPreference(
          DAY_NIGHT_NIGHT_ONLY) as CheckBoxPreference).isChecked = sharedPreferences.getBoolean(
          DAY_NIGHT_NIGHT_ONLY, false)
      (findPreference(
          REPORTS_ENABLED) as CheckBoxPreference).isChecked = sharedPreferences.getBoolean(
          REPORTS_ENABLED, true)

      val themeNavBarPref = findPreference(THEME_NAV_BAR) as CheckBoxPreference
      themeNavBarPref.isChecked = sharedPreferences.getBoolean(THEME_NAV_BAR, false)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
      when (preference.key) {
        SMART_LINKING_GLOBAL_ENABLED -> {
          sharedPreferences.edit {
            putBoolean(SMART_LINKING_GLOBAL_ENABLED, (preference as CheckBoxPreference).isChecked)
          }
          return true
        }
        DAY_NIGHT_AUTO -> {
          val isChecked = (preference as CheckBoxPreference).isChecked
          sharedPreferences.edit {
            putBoolean(DAY_NIGHT_AUTO, preference.isChecked).apply {
              if (isChecked) {
                // If we're enabling auto, clear out the prev daynight night-only mode
                putBoolean(DAY_NIGHT_NIGHT_ONLY, false)
                (findPreference(DAY_NIGHT_NIGHT_ONLY) as CheckBoxPreference).isChecked = false
              }
            }
          }
          activity?.updateNightMode(auto = isChecked)
          return true
        }
        DAY_NIGHT_NIGHT_ONLY -> {
          val isChecked = (preference as CheckBoxPreference).isChecked
          sharedPreferences.edit {
            putBoolean(DAY_NIGHT_NIGHT_ONLY, isChecked)
          }
          activity?.updateNightMode(enabled = isChecked)
          return true
        }
        THEME_NAV_BAR -> {
          val isChecked = (preference as CheckBoxPreference).isChecked
          sharedPreferences.edit {
            putBoolean(THEME_NAV_BAR, isChecked)
          }
          (activity as SettingsActivity).run {
            resultData.putBoolean(NAV_COLOR_UPDATED, true)
            this@SettingsFrag.navColorizer.refresh(recreate = true)
          }
          return true
        }
        "reorder_services_section" -> {
          (activity as SettingsActivity).resultData.putBoolean(SERVICE_ORDER_UPDATED, true)
          startActivity(Intent(activity, OrderServicesActivity::class.java))
          return true
        }
        REPORTS_ENABLED -> {
          val isChecked = (preference as CheckBoxPreference).isChecked
          FirebasePerformance.getInstance().isPerformanceCollectionEnabled = isChecked
          sharedPreferences.edit {
            putBoolean(REPORTS_ENABLED, isChecked)
          }
          Snackbar.make(view!!, R.string.settings_reset, Snackbar.LENGTH_SHORT)
              .setAction(R.string.undo) {
                // TODO Maybe this should actually be a restart button
                sharedPreferences.edit {
                  putBoolean(REPORTS_ENABLED, !isChecked)
                }
                preference.isChecked = !isChecked
              }
              .show()
          return true
        }
        "clear_cache" -> {
          Single.fromCallable {
            // TODO would be nice to measure the size impact of this file ¯\_(ツ)_/¯
            sharedPreferences.edit().clear().apply()
            val cacheCleaned = activity!!.applicationContext.clearCache()
            val networkCacheCleaned = with(cache.get()) {
              val initialSize = size()
              evictAll()
              return@with initialSize - size()
            }
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
            val clearedLogs = lumberYard.cleanUp()
            return@fromCallable cacheCleaned + deletedFromDb + networkCacheCleaned + clearedLogs
          }.subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .autoDisposable(activity as BaseActivity)
              .subscribe { cleanedAmount, throwable ->
                // TODO Use jw's byte units lib, this isn't totally accurate
                val errorMessage = throwable?.let {
                  getString(R.string.settings_error_cleaning_cache)
                } ?: getString(R.string.clear_cache_success, cleanedAmount.format())
                Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT).show()
              }
          return true
        }
        "about" -> {
          startActivity(Intent(activity, AboutActivity::class.java))
          return true
        }
        "services" -> {
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
