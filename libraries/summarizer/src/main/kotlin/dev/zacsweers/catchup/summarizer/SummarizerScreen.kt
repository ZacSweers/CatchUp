package dev.zacsweers.catchup.summarizer

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.codegen.annotations.CircuitInject
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dev.zacsweers.catchup.di.AppScope
import io.sweers.catchup.base.ui.BackPressNavButton
import io.sweers.catchup.base.ui.NavButtonType
import kotlinx.parcelize.Parcelize

@Parcelize
data class SummarizerScreen(val title: String, val url: String) : Screen {
  sealed interface State : CircuitUiState {
    val title: String

    data class Loading(override val title: String) : State
    data class Error(override val title: String, val url: String, val message: String) : State
    data class Success(
      override val title: String,
      val summary: String,
    ) : State
  }
}

fun SummarizerResult.toState(title: String, url: String): SummarizerScreen.State {
  return when (this) {
    is SummarizerResult.Success -> SummarizerScreen.State.Success(title, summary)
    is SummarizerResult.NotFound ->
      SummarizerScreen.State.Error(title, url, "Unable to summarize this.")
    is SummarizerResult.Error -> SummarizerScreen.State.Error(title, url, message)
  }
}

class SummarizerPresenter
@AssistedInject
constructor(
  @Assisted private val screen: SummarizerScreen,
  private val repository: SummarizerRepository,
) : Presenter<SummarizerScreen.State> {

  @CircuitInject(SummarizerScreen::class, AppScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(
      screen: SummarizerScreen,
    ): SummarizerPresenter
  }

  @Composable
  override fun present(): SummarizerScreen.State {
    val summary by
      produceState<SummarizerScreen.State>(SummarizerScreen.State.Loading(screen.title)) {
        value = repository.getSummarization(screen.url).toState(screen.title, screen.url)
      }
    return summary
  }
}

@CircuitInject(SummarizerScreen::class, AppScope::class)
@Composable
fun Summarizer(state: SummarizerScreen.State, modifier: Modifier = Modifier) {
  val sysUi = rememberSystemUiController()
  sysUi.setSystemBarsColor(MaterialTheme.colorScheme.surface)
  // TODO restore
  Surface(modifier.fillMaxSize()) {
    Box(Modifier.systemBarsPadding()) {
      Column(
        Modifier.align(Alignment.Center)
          .padding(32.dp)
          .scrollable(rememberScrollState(), Orientation.Vertical)
          .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(state.title, style = MaterialTheme.typography.displaySmall)
        when (state) {
          is SummarizerScreen.State.Loading -> {
            // accompanist loading?
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Summarizing...", fontStyle = FontStyle.Italic)
          }
          is SummarizerScreen.State.Error -> {
            Text(
              "Error summarizing.\n${state.message}",
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.displaySmall
            )
            Button(
              onClick = {
                // TODO
              },
              modifier = Modifier.padding(top = 16.dp)
            ) {
              Text("Open in browser")
            }
          }
          is SummarizerScreen.State.Success -> {
            Text(
              state.summary,
              textAlign = TextAlign.Justify,
              style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(24.dp))
            Text("Powered by ChatGPT", style = MaterialTheme.typography.labelMedium)
          }
        }
      }

      BackPressNavButton(
        Modifier.align(Alignment.TopStart).padding(16.dp),
        type = NavButtonType.CLOSE
      )
    }
  }
}
