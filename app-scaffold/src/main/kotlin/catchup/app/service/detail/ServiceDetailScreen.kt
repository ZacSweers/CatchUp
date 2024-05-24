package catchup.app.service.detail

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import catchup.app.data.LinkManager
import catchup.app.service.ActionRow
import catchup.app.service.openUrl
import catchup.app.ui.activity.ImageViewerScreen
import catchup.base.ui.BackPressNavButton
import catchup.compose.ContentAlphas
import catchup.di.AppScope
import catchup.service.api.Comment
import catchup.service.api.Detail
import catchup.service.api.Service
import catchup.util.injection.qualifiers.ApplicationContext
import catchup.util.kotlin.format
import coil.compose.AsyncImage
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.retained.collectAsRetainedState
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.internal.rememberStableCoroutineScope
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.overlays.showFullScreenOverlay
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import me.saket.unfurl.UnfurlResult
import timber.log.Timber

@Parcelize
data class ServiceDetailScreen(
  val serviceId: String,
  val itemId: Long,
  val id: String,
  val title: String,
  val text: String?,
  val imageUrl: String?,
  val linkUrl: String?,
  val score: Int?,
  val commentsCount: Int?,
) : Screen {
  data class State(
    val detail: Detail,
    val unfurl: UnfurlResult?,
    val themeColor: Color,
    val eventSink: (Event) -> Unit = {},
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    data object OpenImage : Event

    data object OpenUrl : Event
  }
}

class ServiceDetailPresenter
@AssistedInject
constructor(
  @Assisted val screen: ServiceDetailScreen,
  @ApplicationContext private val context: Context,
  services: @JvmSuppressWildcards Map<String, Provider<Service>>,
  private val linkManager: LinkManager,
  private val detailRepoFactory: DetailRepository.Factory,
) : Presenter<ServiceDetailScreen.State> {

  @CircuitInject(ServiceDetailScreen::class, AppScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(screen: ServiceDetailScreen): ServiceDetailPresenter
  }

  private val service = services.getValue(screen.serviceId).get()
  private val themeColor = Color(context.getColor(service.meta().themeColor))
  private val initialState =
    ServiceDetailScreen.State(
      Detail.Shallow(
        screen.id,
        screen.itemId,
        screen.title,
        screen.text,
        screen.imageUrl,
        screen.score,
      ),
      null,
      themeColor,
    )

  @Composable
  override fun present(): ServiceDetailScreen.State {
    val detailRepository = rememberRetained {
      detailRepoFactory.create(screen.itemId, screen.serviceId)
    }
    val detailFlow = rememberRetained(init = detailRepository::loadDetail)
    val compositeDetail by detailFlow.collectAsRetainedState(initial = null)
    val scope = rememberStableCoroutineScope()
    val overlayHost = LocalOverlayHost.current
    val (detail, unfurl) = compositeDetail ?: return initialState
    return ServiceDetailScreen.State(detail, unfurl, themeColor) { event ->
      when (event) {
        ServiceDetailScreen.Event.OpenImage -> {
          scope.launch {
            overlayHost.showFullScreenOverlay(
              ImageViewerScreen(
                detail.imageUrl!!,
                detail.imageUrl!!,
                // TODO
                isBitmap = true,
                null,
                detail.imageUrl!!,
              )
            )
          }
        }
        ServiceDetailScreen.Event.OpenUrl -> {
          scope.launch { linkManager.openUrl(detail.linkUrl!!) }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(ServiceDetailScreen::class, AppScope::class)
@Composable
fun DetailUi(state: ServiceDetailScreen.State, modifier: Modifier = Modifier) {
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      CenterAlignedTopAppBar(
        navigationIcon = { BackPressNavButton() },
        title = {
          // TODO animation is clipped
          AnimatedContent(scrollBehavior.state.overlappedFraction != 0f, label = "AppBar Title") {
            scrolled ->
            if (scrolled) {
              Text(text = "${state.detail.commentsCount?.toString() ?: "No"} comments")
            }
          }
        },
        scrollBehavior = scrollBehavior,
      )
    },
  ) { innerPadding ->
    CommentsList(state, Modifier.padding(innerPadding))
  }
}

@Composable
private fun CommentsList(state: ServiceDetailScreen.State, modifier: Modifier = Modifier) {
  // TODO handle empty state
  LazyColumn(modifier = modifier) {
    val numComments =
      when (state.detail) {
        is Detail.Shallow -> 0
        is Detail.Full -> state.detail.comments.size
      }
    items(1 + numComments) { index ->
      if (index == 0) {
        HeaderItem(state)
      } else {
        when (state.detail) {
          is Detail.Shallow -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator()
            }
          }
          is Detail.Full -> {
            CommentItem(state.detail.comments[index - 1])
          }
        }
      }
    }
  }
}

@Composable
private fun HeaderItem(state: ServiceDetailScreen.State, modifier: Modifier = Modifier) {
  Surface(modifier = modifier.animateContentSize()) {
    Column {
      state.detail.imageUrl?.let {
        AsyncImage(
          model = it,
          contentDescription = "Image",
          modifier =
            Modifier.fillMaxWidth().clickable {
              state.eventSink(ServiceDetailScreen.Event.OpenImage)
            },
          contentScale = ContentScale.FillWidth,
        )
        Spacer(modifier = Modifier.height(8.dp))
      }
      Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = spacedBy(8.dp),
      ) {
        Text(state.detail.title, style = MaterialTheme.typography.titleMedium)
        state.detail.text?.takeUnless(String::isBlank)?.let {
          Text(it, style = MaterialTheme.typography.bodyMedium)
        }
        state.unfurl
          // TODO Improve this. Maybe just ignore self-referencing unfurls?
          ?.takeUnless { it.thumbnail == null }
          ?.let {
            UnfurlItem(state.detail.title, it) {
              state.eventSink(ServiceDetailScreen.Event.OpenUrl)
            }
          }

        // TODO metadata

        HorizontalDivider(thickness = Dp.Hairline)
        // Action buttons
        ActionRow(
          itemId = state.detail.itemId,
          themeColor = state.themeColor,
          onShareClick = {
            // TODO
          },
        )
      }
    }
  }
}

@Composable
private fun CommentItem(comment: Comment, modifier: Modifier = Modifier) {
  val startPadding = 8.dp * comment.depth
  Surface(modifier = modifier.padding(start = startPadding)) {
    Box {
      Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
        Row {
          Text(
            comment.author,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlphas.Medium),
          )
          Text(
            " | ${comment.score.toLong().format()}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlphas.Disabled),
          )
          // TODO move this formatting into presenter somehow
          val formattedTimestamp =
            remember(comment.timestamp) {
              DateUtils.getRelativeTimeSpanString(
                  comment.timestamp.toEpochMilliseconds(),
                  System.currentTimeMillis(),
                  0L,
                  DateUtils.FORMAT_ABBREV_ALL,
                )
                .toString()
            }
          // TODO markdown support
          Text(
            formattedTimestamp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlphas.Disabled),
          )
        }
        Spacer(Modifier.height(4.dp))
        Text(comment.text, style = MaterialTheme.typography.bodySmall)
        // TODO clickable links if any
      }
      HorizontalDivider(Modifier.align(Alignment.TopCenter), thickness = Dp.Hairline)
    }
  }
}

@Composable
private fun UnfurlItem(
  parentTitle: String,
  unfurl: UnfurlResult,
  modifier: Modifier = Modifier,
  onOpenUrl: () -> Unit,
) {
  ElevatedCard(modifier = modifier, onClick = onOpenUrl) {
    val thumbnail = unfurl.thumbnail
    val title = unfurl.title ?: unfurl.url.toString()
    if (thumbnail == null) {
      Row(Modifier.padding(16.dp)) {
        // TODO if thumbnail is available, show rich preview. If just favicon, show just in corner
        (unfurl.favicon)?.let {
          AsyncImage(
            model = it,
            modifier = Modifier.size(48.dp).align(Alignment.CenterVertically),
            contentDescription = "Preview",
          )
          Spacer(Modifier.width(8.dp))
        }
        UnfurlText(title, showTitle = title != parentTitle, unfurl.description)
      }
    } else {
      Column {
        AsyncImage(
          model = thumbnail,
          contentDescription = "Preview",
          modifier = Modifier.fillMaxWidth(),
          contentScale = ContentScale.FillWidth,
        )
        UnfurlText(
          title,
          showTitle = title != parentTitle,
          unfurl.description,
          modifier = Modifier.padding(16.dp),
        )
      }
    }
  }
}

@Composable
private fun UnfurlText(
  title: String,
  showTitle: Boolean,
  description: String?,
  modifier: Modifier = Modifier,
) {
  Column(modifier) {
    if (showTitle) {
      Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
    description?.let {
      Text(
        it,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlphas.Medium),
      )
    }
  }
}
