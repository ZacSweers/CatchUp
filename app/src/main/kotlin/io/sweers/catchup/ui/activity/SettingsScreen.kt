package io.sweers.catchup.ui.activity

import android.content.Context
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import com.alorma.compose.settings.storage.base.SettingValueState
import com.alorma.compose.settings.storage.base.getValue
import com.alorma.compose.settings.storage.base.setValue
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.byteunits.BinaryByteUnit
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.Ui
import com.slack.circuit.codegen.annotations.CircuitInject
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dev.zacsweers.catchup.di.AppScope
import io.sweers.catchup.CatchUpPreferences
import io.sweers.catchup.R
import io.sweers.catchup.data.LumberYard
import io.sweers.catchup.ui.about.AboutScreen
import io.sweers.catchup.util.clearCache
import io.sweers.catchup.util.clearFiles
import io.sweers.catchup.util.injection.qualifiers.ApplicationContext
import io.sweers.catchup.util.restartApp
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import okhttp3.Cache

@Parcelize
object SettingsScreen : Screen {
  data class State(val eventSink: (Event) -> Unit) : CircuitUiState
  sealed interface Event {
    // TODO does this make sense or should the presenter decide?
    data class NavToScreen(val screen: Screen) : Event
    object ClearCache : Event
  }
}

class SettingsPresenter
@AssistedInject
constructor(
  @Assisted private val navigator: Navigator,
  @ApplicationContext private val appContext: Context,
  private val cache: dagger.Lazy<Cache>,
  private val lumberYard: LumberYard,
  private val catchUpPreferences: CatchUpPreferences,
) : Presenter<SettingsScreen.State> {

  @CircuitInject(SettingsScreen::class, AppScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(navigator: Navigator): SettingsPresenter
  }

  @Composable
  override fun present(): SettingsScreen.State {
    // TODO blerg this isn't good in Circuit. Need an ActivityStarter instead on DI
    val view = LocalView.current

    LaunchedEffect(view) {
      catchUpPreferences.reports
        .distinctUntilChanged()
        .drop(1) // Drop the initial true emission
        .collect {
          // If we change reports to false, restart
          // TODO circuit-ify this
          Snackbar.make(
              view,
              appContext.getString(R.string.settings_reset),
              Snackbar.LENGTH_INDEFINITE
            )
            .setAction(R.string.restart) { appContext.restartApp() }
            .show()
        }
    }

    val scope = rememberCoroutineScope()
    return SettingsScreen.State { event ->
      when (event) {
        SettingsScreen.Event.ClearCache -> {
          scope.launch {
            val message =
              try {
                val cleanedAmount = clearCache()
                appContext.getString(
                  R.string.clear_cache_success,
                  BinaryByteUnit.format(cleanedAmount)
                )
              } catch (e: Exception) {
                appContext.getString(R.string.settings_error_cleaning_cache)
              }
            // TODO circuit-ify this
            Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE)
              .setAction(R.string.restart) { appContext.restartApp() }
              .show()
          }
        }
        is SettingsScreen.Event.NavToScreen -> {
          navigator.goTo(event.screen)
        }
      }
    }
  }

  // TODO extract this somewhere more testable
  private suspend fun clearCache(): Long {
    return withContext(Dispatchers.IO) {
      var cleanedSize = 0L

      // Datastore and Preferences are covered by this
      cleanedSize += appContext.clearFiles()
      cleanedSize += appContext.clearCache()
      cleanedSize +=
        with(cache.get()) {
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
      return@withContext cleanedSize
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@CircuitInject(SettingsScreen::class, AppScope::class)
class SettingsUi
@Inject
constructor(
  // TODO this is unfortunate but sorta how the settings library used here works
  private val catchUpPreferences: CatchUpPreferences,
) : Ui<SettingsScreen.State>, BaseSettingsUi by RealBaseSettingsUi(catchUpPreferences.datastore) {

  @Composable
  private fun BooleanPreference(
    key: Preferences.Key<Boolean>,
    defaultValue: Boolean,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    // TODO implement this
    dependentKeys: Set<Preferences.Key<Boolean>> = emptySet()
    // TODO icon
    ) {
    val state =
      rememberBooleanSettingState(
        key = key,
        defaultValue = defaultValue,
      )
    CheckboxPref(
      modifier = modifier,
      title = title,
      subtitle = subtitle,
      state = state,
    )
  }

  @OptIn(ExperimentalFoundationApi::class)
  @Composable
  override fun Content(state: SettingsScreen.State) {
    val eventSink = state.eventSink
    Scaffold(
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      containerColor = Color.Transparent,
      topBar = {
        TopAppBar(
          title = { Text(stringResource(R.string.title_activity_settings)) },
          navigationIcon = {
            val onBackPressedDispatcher =
              LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            IconButton(onClick = onBackPressedDispatcher::onBackPressed) {
              Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
          },
        )
      },
    ) { innerPadding ->
      LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding)
      ) {
        stickyHeader(key = "general_header") {
          ComposableHeaderItem(stringResource(R.string.general), displayDivider = false)
        }
        item(key = "smart_linking") {
          BooleanPreference(
            key = CatchUpPreferences.Keys.smartlinkingGlobal,
            defaultValue = true,
            title = stringResource(R.string.pref_smart_linking_title),
            subtitle = stringResource(R.string.pref_smart_linking_summary),
          )
        }

        item(key = "reorder_services") {
          ClickablePreference(
            title = stringResource(R.string.pref_reorder_services),
            subtitle = stringResource(R.string.pref_order_services_description)
          ) {
            eventSink(SettingsScreen.Event.NavToScreen(OrderServicesScreen))
          }
        }

        item(key = "clear_cache") {
          ClickablePreference(
            title = stringResource(R.string.pref_clear_cache),
            subtitle = stringResource(R.string.pref_clear_cache_summary),
          ) {
            eventSink(SettingsScreen.Event.ClearCache)
          }
        }

        stickyHeader(key = "theming_header") {
          ComposableHeaderItem(stringResource(R.string.prefs_theme), displayDivider = true)
        }
        item(key = "auto_theme") {
          BooleanPreference(
            key = CatchUpPreferences.Keys.dayNightAuto,
            defaultValue = true,
            title = stringResource(R.string.pref_auto_set_theme),
            subtitle = stringResource(R.string.pref_auto_set_theme_summary),
          )
        }
        item(key = "force_night") {
          // TODO would rather make the force night option disabled, but dunno how to do that
          val autoEnabled by catchUpPreferences.dayNightAuto.collectAsState(initial = true)
          if (!autoEnabled) {
            BooleanPreference(
              modifier = Modifier.animateItemPlacement(),
              key = CatchUpPreferences.Keys.dayNightForceNight,
              defaultValue = false,
              title = stringResource(R.string.pref_force_dark_theme),
              // TODO this is dynamic with pref_dark_theme_enabled/disabled
              //  subtitle = stringResource(R.string.pref_auto_set_theme_summary),
            )
          }
        }

        stickyHeader(key = "catchup_header") {
          ComposableHeaderItem("CatchUp", displayDivider = true)
        }

        item(key = "about") {
          ClickablePreference(
            title = stringResource(R.string.about),
          ) {
            eventSink(SettingsScreen.Event.NavToScreen(AboutScreen))
          }
        }

        item(key = "reports") {
          BooleanPreference(
            key = CatchUpPreferences.Keys.reports,
            defaultValue = true,
            title = stringResource(R.string.pref_reports),
            subtitle = stringResource(R.string.pref_reports_summary),
          )
        }
      }
    }
  }
}

@Composable
private fun ComposableHeaderItem(text: String, displayDivider: Boolean) {
  Surface(modifier = Modifier.fillMaxWidth()) {
    Box {
      Text(
        modifier = Modifier.padding(16.dp),
        text = text,
        color = colorResource(R.color.colorAccent),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      if (displayDivider) {
        Divider()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClickablePreference(
  modifier: Modifier = Modifier,
  title: String,
  subtitle: String? = null,
  icon: @Composable (() -> Unit)? = null,
  onClick: () -> Unit,
) {
  Surface(modifier = modifier.fillMaxWidth(), onClick = onClick) {
    SimplePrefItem(
      title,
      subtitle,
      icon,
    )
  }
}

@Composable
private fun CheckboxPref(
  modifier: Modifier = Modifier,
  state: SettingValueState<Boolean>,
  title: String,
  subtitle: String? = null,
  icon: @Composable (() -> Unit)? = null,
  onCheckedChange: (Boolean) -> Unit = {},
) {
  var storageValue by state
  val update: (Boolean) -> Unit = { boolean ->
    storageValue = boolean
    onCheckedChange(storageValue)
  }
  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .toggleable(
          value = storageValue,
          role = Role.Checkbox,
          onValueChange = { update(!storageValue) }
        ),
  ) {
    SimplePrefItem(
      title,
      subtitle,
      icon,
    ) {
      Switch(checked = storageValue, onCheckedChange = update)
    }
  }
}

@Composable
private fun SimplePrefItem(
  title: String,
  subtitle: String? = null,
  icon: @Composable (() -> Unit)? = null,
  action: @Composable RowScope.() -> Unit = {},
) {
  Row(
    modifier = Modifier.padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = spacedBy(16.dp)
  ) {
    icon?.let {
      // TODO()
      it()
    }
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = spacedBy(4.dp),
    ) {
      Text(text = title, style = MaterialTheme.typography.titleMedium)
      subtitle?.let {
        CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
          Text(
            text = it,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
    action()
  }
}
