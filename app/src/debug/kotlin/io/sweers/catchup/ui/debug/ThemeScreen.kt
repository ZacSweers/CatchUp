package io.sweers.catchup.ui.debug

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Screen
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.catchup.compose.CatchUpTheme
import dev.zacsweers.catchup.di.AppScope
import kotlinx.parcelize.Parcelize

@Parcelize
object ThemeScreen : Screen {
  object State : CircuitUiState
}

@CircuitInject(ThemeScreen::class, AppScope::class)
@Composable
fun ThemeScreenPresenter() = ThemeScreen.State

@CircuitInject(ThemeScreen::class, AppScope::class)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThemeScreenUI(modifier: Modifier) {
  val isInDarkMode = isSystemInDarkTheme()
  var darkMode by remember { mutableStateOf(isInDarkMode) }
  CatchUpTheme(darkMode) {
    LazyColumn(modifier.fillMaxWidth().systemBarsPadding()) {
      stickyHeader {
        Row(Modifier.fillParentMaxWidth().padding(16.dp)) {
          Text("Dark mode")
          Spacer(Modifier.weight(1f))
          Switch(checked = darkMode, onCheckedChange = { darkMode = it })
        }
      }
      item {
        Column(
          Modifier.fillParentMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp)
        ) {
          Text(
            "(primaryContainer)\nonPrimaryContainer",
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
          Column(
            Modifier.fillParentMaxWidth()
              .background(MaterialTheme.colorScheme.primary)
              .padding(16.dp)
          ) {
            Text("(primary)\nonPrimary", color = MaterialTheme.colorScheme.onPrimary)
          }
        }
      }
      item {
        Column(
          Modifier.fillParentMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(16.dp)
        ) {
          Text(
            "(secondaryContainer)\nonSecondaryContainer",
            color = MaterialTheme.colorScheme.onSecondaryContainer
          )
          Column(
            Modifier.fillParentMaxWidth()
              .background(MaterialTheme.colorScheme.secondary)
              .padding(16.dp)
          ) {
            Text("(secondary)\nonSecondary", color = MaterialTheme.colorScheme.onSecondary)
          }
        }
      }
      item {
        Column(
          Modifier.fillParentMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(16.dp)
        ) {
          Text(
            "(tertiaryContainer)\nonTertiaryContainer",
            color = MaterialTheme.colorScheme.onTertiaryContainer
          )
          Column(
            Modifier.fillParentMaxWidth()
              .background(MaterialTheme.colorScheme.tertiary)
              .padding(16.dp)
          ) {
            Text("(tertiary)\nonTertiary", color = MaterialTheme.colorScheme.onTertiary)
          }
        }
      }
      item {
        Column(
          Modifier.fillParentMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp)
        ) {
          Text(
            "(errorContainer)\nonErrorContainer",
            color = MaterialTheme.colorScheme.onErrorContainer
          )
          Column(
            Modifier.fillParentMaxWidth().background(MaterialTheme.colorScheme.error).padding(16.dp)
          ) {
            Text("(error)\nonError", color = MaterialTheme.colorScheme.onError)
          }
        }
      }
      item {
        Column(
          Modifier.fillParentMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
        ) {
          Text(
            "(surfaceVariant)\nonSurfaceVariant",
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Column(
            Modifier.fillParentMaxWidth()
              .background(MaterialTheme.colorScheme.surface)
              .padding(16.dp)
          ) {
            Text("(surface)\nonSurface", color = MaterialTheme.colorScheme.onSurface)
            Text("surfaceTint", color = MaterialTheme.colorScheme.surfaceTint)
          }
        }
      }
      item {
        Column(
          Modifier.fillParentMaxWidth()
            .background(MaterialTheme.colorScheme.inverseSurface)
            .padding(16.dp)
        ) {
          Text(
            "(inverseSurface)\ninverseOnSurface",
            color = MaterialTheme.colorScheme.inverseOnSurface
          )
        }
      }
      item {
        Column {
          Divider(
            Modifier.fillParentMaxWidth(),
            thickness = 5.dp,
            color = MaterialTheme.colorScheme.outline
          )
          Divider(
            Modifier.fillParentMaxWidth(),
            thickness = 5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
          )
        }
      }
    }
  }
  // md_theme_light_background
  // md_theme_light_onBackground
  // md_theme_light_inversePrimary
  // md_theme_light_shadow
  // md_theme_light_scrim
}
