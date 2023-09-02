package dev.zacsweers.catchup.circuit

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.foundation.NavEvent
import com.slack.circuit.foundation.onNavEvent
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Screen
import com.slack.circuit.runtime.presenter.Presenter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dev.zacsweers.catchup.di.AppScope
import kotlinx.parcelize.Parcelize

@Parcelize
data class ScaffoldScreen(val title: String, val screen: Screen) : Screen {
  data class State(val title: String, val screen: Screen, val eventSink: (Event) -> Unit) :
    CircuitUiState

  data class Event(val navEvent: NavEvent) : CircuitUiEvent
}

class ScaffoldPresenter
@AssistedInject
constructor(
  @Assisted val screen: ScaffoldScreen,
  @Assisted val navigator: Navigator,
) : Presenter<ScaffoldScreen.State> {
  @Composable
  override fun present(): ScaffoldScreen.State {
    return ScaffoldScreen.State(screen.title, screen.screen) { event ->
      navigator.onNavEvent(event.navEvent)
    }
  }

  @CircuitInject(ScaffoldScreen::class, AppScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(screen: ScaffoldScreen, navigator: Navigator): ScaffoldPresenter
  }
}

@CircuitInject(ScaffoldScreen::class, AppScope::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldUi(state: ScaffoldScreen.State, modifier: Modifier = Modifier) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    containerColor = Color.Transparent,
    topBar = {
      TopAppBar(
        title = { Text(state.title, fontWeight = FontWeight.Black) },
        scrollBehavior = scrollBehavior
      )
    },
  ) { innerPadding ->
    CircuitContent(state.screen, modifier = Modifier.padding(innerPadding))
  }
}
