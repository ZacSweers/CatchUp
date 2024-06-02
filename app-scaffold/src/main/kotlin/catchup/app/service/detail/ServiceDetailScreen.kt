package catchup.app.service.detail

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import catchup.app.data.LinkManager
import catchup.app.service.ActionRow
import catchup.app.service.TextItemFooter
import catchup.app.service.TextItemHeader
import catchup.app.service.detail.ServiceDetailScreen.Event.OpenImage
import catchup.app.service.detail.ServiceDetailScreen.Event.OpenUrl
import catchup.app.service.detail.ServiceDetailScreen.Event.Share
import catchup.app.service.detail.ServiceDetailScreen.Event.ToggleCollapse
import catchup.app.service.openUrl
import catchup.app.service.shareUrl
import catchup.app.ui.activity.ImageViewerScreen
import catchup.base.ui.BackPressNavButton
import catchup.base.ui.rememberSystemBarColorController
import catchup.compose.ContentAlphas
import catchup.compose.minus
import catchup.di.AppScope
import catchup.service.api.Comment
import catchup.service.api.Detail
import catchup.service.api.Service
import catchup.unfurler.UnfurlResult
import catchup.util.injection.qualifiers.ApplicationContext
import catchup.util.kotlin.format
import coil.compose.AsyncImage
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.mikepenz.markdown.model.MarkdownColors
import com.mikepenz.markdown.model.MarkdownTypography
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
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.parcelize.Parcelize

@Parcelize
data class ServiceDetailScreen(
  val serviceId: String,
  val itemId: Long,
  val id: String,
  val title: String,
  val text: String?,
  val imageUrl: String?,
  val linkUrl: String?,
  val score: Long?,
  val commentsCount: Int?,
  val tag: String?,
  val author: String?,
  val timestamp: Long?,
) : Screen {
  data class State(
    val detail: Detail,
    val unfurl: UnfurlResult?,
    val themeColor: Color,
    val collapsedItems: Set<String> = emptySet(),
    val eventSink: (Event) -> Unit = {},
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    data object OpenImage : Event

    data object OpenUrl : Event

    data object Share : Event

    data class ToggleCollapse(val commentId: String) : Event
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
        id = screen.id,
        itemId = screen.itemId,
        title = screen.title,
        text = screen.text,
        imageUrl = screen.imageUrl,
        score = screen.score,
        tag = screen.tag,
        author = screen.author,
        timestamp = screen.timestamp?.let(Instant::fromEpochMilliseconds),
      ),
      null,
      themeColor,
    )

  @Composable
  override fun present(): ServiceDetailScreen.State {
    val detailRepository = rememberRetained {
      detailRepoFactory.create(screen.itemId, screen.serviceId)
    }
    val collapsedItems = rememberRetained { mutableStateMapOf<String, Unit>() }
    val detailFlow = rememberRetained(init = detailRepository::loadDetail)
    val compositeDetail by detailFlow.collectAsRetainedState(initial = null)
    val scope = rememberStableCoroutineScope()
    val overlayHost = LocalOverlayHost.current
    val (detail, unfurl) = compositeDetail ?: return initialState
    val filteredDetail by
      rememberRetained(detail, collapsedItems) {
        derivedStateOf {
          when (detail) {
            is Detail.Shallow -> detail
            is Detail.Full -> {
              detail.copy(
                comments =
                  persistentListOf<Comment>().mutate {
                    var collapsedDepth = -1
                    for (comment in detail.comments) {
                      if (collapsedDepth != -1 && comment.depth >= collapsedDepth) {
                        continue
                      } else if (comment.id in collapsedItems) {
                        collapsedDepth = comment.depth + 1
                        it.add(comment)
                      } else {
                        collapsedDepth = -1
                        it.add(comment)
                      }
                    }
                  }
              )
            }
          }
        }
      }
    return ServiceDetailScreen.State(
      detail = filteredDetail,
      unfurl = unfurl.takeUnless { !filteredDetail.allowUnfurl },
      themeColor = themeColor,
      collapsedItems = collapsedItems.keys,
    ) { event ->
      when (event) {
        OpenImage -> {
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
        OpenUrl -> {
          scope.launch { linkManager.openUrl(detail.linkUrl!!) }
        }
        is ToggleCollapse -> {
          val commentId = event.commentId
          if (commentId in collapsedItems) {
            collapsedItems -= commentId
          } else {
            collapsedItems[commentId] = Unit
          }
        }
        Share -> {
          linkManager.shareUrl(detail.shareUrl!!, detail.title)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(ServiceDetailScreen::class, AppScope::class)
@Composable
fun DetailUi(state: ServiceDetailScreen.State, modifier: Modifier = Modifier) {
  // TODO this is a bit awkward, maybe we should have some middleware that resets
  //  status bar colors on every screen change?
  val systemBarColorController = rememberSystemBarColorController()
  systemBarColorController.systemBarsDarkContentEnabled = !isSystemInDarkTheme()
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      CenterAlignedTopAppBar(
        navigationIcon = { BackPressNavButton() },
        title = {
          val formattedScore =
            remember(state.detail.commentsCount) {
              "${state.detail.commentsCount?.toLong()?.format() ?: "No"} comments"
            }
          // TODO animation is clipped
          AnimatedContent(scrollBehavior.state.overlappedFraction != 0f, label = "AppBar Title") {
            scrolled ->
            if (scrolled) {
              Text(text = formattedScore)
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
  LazyColumn(modifier = modifier) {
    item(key = "header", contentType = "header") { HeaderItem(state, Modifier.animateItem()) }

    val numComments =
      when (state.detail) {
        is Detail.Shallow -> -1
        is Detail.Full -> state.detail.comments.size
      }

    when (numComments) {
      -1 -> {
        item(key = "loading", contentType = "loading") {
          Box(Modifier.fillParentMaxSize().animateItem(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = state.themeColor)
          }
        }
      }
      0 -> {
        item(key = "empty", contentType = "empty") {
          Box(
            modifier = Modifier.animateItem().padding(16.dp).fillMaxWidth(),
            contentAlignment = Alignment.Center,
          ) {
            Text("No comments 📭")
          }
        }
      }
      else -> {
        items(
          count = numComments,
          key = { i ->
            // Ensure stable keys so animations look good
            (state.detail as Detail.Full).comments[i].id
          },
          contentType = { "comment" },
        ) { index ->
          val comment = (state.detail as Detail.Full).comments[index]
          CommentItem(
            comment,
            isCollapsed = comment.id in state.collapsedItems,
            themeColor = state.themeColor,
            modifier = Modifier.animateItem(),
          ) {
            state.eventSink(ToggleCollapse(comment.id))
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
          modifier = Modifier.fillMaxWidth().clickable { state.eventSink(OpenImage) },
          contentScale = ContentScale.FillWidth,
        )
        Spacer(modifier = Modifier.height(8.dp))
      }
      Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = spacedBy(8.dp),
      ) {
        val detail = state.detail
        Text(detail.title, style = MaterialTheme.typography.titleMedium)
        detail.text?.takeUnless(String::isBlank)?.let {
          HorizontalDivider(thickness = Dp.Hairline)
          Markdown(
            it,
            typography = catchupMarkdownTypography(),
            colors = catchupMarkdownColors(state.themeColor),
          )
        }

        state.unfurl?.let { UnfurlItem(detail.title, it) { state.eventSink(OpenUrl) } }

        TextItemHeader(
          score = detail.score,
          tag = detail.tag,
          tagHintColor = state.themeColor,
          timestamp = detail.timestamp,
        )
        TextItemFooter(author = detail.author, source = null)

        HorizontalDivider(thickness = Dp.Hairline)

        // Action buttons
        ActionRow(
          itemId = state.detail.itemId,
          themeColor = state.themeColor,
          onShareClick = { state.eventSink(Share) },
        )
      }
      HorizontalDivider(thickness = Dp.Hairline)
    }
  }
}

@Composable
private fun CommentItem(
  comment: Comment,
  isCollapsed: Boolean,
  modifier: Modifier = Modifier,
  themeColor: Color = MaterialTheme.colorScheme.tertiary,
  onToggleCollapse: () -> Unit,
) {
  // Disable minimum height for a clickable surface as we're ok with it going smaller here. Without
  // this, there ends up being a lot of padding around the clickable area.
  CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
    Surface(modifier = modifier, onClick = onToggleCollapse) {
      Box {
        val startPadding = 8.dp * comment.depth
        Column(
          modifier =
            Modifier.padding(start = startPadding + 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
              .animateContentSize()
        ) {
          Row {
            val commonModifier =
              if (isCollapsed) Modifier.align(Alignment.CenterVertically) else Modifier
            Text(
              comment.author,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlphas.Medium),
              modifier = commonModifier,
            )
            Text(
              " | ${comment.score.toLong().format()}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlphas.Disabled),
              modifier = commonModifier,
            )
            if (isCollapsed) {
              // TODO show number of children comments?
              // TODO animate/crossfade this somehow with timestamp?
              Spacer(Modifier.weight(1f))
              Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Expand",
                modifier = commonModifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlphas.Disabled),
              )
            } else {
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
              Text(
                formattedTimestamp,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlphas.Disabled),
              )
            }
          }
          if (!isCollapsed) {
            Spacer(Modifier.height(4.dp))
            Markdown(
              comment.text,
              colors = catchupMarkdownColors(themeColor),
              typography = catchupMarkdownTypography(),
            )
            if (comment.clickableUrls.isNotEmpty()) {
              Spacer(Modifier.height(4.dp))
              // TODO refine this UI. Currently unsupported though
              Column(verticalArrangement = spacedBy(4.dp)) {
                val uriHandler = LocalUriHandler.current
                for (url in comment.clickableUrls) {
                  OutlinedButton(onClick = { uriHandler.openUri(url.url) }) { Text(url.text) }
                }
              }
            }
          }
        }
        HorizontalDivider(
          modifier = Modifier.padding(start = startPadding).align(Alignment.BottomCenter),
          thickness = Dp.Hairline,
        )
      }
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
    val title = unfurl.title ?: unfurl.url
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
        UnfurlText(title, showTitle = title != parentTitle, unfurl.description, unfurl.domain)
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
          unfurl.domain,
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
  domain: String?,
  modifier: Modifier = Modifier,
) {
  Column(modifier.fillMaxWidth()) {
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
    domain?.let {
      Text(
        it,
        modifier = Modifier.align(Alignment.End),
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlphas.Disabled),
      )
    }
  }
}

@Composable
fun catchupMarkdownColors(linkColor: Color): MarkdownColors {
  return markdownColor(linkText = linkColor)
}

private val defaultSmallTextSize = 12.sp

// TODO any other tunings/stylings?
@Composable
fun catchupMarkdownTypography(
  seedSize: TextUnit = defaultSmallTextSize,
  h1: TextStyle = MaterialTheme.typography.titleLarge.copy(fontSize = seedSize * 2.0f),
  h2: TextStyle = MaterialTheme.typography.titleLarge.copy(fontSize = seedSize * 1.8f),
  h3: TextStyle = MaterialTheme.typography.titleLarge.copy(fontSize = seedSize * 1.6f),
  h4: TextStyle = MaterialTheme.typography.titleLarge.copy(fontSize = seedSize * 1.4f),
  h5: TextStyle = MaterialTheme.typography.titleLarge.copy(fontSize = seedSize * 1f),
  h6: TextStyle = MaterialTheme.typography.titleLarge.copy(fontSize = seedSize * 0.85f),
  text: TextStyle = MaterialTheme.typography.bodySmall,
  code: TextStyle =
    MaterialTheme.typography.bodySmall.copy(
      fontFamily = FontFamily.Monospace,
      fontSize = seedSize - 1.sp,
    ),
  quote: TextStyle =
    MaterialTheme.typography.bodySmall
      .copy(fontSize = seedSize - 1.sp)
      .plus(SpanStyle(fontStyle = FontStyle.Italic)),
  paragraph: TextStyle = MaterialTheme.typography.bodySmall.copy(fontSize = seedSize),
  ordered: TextStyle = MaterialTheme.typography.bodySmall.copy(fontSize = seedSize),
  bullet: TextStyle = MaterialTheme.typography.bodySmall.copy(fontSize = seedSize),
  list: TextStyle = MaterialTheme.typography.bodySmall.copy(fontSize = seedSize),
): MarkdownTypography =
  DefaultMarkdownTypography(
    h1 = h1,
    h2 = h2,
    h3 = h3,
    h4 = h4,
    h5 = h5,
    h6 = h6,
    text = text,
    quote = quote,
    code = code,
    paragraph = paragraph,
    ordered = ordered,
    bullet = bullet,
    list = list,
  )
