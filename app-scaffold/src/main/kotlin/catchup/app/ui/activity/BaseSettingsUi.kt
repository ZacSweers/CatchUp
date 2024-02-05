package catchup.app.ui.activity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import catchup.compose.rememberStableCoroutineScope
import com.alorma.compose.settings.storage.base.SettingValueState
import com.alorma.compose.settings.storage.datastore.GenericPreferenceDataStoreSettingValueState
import com.alorma.compose.settings.storage.datastore.rememberPreferenceDataStoreBooleanSettingState
import com.alorma.compose.settings.storage.datastore.rememberPreferenceDataStoreIntSettingState

@Stable
interface BaseSettingsUi {
  @Composable
  fun rememberBooleanSettingState(
    key: Preferences.Key<Boolean>,
    defaultValue: Boolean,
  ): SettingValueState<Boolean>

  @Composable
  fun rememberStringSettingState(
    key: Preferences.Key<String>,
    defaultValue: String,
  ): SettingValueState<String>

  @Composable
  fun rememberIntSettingState(key: Preferences.Key<Int>, defaultValue: Int): SettingValueState<Int>

  @Composable
  fun rememberLongSettingState(
    key: Preferences.Key<Long>,
    defaultValue: Long,
  ): SettingValueState<Long>
}

class RealBaseSettingsUi(private val dataStore: DataStore<Preferences>) : BaseSettingsUi {
  @Composable
  override fun rememberBooleanSettingState(key: Preferences.Key<Boolean>, defaultValue: Boolean) =
    rememberPreferenceDataStoreBooleanSettingState(
      key = key.name,
      dataStore = dataStore,
      defaultValue = defaultValue,
    )

  @Composable
  override fun rememberIntSettingState(key: Preferences.Key<Int>, defaultValue: Int) =
    rememberPreferenceDataStoreIntSettingState(
      key = key.name,
      dataStore = dataStore,
      defaultValue = defaultValue,
    )

  @Composable
  override fun rememberLongSettingState(
    key: Preferences.Key<Long>,
    defaultValue: Long,
  ): SettingValueState<Long> {
    val scope = rememberStableCoroutineScope()
    return remember {
      GenericPreferenceDataStoreSettingValueState(
        coroutineScope = scope,
        dataStore = dataStore,
        dataStoreKey = key,
        defaultValue = defaultValue,
      )
    }
  }

  @Composable
  override fun rememberStringSettingState(
    key: Preferences.Key<String>,
    defaultValue: String,
  ): SettingValueState<String> {
    val scope = rememberStableCoroutineScope()
    return remember {
      GenericPreferenceDataStoreSettingValueState(
        coroutineScope = scope,
        dataStore = dataStore,
        dataStoreKey = key,
        defaultValue = defaultValue,
      )
    }
  }
}
