package catchup.app.ui.debug

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import android.util.DisplayMetrics
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.datastore.preferences.core.Preferences.Key
import catchup.app.CatchUpPreferences
import catchup.app.data.DebugPreferences
import catchup.app.data.DiskLumberYard
import catchup.app.data.LumberYard
import catchup.app.data.LumberYard.Entry
import catchup.app.home.DrawerScreen
import catchup.app.ui.activity.BaseSettingsUi
import catchup.app.ui.activity.RealBaseSettingsUi
import catchup.app.ui.debug.DebugItem.Element
import catchup.app.ui.debug.DebugItem.Element.SpinnerElement.ValueType
import catchup.app.ui.debug.DebugItem.Header
import catchup.app.ui.debug.DebugSettingsScreen.Event.NavigateTo
import catchup.app.ui.debug.DebugSettingsScreen.Event.ShareLogs
import catchup.app.ui.debug.DebugSettingsScreen.Event.ToggleLogs
import catchup.app.ui.debug.DebugSettingsScreen.State
import catchup.app.ui.debug.LogsShareResult.DISMISS
import catchup.app.ui.debug.LogsShareResult.SHARE
import catchup.appconfig.AppConfig
import catchup.compose.CatchUpTheme
import catchup.compose.rememberStableCoroutineScope
import catchup.di.AppScope
import catchup.di.DataMode
import catchup.util.truncateAt
import com.alorma.compose.settings.storage.base.SettingValueState
import com.jakewharton.processphoenix.ProcessPhoenix
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuitx.android.IntentScreen
import com.slack.circuitx.overlays.BottomSheetOverlay
import com.squareup.anvil.annotations.ContributesTo
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dev.zacsweers.catchup.app.scaffold.R
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import leakcanary.LeakCanary
import okhttp3.Cache
import okhttp3.OkHttpClient

@Parcelize private object LogsModal : Screen

@ContributesTo(AppScope::class)
@Module
object ContributorModule {
  @Provides fun provideDrawerScreen(): DrawerScreen = DrawerScreen(DebugSettingsScreen)
}

@Parcelize
object DebugSettingsScreen : Screen {
  data class State(
    val items: ImmutableList<DebugItem>,
    val logsToShow: ImmutableList<Entry>,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    data class NavigateTo(val screen: Screen) : Event

    data class ToggleLogs(val show: Boolean) : Event

    data object ShareLogs : Event
  }
}

class DebugSettingsPresenter
@AssistedInject
constructor(
  @Assisted private val navigator: Navigator,
  private val client: Lazy<OkHttpClient>,
  private val lumberYard: LumberYard,
  private val debugPreferences: DebugPreferences,
  private val appConfig: AppConfig,
) : Presenter<State> {
  @CircuitInject(DebugSettingsScreen::class, AppScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(navigator: Navigator): DebugSettingsPresenter
  }

  @Composable
  override fun present(): State {
    LaunchedEffect(Unit) { debugPreferences.animationSpeed.collect(::applyAnimationSpeed) }

    var showLogs by remember { mutableStateOf(false) }

    val clientCache by
      produceState<Cache?>(null) { value = withContext(IO) { client.get().cache!! } }

    val scope = rememberStableCoroutineScope()
    val displayMetrics = LocalContext.current.resources.displayMetrics
    val items =
      remember(displayMetrics, clientCache) {
        clientCache?.let { items(displayMetrics, it) } ?: persistentListOf()
      }
    return State(
      items,
      if (showLogs) {
        lumberYard.bufferedLogs()
      } else {
        persistentListOf()
      },
    ) { event ->
      when (event) {
        is NavigateTo -> {
          if (event.screen is LogsModal) {
            showLogs = true
          } else {
            navigator.goTo(event.screen)
          }
        }
        is ToggleLogs -> showLogs = event.show
        ShareLogs -> {
          showLogs = false
          scope.launch {
            val text =
              if (lumberYard is DiskLumberYard) {
                lumberYard.flush()
                // TODO write back to a file first to share?
                lumberYard.currentLogFileText()
              } else {
                lumberYard
                  .bufferedLogs()
                  .joinToString(separator = "\n", transform = Entry::prettyPrint)
              }
            val sendIntent = Intent(Intent.ACTION_SEND)
            sendIntent.type = "text/plain"
            sendIntent.putExtra(Intent.EXTRA_TEXT, text)
            navigator.goTo(IntentScreen(Intent.createChooser(sendIntent, "Share logs")))
          }
        }
      }
    }
  }

  @SuppressLint("DiscouragedPrivateApi")
  private fun applyAnimationSpeed(multiplier: Int) {
    try {
      val method =
        ValueAnimator::class
          .java
          .getDeclaredMethod("setDurationScale", Float::class.javaPrimitiveType)
      method.invoke(null, multiplier.toFloat())
    } catch (e: Exception) {
      throw RuntimeException("Unable to apply animation speed.", e)
    }
  }

  private fun items(displayMetrics: DisplayMetrics, cache: Cache): ImmutableList<DebugItem> {
    return buildList {
        add(Header("Network"))
        add(
          Element.SpinnerElement(
            "Data Mode",
            CatchUpPreferences.Keys.dataMode,
            DataMode.entries.map { it.name },
            ValueType.STRING,
            defaultOptionIndex = DataMode.REAL.ordinal,
          )
        )
        // TODO conditional based on mock mode?
        add(
          Element.SpinnerElement(
            "Delay",
            DebugPreferences.Keys.networkDelay,
            listOf(250, 500, 1000, 2000, 3000, 5000),
            ValueType.LONG,
            defaultOptionIndex = 3,
          ) { index ->
            "${index}ms"
          }
        )
        add(
          Element.SpinnerElement(
            "Variance",
            DebugPreferences.Keys.networkVariancePercent,
            listOf(20, 40, 60),
            ValueType.INT,
            defaultOptionIndex = 1,
          ) { index ->
            "${index}%"
          }
        )
        add(
          Element.SpinnerElement(
            "Error",
            DebugPreferences.Keys.networkFailurePercent,
            listOf(0, 3, 10, 25, 50, 75, 100),
            ValueType.INT,
            defaultOptionIndex = 1,
          ) { index ->
            "${index}%"
          }
        )

        add(Header("User Interface"))
        add(
          Element.SpinnerElement(
            "Animations",
            DebugPreferences.Keys.animationSpeed,
            listOf(1, 2, 3, 5, 10),
            ValueType.INT,
            defaultOptionIndex = 0,
          ) { index ->
            if (index == 1) {
              "Normal"
            } else {
              "${index}x slower"
            }
          }
        )
        add(Header("Logs"))
        add(Element.ButtonElement("Show logs", LogsModal))
        add(
          Element.ButtonElement(
            "Leak Analysis",
            IntentScreen(LeakCanary.newLeakDisplayActivityIntent()),
          )
        )
        add(Header("Build Information"))
        add(Element.ValueElement("Name", appConfig.versionName))
        add(Element.ValueElement("Code", appConfig.versionCode.toString()))
        add(Element.ValueElement("Date", appConfig.timestamp))

        add(Header("Device Information"))
        add(Element.ValueElement("Make", Build.MANUFACTURER truncateAt 20))
        add(Element.ValueElement("Model", Build.MODEL truncateAt 20))
        add(
          Element.ValueElement(
            "Resolution",
            "${displayMetrics.heightPixels}x${displayMetrics.widthPixels}",
          )
        )
        add(
          Element.ValueElement(
            "Density",
            "${displayMetrics.densityDpi}dpi (${displayMetrics.bucket})",
          )
        )
        add(Element.ValueElement("Release", Build.VERSION.RELEASE))
        add(Element.ValueElement("API", appConfig.sdkInt.toString()))

        add(Header("OkHttp Cache"))
        add(Element.ValueElement("Max Size", cache.maxSize().sizeString))
        val writeTotal = cache.writeSuccessCount() + cache.writeAbortCount()
        val percentage = (1f * cache.writeAbortCount() / writeTotal * 100).toInt()
        add(
          Element.ValueElement(
            "Write Errors",
            "${cache.writeAbortCount()} / $writeTotal ($percentage%)",
          )
        )
        add(Element.ValueElement("Request Count", cache.requestCount().toString()))
        add(Element.ValueElement("   Network Count", cache.networkCount().toString()))
        add(Element.ValueElement("   Hit Count", cache.hitCount().toString()))
      }
      .toImmutableList()
  }
}

private enum class LogsShareResult {
  SHARE,
  DISMISS,
}

@CircuitInject(DebugSettingsScreen::class, AppScope::class)
class DebugSettingsUi @Inject constructor(private val debugPreferences: DebugPreferences) :
  Ui<State>, BaseSettingsUi by RealBaseSettingsUi(debugPreferences.datastore) {
  @Composable
  override fun Content(state: State, modifier: Modifier) {
    if (state.logsToShow.isNotEmpty()) {
      val overlayHost = LocalOverlayHost.current
      LaunchedEffect(overlayHost) {
        val result =
          overlayHost.show(
            BottomSheetOverlay(state.logsToShow, onDismiss = { DISMISS }) { entries, navigator ->
              LogsList(entries) { navigator.finish(SHARE) }
            }
          )
        when (result) {
          SHARE -> {
            state.eventSink(ShareLogs)
          }
          DISMISS -> {
            state.eventSink(ToggleLogs(show = false))
          }
        }
      }
    }

    CatchUpTheme(useDarkTheme = true) {
      Surface(modifier.fillMaxHeight()) {
        LazyColumn(verticalArrangement = spacedBy(8.dp), modifier = Modifier.systemBarsPadding()) {
          item(key = "header") {
            Row(
              Modifier.fillMaxWidth().padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.End,
            ) {
              Text(
                stringResource(R.string.development_settings),
                style = MaterialTheme.typography.headlineSmall,
              )
              Spacer(Modifier.width(16.dp))
              // TODO kinda gross but shrug
              val icon =
                (AppCompatResources.getDrawable(LocalContext.current, R.mipmap.ic_launcher)
                    as AdaptiveIconDrawable)
                  .toBitmap()
              Image(
                bitmap = icon.asImageBitmap(),
                contentDescription = "CatchUp icon",
                modifier = Modifier.size(48.dp),
              )
            }
          }

          state.items.forEachIndexed { index, item ->
            when (item) {
              is Header -> {
                item(index) { DebugSectionHeader(item.title) }
              }
              is Element -> {
                item(index) {
                  val scope = rememberStableCoroutineScope()
                  val context = LocalContext.current
                  DebugElementContent(item, { screen -> state.eventSink(NavigateTo(screen)) }) {
                    key,
                    value ->
                    scope.launch {
                      debugPreferences.edit { prefs ->
                        @Suppress("UNCHECKED_CAST")
                        prefs[key as Key<Any?>] = value
                      }
                      if (item.requiresRestart) {
                        ProcessPhoenix.triggerRebirth(context)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  @Composable
  private fun DebugElementContent(
    element: Element,
    onNavigate: (Screen) -> Unit,
    onUpdate: (Key<*>, Any?) -> Unit,
  ) {
    @Suppress("UNCHECKED_CAST")
    when (element) {
      is Element.ButtonElement -> {
        element.Content { screen -> onNavigate(screen) }
      }
      is Element.SpinnerElement<*> -> {
        // TODO this is really... ugly
        val state: SettingValueState<*> =
          when (element.valueType) {
            ValueType.INT -> {
              val typedElement = element as Element.SpinnerElement<Int>
              rememberIntSettingState(
                typedElement.key,
                typedElement.options[typedElement.defaultOptionIndex],
              )
            }
            ValueType.LONG -> {
              val typedElement = element as Element.SpinnerElement<Long>
              rememberLongSettingState(
                typedElement.key,
                typedElement.options[typedElement.defaultOptionIndex],
              )
            }
            ValueType.STRING -> {
              val typedElement = element as Element.SpinnerElement<String>
              rememberStringSettingState(
                typedElement.key,
                typedElement.options[typedElement.defaultOptionIndex],
              )
            }
          }
        (element as Element.SpinnerElement<Any>).Content(state as SettingValueState<Any>) { newValue
          ->
          onUpdate(element.key, newValue)
        }
      }
      is Element.SwitchElement -> {
        element.Content(
          rememberBooleanSettingState(key = element.key, defaultValue = element.defaultValue)
        ) { newValue ->
          onUpdate(element.key, newValue)
        }
      }
      is Element.ValueElement -> {
        element.Content()
      }
    }
  }
}

// TODO
//  - DSL for building it
//  - What about scalpel/telescope/etc?

@Immutable
sealed interface DebugItem {
  data class Header(val title: String) : DebugItem

  sealed interface Element : DebugItem {
    val requiresRestart: Boolean

    data class SpinnerElement<T>(
      val title: String,
      val key: Key<T>,
      val options: List<T>,
      val valueType: ValueType<T>,
      val defaultOptionIndex: Int = 0,
      override val requiresRestart: Boolean = false,
      val formatSelection: (T) -> String = { it.toString() },
    ) : Element {
      sealed interface ValueType<T> {
        data object STRING : ValueType<String>

        data object INT : ValueType<Int>

        data object LONG : ValueType<Long>
      }

      @OptIn(ExperimentalMaterial3Api::class)
      @Composable
      fun Content(
        state: SettingValueState<T>,
        modifier: Modifier = Modifier,
        onSelection: (T) -> Unit,
      ) {
        val update: (T) -> Unit = { newValue ->
          state.value = newValue
          onSelection(state.value)
        }
        Row(
          modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          DebugLabelText(title, modifier = Modifier.weight(1f))

          var expanded by remember { mutableStateOf(false) }
          ExposedDropdownMenuBox(
            modifier = Modifier.weight(1f),
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
          ) {
            OutlinedTextField(
              value = formatSelection(state.value),
              modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
              textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
              onValueChange = {},
              readOnly = true,
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
              for (option in options) {
                DropdownMenuItem(
                  text = {
                    Text(
                      text = formatSelection(option),
                      // TODO why do none of these work?
                      style = LocalTextStyle.current.copy(textAlign = TextAlign.End),
                      textAlign = TextAlign.End,
                    )
                  },
                  onClick = {
                    expanded = false
                    update(option)
                  },
                )
              }
            }
          }
        }
      }
    }

    data class ButtonElement(
      val text: String,
      val onClickScreen: Screen,
      override val requiresRestart: Boolean = false,
    ) : Element {
      @Composable
      fun Content(modifier: Modifier = Modifier, onClick: (Screen) -> Unit) {
        Button(
          modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp),
          onClick = { onClick(onClickScreen) },
        ) {
          Text(text)
        }
      }
    }

    data class SwitchElement(
      val title: String,
      val key: Key<Boolean>,
      val defaultValue: Boolean,
      override val requiresRestart: Boolean = false,
    ) : Element {
      @Composable
      fun Content(
        state: SettingValueState<Boolean>,
        modifier: Modifier = Modifier,
        onCheckedChange: (Boolean) -> Unit,
      ) {
        val update: (Boolean) -> Unit = { boolean ->
          state.value = boolean
          onCheckedChange(state.value)
        }
        Row(
          modifier =
            modifier
              .toggleable(
                value = state.value,
                role = Role.Checkbox,
                onValueChange = { update(!state.value) },
              )
              .padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          DebugLabelText(title)
          Spacer(modifier = Modifier.weight(1f))
          Switch(checked = state.value, onCheckedChange = update)
        }
      }
    }

    data class ValueElement(
      val title: String,
      val value: String, // Are any of these live?
      override val requiresRestart: Boolean = false,
    ) : Element {
      @Composable
      fun Content(modifier: Modifier = Modifier) {
        Row(
          modifier = modifier.padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          DebugLabelText(title)
          Spacer(modifier = Modifier.weight(1f))
          Text(value)
        }
      }
    }
  }
}

@Composable
private fun DebugSectionHeader(text: String) {
  Column {
    Spacer(Modifier.height(16.dp))
    Text(
      text = text.uppercase(),
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 12.dp),
    )
    Spacer(Modifier.height(2.dp))
    HorizontalDivider()
    Spacer(Modifier.height(2.dp))
  }
}

@Composable
private fun DebugLabelText(text: String, modifier: Modifier = Modifier) {
  Text(text, modifier = modifier)
}

private val DisplayMetrics.bucket: String
  get() {
    return when (densityDpi) {
      DisplayMetrics.DENSITY_LOW -> "ldpi"
      DisplayMetrics.DENSITY_MEDIUM -> "mdpi"
      DisplayMetrics.DENSITY_HIGH -> "hdpi"
      DisplayMetrics.DENSITY_XHIGH -> "xhdpi"
      DisplayMetrics.DENSITY_XXHIGH -> "xxhdpi"
      DisplayMetrics.DENSITY_XXXHIGH -> "xxxhdpi"
      DisplayMetrics.DENSITY_TV -> "tvdpi"
      else -> densityDpi.toString()
    }
  }

private val Long.sizeString: String
  get() {
    var bytes = this
    val units = arrayOf("B", "KB", "MB", "GB")
    var unit = 0
    while (bytes >= 1024) {
      bytes /= 1024
      unit += 1
    }
    return bytes.toString() + units[unit]
  }
