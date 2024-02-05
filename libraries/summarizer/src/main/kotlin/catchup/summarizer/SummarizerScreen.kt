package catchup.summarizer

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
import catchup.base.ui.BackPressNavButton
import catchup.base.ui.NavButtonType.CLOSE
import catchup.base.ui.rememberSystemBarColorController
import catchup.di.AppScope
import catchup.summarizer.SummarizerResult.Error
import catchup.summarizer.SummarizerResult.NotFound
import catchup.summarizer.SummarizerResult.Success
import catchup.summarizer.SummarizerResult.Unavailable
import catchup.summarizer.SummarizerScreen.State
import catchup.summarizer.SummarizerScreen.State.Loading
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.parcelize.Parcelize

@Parcelize
data class SummarizerScreen(val title: String, val url: String) : Screen {
  sealed interface State : CircuitUiState {
    val title: String

    data class Loading(override val title: String) : State

    data class Error(override val title: String, val url: String, val message: String) : State

    data class Success(override val title: String, val summary: String) : State
  }
}

private fun SummarizerResult.toState(title: String, url: String): State {
  return when (this) {
    is Success -> State.Success(title, summary)
    is NotFound -> State.Error(title, url, "Unable to summarize this.")
    is Unavailable -> State.Error(title, url, "Summarization not available.")
    is Error -> State.Error(title, url, message)
  }
}

class SummarizerPresenter
@AssistedInject
constructor(
  @Assisted private val screen: SummarizerScreen,
  private val repository: SummarizerRepository,
) : Presenter<State> {

  @CircuitInject(SummarizerScreen::class, AppScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(screen: SummarizerScreen): SummarizerPresenter
  }

  @Composable
  override fun present(): State {
    val summary by
      produceState<State>(Loading(screen.title)) {
        value = repository.getSummarization(screen.url).toState(screen.title, screen.url)
      }
    return summary
  }
}

@CircuitInject(SummarizerScreen::class, AppScope::class)
@Composable
fun Summarizer(state: State, modifier: Modifier = Modifier) {
  val sysUi = rememberSystemBarColorController()
  sysUi.setSystemBarsColor(MaterialTheme.colorScheme.surface)
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
        Spacer(Modifier.height(24.dp))
        when (state) {
          is Loading -> {
            // accompanist loading?
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Summarizing...", fontStyle = FontStyle.Italic)
          }
          is State.Error -> {
            Text(
              "Error summarizing.\n${state.message}",
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.displaySmall,
            )
            Button(
              onClick = {
                // TODO
              },
              modifier = Modifier.padding(top = 16.dp),
            ) {
              Text("Open in browser")
            }
          }
          is State.Success -> {
            // TODO show a blinking cursor? Typewriter effect could be cool but probably only cool
            //  once
            Text(
              state.summary,
              textAlign = TextAlign.Justify,
              style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(24.dp))
            Text("Generated with ChatGPT", style = MaterialTheme.typography.labelMedium)
          }
        }
      }

      BackPressNavButton(Modifier.align(Alignment.TopStart).padding(16.dp), type = CLOSE)
    }
  }
}
