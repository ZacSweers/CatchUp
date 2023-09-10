package catchup.app.service

import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import catchup.bookmarks.BookmarkRepository
import catchup.di.AppScope
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.parcelize.Parcelize

@Parcelize
data class BookmarkIconScreen(val id: Long, val themeColor: Int) : Screen {
  data class State(val isBookmarked: Boolean, val themeColor: Color, val toggle: () -> Unit) :
    CircuitUiState
}

class BookmarkIconPresenter
@AssistedInject
constructor(
  @Assisted private val screen: BookmarkIconScreen,
  private val bookmarkRepository: BookmarkRepository,
) : Presenter<BookmarkIconScreen.State> {
  private val themeColor = Color(screen.themeColor)

  @Composable
  override fun present(): BookmarkIconScreen.State {
    val isBookmarked by bookmarkRepository.isBookmarked(screen.id).collectAsState(false)
    return BookmarkIconScreen.State(isBookmarked, themeColor) {
      if (isBookmarked) {
        bookmarkRepository.removeBookmark(screen.id)
      } else {
        bookmarkRepository.addBookmark(screen.id)
      }
    }
  }

  @CircuitInject(BookmarkIconScreen::class, AppScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(screen: BookmarkIconScreen): BookmarkIconPresenter
  }
}

@CircuitInject(BookmarkIconScreen::class, AppScope::class)
@Composable
fun BookmarkIcon(state: BookmarkIconScreen.State, modifier: Modifier = Modifier) {
  val icon =
    if (state.isBookmarked) {
      Filled.Bookmark
    } else {
      Outlined.Bookmark
    }
  TextActionItem(
    icon = icon,
    tint = state.themeColor,
    contentDescription = "Bookmark",
    modifier = modifier
  ) {
    state.toggle()
  }
}
