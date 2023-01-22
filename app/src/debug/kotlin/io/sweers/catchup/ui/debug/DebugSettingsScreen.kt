package io.sweers.catchup.ui.debug

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import com.alorma.compose.settings.storage.base.SettingValueState
import com.alorma.compose.settings.storage.base.getValue
import com.alorma.compose.settings.storage.base.setValue
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.Ui
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.overlay.LocalOverlayHost
import com.squareup.anvil.annotations.ContributesTo
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dev.zacsweers.catchup.appconfig.AppConfig
import dev.zacsweers.catchup.circuit.BottomSheetOverlay
import dev.zacsweers.catchup.circuit.IntentScreen
import dev.zacsweers.catchup.compose.CatchUpTheme
import dev.zacsweers.catchup.di.AppScope
import io.sweers.catchup.R
import io.sweers.catchup.data.DebugPreferences
import io.sweers.catchup.data.LumberYard
import io.sweers.catchup.home.DrawerScreen
import io.sweers.catchup.ui.activity.BaseSettingsUi
import io.sweers.catchup.ui.activity.RealBaseSettingsUi
import io.sweers.catchup.ui.debug.DebugItem.Element.SpinnerElement.ValueType
import io.sweers.catchup.util.truncateAt
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import leakcanary.LeakCanary
import okhttp3.Cache
import okhttp3.OkHttpClient

private fun items(
  appConfig: AppConfig,
  displayMetrics: DisplayMetrics,
  cache: Cache?
): ImmutableList<DebugItem> {
  if (cache == null) return persistentListOf()
  return buildList {
      add(DebugItem.Header("Network"))
      add(
        // TODO requires context.restartApp()
        DebugItem.Element.SwitchElement(
          "Mock Mode",
          DebugPreferences.Keys.mockModeEnabled,
          defaultValue = false
        )
      )
      // TODO conditional based on mock mode?
      add(
        DebugItem.Element.SpinnerElement(
          "Delay",
          DebugPreferences.Keys.networkDelay,
          listOf(250, 500, 1000, 2000, 3000, 5000),
          ValueType.LONG,
          defaultOptionIndex = 3
        ) { index ->
          "${index}ms"
        }
      )
      add(
        DebugItem.Element.SpinnerElement(
          "Variance",
          DebugPreferences.Keys.networkVariancePercent,
          listOf(20, 40, 60),
          ValueType.INT,
          defaultOptionIndex = 1
        ) { index ->
          "${index}%"
        }
      )
      add(
        DebugItem.Element.SpinnerElement(
          "Error",
          DebugPreferences.Keys.networkFailurePercent,
          listOf(0, 3, 10, 25, 50, 75, 100),
          ValueType.INT,
          defaultOptionIndex = 1
        ) { index ->
          "${index}%"
        }
      )

      add(DebugItem.Header("User Interface"))
      add(
        DebugItem.Element.SpinnerElement(
          "Animations",
          DebugPreferences.Keys.animationSpeed,
          listOf(1, 2, 3, 5, 10),
          ValueType.INT,
          defaultOptionIndex = 0
        ) { index ->
          if (index == 1) {
            "Normal"
          } else {
            "${index}x slower"
          }
        }
      )
      add(DebugItem.Header("Logs"))
      add(DebugItem.Element.ButtonElement("Show logs", LogsModal))
      add(
        DebugItem.Element.ButtonElement(
          "Leak Analysis",
          IntentScreen(LeakCanary.newLeakDisplayActivityIntent())
        )
      )
      add(DebugItem.Header("Build Information"))
      add(DebugItem.Element.ValueElement("Name", appConfig.versionName))
      add(DebugItem.Element.ValueElement("Code", appConfig.versionCode.toString()))
      add(DebugItem.Element.ValueElement("Date", appConfig.timestamp))

      add(DebugItem.Header("Device Information"))
      add(DebugItem.Element.ValueElement("Make", Build.MANUFACTURER truncateAt 20))
      add(DebugItem.Element.ValueElement("Model", Build.MODEL truncateAt 20))
      add(
        DebugItem.Element.ValueElement(
          "Resolution",
          "${displayMetrics.heightPixels}x${displayMetrics.widthPixels}"
        )
      )
      add(
        DebugItem.Element.ValueElement(
          "Density",
          "${displayMetrics.densityDpi}dpi (${displayMetrics.bucket})"
        )
      )
      add(DebugItem.Element.ValueElement("Release", Build.VERSION.RELEASE))
      add(DebugItem.Element.ValueElement("API", appConfig.sdkInt.toString()))

      add(DebugItem.Header("OkHttp Cache"))
      add(DebugItem.Element.ValueElement("Max Size", cache.maxSize().sizeString))
      val writeTotal = cache.writeSuccessCount() + cache.writeAbortCount()
      val percentage = (1f * cache.writeAbortCount() / writeTotal * 100).toInt()
      add(
        DebugItem.Element.ValueElement(
          "Write Errors",
          "${cache.writeAbortCount()} / $writeTotal ($percentage%)"
        )
      )
      add(DebugItem.Element.ValueElement("Request Count", cache.requestCount().toString()))
      add(DebugItem.Element.ValueElement("   Network Count", cache.networkCount().toString()))
      add(DebugItem.Element.ValueElement("   Hit Count", cache.hitCount().toString()))
    }
    .toImmutableList()
}

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
    val logsToShow: ImmutableList<LumberYard.Entry>,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState
  sealed interface Event {
    data class NavigateTo(val screen: Screen) : Event
    data class ToggleLogs(val show: Boolean) : Event
    object ShareLogs : Event
  }
}

class DebugSettingsPresenter
@AssistedInject
constructor(
  @Assisted private val navigator: Navigator,
  private val client: Lazy<OkHttpClient>,
  private val lumberYard: LumberYard,
  private val debugPreferences: DebugPreferences,
  private val appConfig: AppConfig
) : Presenter<DebugSettingsScreen.State> {
  @CircuitInject(DebugSettingsScreen::class, AppScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(navigator: Navigator): DebugSettingsPresenter
  }

  @Composable
  override fun present(): DebugSettingsScreen.State {
    LaunchedEffect(Unit) {
      debugPreferences.animationSpeed.distinctUntilChanged().collect(::applyAnimationSpeed)
    }

    var showLogs by remember { mutableStateOf(false) }

    val clientCache by
      produceState<Cache?>(null) { value = withContext(IO) { client.get().cache!! } }

    val scope = rememberCoroutineScope()
    return DebugSettingsScreen.State(
      items(appConfig, LocalContext.current.resources.displayMetrics, clientCache),
      if (showLogs) lumberYard.bufferedLogs().toImmutableList() else persistentListOf(),
    ) { event ->
      when (event) {
        is DebugSettingsScreen.Event.NavigateTo -> {
          if (event.screen is LogsModal) {
            showLogs = true
          } else {
            navigator.goTo(event.screen)
          }
        }
        is DebugSettingsScreen.Event.ToggleLogs -> showLogs = event.show
        DebugSettingsScreen.Event.ShareLogs -> {
          showLogs = false
          scope.launch {
            val file = withContext(IO) { lumberYard.save() }
            val sendIntent = Intent(Intent.ACTION_SEND)
            sendIntent.type = "text/plain"
            sendIntent.putExtra(Intent.EXTRA_STREAM, file.toUri())
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            navigator.goTo(IntentScreen(sendIntent, isChooser = true))
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
}

private enum class LogsShareResult {
  SHARE,
  DISMISS
}

@CircuitInject(DebugSettingsScreen::class, AppScope::class)
class DebugSettingsUi
@Inject
constructor(
  private val debugPreferences: DebugPreferences,
) :
  Ui<DebugSettingsScreen.State>, BaseSettingsUi by RealBaseSettingsUi(debugPreferences.datastore) {
  @Composable
  override fun Content(state: DebugSettingsScreen.State) {
    val eventSink = state.eventSink

    if (state.logsToShow.isNotEmpty()) {
      val overlayHost = LocalOverlayHost.current
      LaunchedEffect(overlayHost) {
        val result =
          overlayHost.show(
            BottomSheetOverlay(state.logsToShow, onDismiss = { LogsShareResult.DISMISS }) {
              entries,
              navigator ->
              LogsList(entries) { navigator.finish(LogsShareResult.SHARE) }
            }
          )
        when (result) {
          LogsShareResult.SHARE -> {
            eventSink(DebugSettingsScreen.Event.ShareLogs)
          }
          LogsShareResult.DISMISS -> {
            eventSink(DebugSettingsScreen.Event.ToggleLogs(show = false))
          }
        }
      }
    }

    CatchUpTheme(useDarkTheme = true) {
      Surface(Modifier.fillMaxHeight()) {
        LazyColumn(verticalArrangement = spacedBy(8.dp), modifier = Modifier.statusBarsPadding()) {
          item(key = "header") {
            Row(
              Modifier.fillMaxWidth().padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.End
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
                modifier = Modifier.size(48.dp)
              )
            }
          }

          state.items.forEachIndexed { index, item ->
            when (item) {
              is DebugItem.Header -> {
                item(index) { DebugSectionHeader(item.title) }
              }
              is DebugItem.Element -> {
                item(index) {
                  val scope = rememberCoroutineScope()
                  DebugElementContent(
                    item,
                    { screen -> eventSink(DebugSettingsScreen.Event.NavigateTo(screen)) }
                  ) { key, value ->
                    scope.launch {
                      debugPreferences.edit { prefs ->
                        @Suppress("UNCHECKED_CAST")
                        prefs[key as Preferences.Key<Any?>] = value
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
    element: DebugItem.Element,
    onNavigate: (Screen) -> Unit,
    onUpdate: (Preferences.Key<*>, Any?) -> Unit,
  ) {
    @Suppress("UNCHECKED_CAST")
    when (element) {
      is DebugItem.Element.ButtonElement -> {
        element.Content { screen -> onNavigate(screen) }
      }
      is DebugItem.Element.SpinnerElement<*> -> {
        // TODO this is really... ugly
        val state: SettingValueState<*> =
          when (element.valueType) {
            ValueType.INT -> {
              val typedElement = element as DebugItem.Element.SpinnerElement<Int>
              rememberIntSettingState(
                typedElement.key,
                typedElement.options[typedElement.defaultOptionIndex],
              )
            }
            ValueType.LONG -> {
              val typedElement = element as DebugItem.Element.SpinnerElement<Long>
              rememberLongSettingState(
                typedElement.key,
                typedElement.options[typedElement.defaultOptionIndex],
              )
            }
            ValueType.STRING -> {
              val typedElement = element as DebugItem.Element.SpinnerElement<String>
              rememberStringSettingState(
                typedElement.key,
                typedElement.options[typedElement.defaultOptionIndex],
              )
            }
          }
        (element as DebugItem.Element.SpinnerElement<Any>).Content(
          state as SettingValueState<Any>
        ) { newValue ->
          onUpdate(element.key, newValue)
        }
      }
      is DebugItem.Element.SwitchElement -> {
        element.Content(
          rememberBooleanSettingState(
            key = element.key,
            defaultValue = element.defaultValue,
          )
        ) { newValue ->
          onUpdate(element.key, newValue)
        }
      }
      is DebugItem.Element.ValueElement -> {
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
    data class SpinnerElement<T>(
      val title: String,
      val key: Preferences.Key<T>,
      val options: List<T>,
      val valueType: ValueType<T>,
      val defaultOptionIndex: Int = 0,
      val formatSelection: (T) -> String = { it.toString() },
    ) : Element {
      sealed interface ValueType<T> {
        object STRING : ValueType<String>
        object INT : ValueType<Int>
        object LONG : ValueType<Long>
      }

      @OptIn(ExperimentalMaterial3Api::class)
      @Composable
      fun Content(state: SettingValueState<T>, onSelection: (T) -> Unit) {
        var storageValue by state
        val update: (T) -> Unit = { newValue ->
          storageValue = newValue
          onSelection(storageValue)
        }
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          DebugLabelText(title, modifier = Modifier.weight(1f))

          var expanded by remember { mutableStateOf(false) }
          ExposedDropdownMenuBox(
            modifier = Modifier.weight(1f),
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
          ) {
            OutlinedTextField(
              value = formatSelection(storageValue),
              modifier = Modifier.menuAnchor(),
              textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
              onValueChange = {},
              readOnly = true,
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            )
            ExposedDropdownMenu(
              expanded = expanded,
              onDismissRequest = { expanded = false },
            ) {
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
                  }
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
    ) : Element {
      @Composable
      fun Content(onClick: (Screen) -> Unit) {
        Button(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
          onClick = { onClick(onClickScreen) }
        ) {
          Text(text)
        }
      }
    }

    data class SwitchElement(
      val title: String,
      val key: Preferences.Key<Boolean>,
      val defaultValue: Boolean,
    ) : Element {
      @Composable
      fun Content(state: SettingValueState<Boolean>, onCheckedChange: (Boolean) -> Unit) {
        var storageValue by state
        val update: (Boolean) -> Unit = { boolean ->
          storageValue = boolean
          onCheckedChange(storageValue)
        }
        Row(
          modifier =
            Modifier.toggleable(
                value = storageValue,
                role = Role.Checkbox,
                onValueChange = { update(!storageValue) }
              )
              .padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          DebugLabelText(title)
          Spacer(modifier = Modifier.weight(1f))
          Switch(checked = storageValue, onCheckedChange = update)
        }
      }
    }
    data class ValueElement(
      val title: String,
      val value: String, // Are any of these live?
    ) : Element {
      @Composable
      fun Content() {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically
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
      modifier = Modifier.padding(horizontal = 12.dp)
    )
    Spacer(Modifier.height(2.dp))
    Divider()
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
