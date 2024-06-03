package catchup.app.ui.activity

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import catchup.app.CatchUpPreferences
import catchup.app.data.DiskLumberYard
import catchup.app.data.LumberYard
import catchup.app.ui.about.AboutScreen
import catchup.app.ui.activity.SettingsScreen.Event.ClearCache
import catchup.app.ui.activity.SettingsScreen.Event.NavToScreen
import catchup.app.ui.activity.SettingsScreen.State
import catchup.app.util.restartApp
import catchup.base.ui.BackPressNavButton
import catchup.compose.ContentAlphas
import catchup.compose.DisableableContent
import catchup.compose.LocalEnabled
import catchup.compose.rememberStableCoroutineScope
import catchup.deeplink.DeepLinkable
import catchup.di.AppScope
import catchup.util.clearCache
import catchup.util.clearFiles
import catchup.util.injection.qualifiers.ApplicationContext
import com.alorma.compose.settings.storage.base.SettingValueState
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.byteunits.BinaryByteUnit
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.squareup.anvil.annotations.ContributesMultibinding
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.multibindings.StringKey
import dev.zacsweers.catchup.app.scaffold.R as AppScaffoldR
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import okhttp3.Cache

@Parcelize
data class SettingsScreen(val showTopAppBar: Boolean = true) : Screen {

  @ContributesMultibinding(AppScope::class, boundType = DeepLinkable::class)
  @StringKey("settings")
  object Deeplinker : DeepLinkable {
    override fun createScreen(queryParams: ImmutableMap<String, List<String?>>): Screen =
      SettingsScreen()
  }

  data class State(val showTopAppBar: Boolean, val eventSink: (Event) -> Unit) : CircuitUiState

  sealed interface Event {
    // TODO does this make sense or should the presenter decide?
    data class NavToScreen(val screen: Screen) : Event

    data object ClearCache : Event
  }
}

class SettingsPresenter
@AssistedInject
constructor(
  @Assisted private val screen: SettingsScreen,
  @Assisted private val navigator: Navigator,
  @ApplicationContext private val appContext: Context,
  private val cache: dagger.Lazy<Cache>,
  private val lumberYard: LumberYard,
  private val catchUpPreferences: CatchUpPreferences,
) : Presenter<State> {

  @CircuitInject(SettingsScreen::class, AppScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(screen: SettingsScreen, navigator: Navigator): SettingsPresenter
  }

  @Composable
  override fun present(): State {
    // TODO blerg this isn't good in Circuit. Need an ActivityStarter instead on DI
    val view = LocalView.current

    LaunchedEffect(view) {
      catchUpPreferences.reports
        .drop(1) // Drop the initial true emission
        .collect {
          // If we change reports to false, restart
          // TODO circuit-ify this
          Snackbar.make(
              view,
              appContext.getString(AppScaffoldR.string.settings_reset),
              Snackbar.LENGTH_INDEFINITE,
            )
            .setAction(AppScaffoldR.string.restart) { appContext.restartApp() }
            .show()
        }
    }

    val scope = rememberStableCoroutineScope()
    return State(screen.showTopAppBar) { event ->
      when (event) {
        ClearCache -> {
          scope.launch {
            val message =
              try {
                val cleanedAmount = clearCache()
                appContext.getString(
                  AppScaffoldR.string.clear_cache_success,
                  BinaryByteUnit.format(cleanedAmount),
                )
              } catch (e: Exception) {
                appContext.getString(AppScaffoldR.string.settings_error_cleaning_cache)
              }
            // TODO circuit-ify this
            Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE)
              .setAction(AppScaffoldR.string.restart) { appContext.restartApp() }
              .show()
          }
        }
        is NavToScreen -> {
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
      cleanedSize +=
        if (lumberYard is DiskLumberYard) {
          lumberYard.cleanUp()
        } else {
          0
        }
      return@withContext cleanedSize
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(SettingsScreen::class, AppScope::class)
class SettingsUi
@Inject
constructor(
  // TODO this is unfortunate but sorta how the settings library used here works
  private val catchUpPreferences: CatchUpPreferences
) : Ui<State>, BaseSettingsUi by RealBaseSettingsUi(catchUpPreferences.datastore) {

  @Composable
  private fun BooleanPreference(
    key: Preferences.Key<Boolean>,
    defaultValue: Boolean,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    // TODO icon
  ) {
    val state = rememberBooleanSettingState(key = key, defaultValue = defaultValue)
    CheckboxPref(modifier = modifier, title = title, subtitle = subtitle, state = state)
  }

  @OptIn(ExperimentalFoundationApi::class)
  @Composable
  override fun Content(state: State, modifier: Modifier) {
    val topAppBar: @Composable () -> Unit =
      if (state.showTopAppBar) {
        {
          TopAppBar(
            title = { Text(stringResource(AppScaffoldR.string.title_activity_settings)) },
            navigationIcon = { BackPressNavButton() },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
          )
        }
      } else {
        {}
      }
    Scaffold(
      modifier = modifier,
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      containerColor = Color.Transparent,
      topBar = topAppBar,
    ) { innerPadding ->
      LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding)
      ) {
        stickyHeader(key = "general_header") {
          ComposableHeaderItem(stringResource(AppScaffoldR.string.general), displayDivider = false)
        }
        item(key = "smart_linking") {
          BooleanPreference(
            key = CatchUpPreferences.Keys.smartlinkingGlobal,
            defaultValue = true,
            title = stringResource(AppScaffoldR.string.pref_smart_linking_title),
            subtitle = stringResource(AppScaffoldR.string.pref_smart_linking_summary),
          )
        }

        item(key = "reorder_services") {
          ClickablePreference(
            title = stringResource(AppScaffoldR.string.pref_reorder_services),
            subtitle = stringResource(AppScaffoldR.string.pref_order_services_description),
          ) {
            state.eventSink(NavToScreen(OrderServicesScreen))
          }
        }

        item(key = "clear_cache") {
          ClickablePreference(
            title = stringResource(AppScaffoldR.string.pref_clear_cache),
            subtitle = stringResource(AppScaffoldR.string.pref_clear_cache_summary),
          ) {
            state.eventSink(ClearCache)
          }
        }

        stickyHeader(key = "theming_header") {
          ComposableHeaderItem(
            stringResource(AppScaffoldR.string.prefs_theme),
            displayDivider = true,
          )
        }

        item(key = "dynamic_theme") {
          BooleanPreference(
            key = CatchUpPreferences.Keys.dynamicTheme,
            modifier = Modifier.animateContentSize(),
            defaultValue = false,
            title = stringResource(AppScaffoldR.string.pref_dynamic_theme_title),
            subtitle = stringResource(AppScaffoldR.string.pref_dynamic_theme_summary),
          )
        }

        item(key = "auto_theme") {
          BooleanPreference(
            key = CatchUpPreferences.Keys.dayNightAuto,
            defaultValue = true,
            title = stringResource(AppScaffoldR.string.pref_auto_set_theme),
            subtitle = stringResource(AppScaffoldR.string.pref_auto_set_theme_summary),
          )
        }
        item(key = "force_night") {
          val autoEnabled by catchUpPreferences.dayNightAuto.collectAsState()
          DisableableContent(enabled = !autoEnabled) {
            val forceNightValue by catchUpPreferences.dayNightForceNight.collectAsState()
            BooleanPreference(
              key = CatchUpPreferences.Keys.dayNightForceNight,
              modifier = Modifier.animateContentSize(), // Because the summary changes
              defaultValue = false,
              title = stringResource(AppScaffoldR.string.pref_force_dark_theme),
              subtitle =
                if (!LocalEnabled.current) {
                  stringResource(AppScaffoldR.string.pref_dark_theme_disabled_auto)
                } else if (forceNightValue) {
                  stringResource(AppScaffoldR.string.pref_dark_theme_enabled)
                } else {
                  stringResource(AppScaffoldR.string.pref_dark_theme_disabled)
                },
            )
          }
        }

        stickyHeader(key = "catchup_header") {
          ComposableHeaderItem("CatchUp", displayDivider = true)
        }

        item(key = "about") {
          ClickablePreference(title = stringResource(AppScaffoldR.string.about)) {
            state.eventSink(NavToScreen(AboutScreen()))
          }
        }

        item(key = "reports") {
          BooleanPreference(
            key = CatchUpPreferences.Keys.reports,
            defaultValue = true,
            title = stringResource(AppScaffoldR.string.pref_reports),
            subtitle = stringResource(AppScaffoldR.string.pref_reports_summary),
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
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      if (displayDivider) {
        HorizontalDivider()
      }
    }
  }
}

@Composable
private fun ClickablePreference(
  title: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  icon: @Composable (() -> Unit)? = null,
  onClick: () -> Unit,
) {
  Surface(modifier = modifier.fillMaxWidth(), onClick = onClick) {
    SimplePrefItem(title, subtitle, icon)
  }
}

@Composable
private fun CheckboxPref(
  state: SettingValueState<Boolean>,
  title: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  icon: @Composable (() -> Unit)? = null,
  onCheckedChange: (Boolean) -> Unit = {},
) {
  val update: (Boolean) -> Unit = { boolean ->
    state.value = boolean
    onCheckedChange(state.value)
  }
  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .toggleable(
          value = state.value,
          role = Role.Checkbox,
          enabled = LocalEnabled.current,
          onValueChange = { update(!state.value) },
        )
  ) {
    SimplePrefItem(title, subtitle, icon) {
      // TODO switches use the primary color, which ends up being white on white.
      Switch(checked = state.value, onCheckedChange = update, enabled = LocalEnabled.current)
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
    horizontalArrangement = spacedBy(16.dp),
  ) {
    icon?.let {
      // TODO()
      it()
    }
    Column(modifier = Modifier.weight(1f), verticalArrangement = spacedBy(4.dp)) {
      val titleAlpha = if (LocalEnabled.current) ContentAlphas.High else ContentAlphas.Disabled
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        // TODO why do I have to do all this manually? Why doesn't this respect LocalContentAlpha
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = titleAlpha),
      )
      subtitle?.let {
        val subtitleAlpha =
          if (LocalEnabled.current) ContentAlphas.Medium else ContentAlphas.Disabled
        Text(
          text = it,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = subtitleAlpha),
        )
      }
    }
    action()
  }
}
