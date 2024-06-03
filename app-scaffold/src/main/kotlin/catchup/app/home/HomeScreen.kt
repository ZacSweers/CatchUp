package catchup.app.home

import androidx.activity.compose.ReportDrawnWhen
import androidx.annotation.ColorRes
import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.window.layout.FoldingFeature
import catchup.app.CatchUpPreferences
import catchup.app.changes.ChangelogHelper
import catchup.app.home.HomeScreen.Event.NestedNavEvent
import catchup.app.home.HomeScreen.Event.OpenBookmarks
import catchup.app.home.HomeScreen.Event.OpenSettings
import catchup.app.home.HomeScreen.Event.Selected
import catchup.app.home.HomeScreen.Event.ShowChangelog
import catchup.app.home.HomeScreen.State
import catchup.app.service.ServiceScreen
import catchup.app.service.bookmarks.Bookmark
import catchup.app.service.bookmarks.BookmarksScreen
import catchup.app.ui.activity.SettingsScreen
import catchup.base.ui.rememberSystemBarColorController
import catchup.bookmarks.BookmarkRepository
import catchup.compose.LocalDisplayFeatures
import catchup.compose.LocalDynamicTheme
import catchup.compose.LocalScrollToTop
import catchup.compose.MutableScrollToTop
import catchup.compose.Wigglable
import catchup.compose.rememberStableCoroutineScope
import catchup.deeplink.DeepLinkable
import catchup.di.AppScope
import catchup.service.api.ServiceMeta
import catchup.util.toDayContext
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import com.google.accompanist.adaptive.TwoPane
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.foundation.NavEvent
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.onNavEvent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.retained.collectAsRetainedState
import com.slack.circuit.retained.produceRetainedState
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.overlays.BottomSheetOverlay
import com.squareup.anvil.annotations.ContributesMultibinding
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.multibindings.StringKey
import dev.zacsweers.catchup.app.scaffold.R as AppScaffoldR
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

// TODO generalize metas to allow dynamic ones, like settings/bookmarks
@ContributesMultibinding(AppScope::class, boundType = DeepLinkable::class)
@StringKey("home")
@Parcelize
object HomeScreen : Screen, DeepLinkable {
  override fun createScreen(queryParams: ImmutableMap<String, List<String?>>): Screen = HomeScreen

  data class State(
    val serviceMetas: ImmutableList<ServiceMeta>,
    val changelogAvailable: Boolean,
    val selectedIndex: Int,
    val bookmarksCount: Long,
    val eventSink: (Event) -> Unit = {},
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    data object OpenSettings : Event

    data object OpenBookmarks : Event

    data object ShowChangelog : Event

    data class NestedNavEvent(val navEvent: NavEvent) : Event

    data class Selected(val index: Int) : Event
  }
}

class HomePresenter
@AssistedInject
constructor(
  @Assisted private val navigator: Navigator,
  private val serviceMetaMap: Map<String, ServiceMeta>,
  private val catchUpPreferences: CatchUpPreferences,
  private val changelogHelper: ChangelogHelper,
  private val bookmarkRepository: BookmarkRepository,
) : Presenter<State> {

  @CircuitInject(HomeScreen::class, AppScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(navigator: Navigator): HomePresenter
  }

  @Composable
  override fun present(): State {
    val currentOrder by rememberRetained { catchUpPreferences.servicesOrder }.collectAsState()
    var selectedIndex by rememberRetained(currentOrder) { mutableIntStateOf(0) }
    val serviceMetas by
      produceRetainedState(initialValue = persistentListOf(), currentOrder) {
        // TODO make enabledPrefKey live?
        check(serviceMetaMap.isNotEmpty()) { "No services found!" }
        value =
          serviceMetaMap.values
            .filter(ServiceMeta::enabled)
            .filter { serviceMeta ->
              catchUpPreferences.datastore.data
                .map { it[booleanPreferencesKey(serviceMeta.enabledPreferenceKey)] ?: true }
                .first()
            }
            .sortedBy { currentOrder.orEmpty().indexOf(it.id) }
            .toImmutableList()
      }
    val context = LocalContext.current
    val changelogAvailable by changelogHelper.changelogAvailable(context).collectAsState(false)

    val countFlow = rememberRetained { bookmarkRepository.bookmarksCountFlow() }
    val bookmarksCount by countFlow.collectAsRetainedState(0L)

    val scope = rememberStableCoroutineScope()
    val overlayHost = LocalOverlayHost.current
    return State(
      serviceMetas = serviceMetas,
      changelogAvailable = changelogAvailable,
      selectedIndex = selectedIndex,
      bookmarksCount = bookmarksCount,
    ) { event ->
      when (event) {
        OpenSettings -> {
          navigator.goTo(SettingsScreen())
        }
        OpenBookmarks -> {
          navigator.goTo(BookmarksScreen)
        }
        is NestedNavEvent -> {
          navigator.onNavEvent(event.navEvent)
        }
        is Selected -> {
          selectedIndex = event.index
          // TODO only do this if we make a TwoPane nav-aware
          //  navigator.goTo(ServiceScreen(serviceMetas[event.index].id))
        }
        ShowChangelog -> {
          scope.launch {
            overlayHost.show(
              BottomSheetOverlay(
                model = Unit,
                content = { _, _ -> changelogHelper.Content() },
                onDismiss = { Unit },
              )
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@CircuitInject(HomeScreen::class, AppScope::class)
fun Home(state: State, modifier: Modifier = Modifier) {
  if (state.serviceMetas.isEmpty()) return // Not loaded yet

  // TODO movable for current content? How can we better save the state of the current detail
  val displayFeatures = LocalDisplayFeatures.current
  val foldingFeature =
    remember(displayFeatures) { displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull() }

  if (foldingFeature != null) {
    // TODO
    //  try a PaneledCircuitContent where it's just a row of the backstack?

    TwoPane(
      first = {
        Box {
          HomeList(state)
          VerticalDivider(Modifier.align(Alignment.CenterEnd), thickness = Dp.Hairline)
        }
      },
      // TODO animate content changes, ideally same as nav decoration
      second = {
        // TODO
        //  should probably just synthesize putting the settings in the list
        //  crossfade?
        // TODO key is necessary for nested nav to work for some reason? Otherwise only works once
        key(state.selectedIndex) {
          val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
          var showTopBar by remember { mutableStateOf(true) }
          // Embed the content in a scaffold for padding and such
          Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
              AnimatedVisibility(showTopBar) {
                TopAppBar(
                  title = {
                    val title =
                      if (state.selectedIndex == state.serviceMetas.size) {
                        stringResource(AppScaffoldR.string.title_activity_settings)
                      } else {
                        val meta = state.serviceMetas[state.selectedIndex]
                        stringResource(meta.name)
                      }
                    Text(title, fontWeight = FontWeight.Black)
                  },
                  scrollBehavior = scrollBehavior,
                  colors = topAppBarColors(),
                )
              }
            },
          ) { innerPadding ->
            val screen =
              remember(state.selectedIndex) {
                if (state.selectedIndex == state.serviceMetas.size) {
                  SettingsScreen(showTopAppBar = false)
                } else {
                  val meta = state.serviceMetas[state.selectedIndex]
                  ServiceScreen(meta.id)
                }
              }
            val nestedBackStack = rememberSaveableBackStack(screen)
            val nestedNavigator = rememberCircuitNavigator(nestedBackStack)
            showTopBar = nestedBackStack.size == 1
            NavigableCircuitContent(
              nestedNavigator,
              backStack = nestedBackStack,
              modifier = Modifier.padding(innerPadding),
            )
          }
        }
      },
      strategy = { density, layoutDirection, layoutCoordinates ->
        // Split vertically if the height is larger than the width
        if (layoutCoordinates.size.height >= layoutCoordinates.size.width) {
            HorizontalTwoPaneStrategy(splitFraction = 0.4f)
          } else {
            HorizontalTwoPaneStrategy(splitFraction = 0.5f)
          }
          .calculateSplitResult(density, layoutDirection, layoutCoordinates)
      },
      displayFeatures = displayFeatures,
      modifier = modifier,
    )
  } else {
    HomePager(state, modifier)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePager(state: State, modifier: Modifier = Modifier) {
  if (state.serviceMetas.isEmpty()) return // Not loaded yet

  val pagerState =
    key(state.serviceMetas) { rememberPagerState(state.selectedIndex) { state.serviceMetas.size } }

  LaunchedEffect(pagerState) {
    snapshotFlow { pagerState.settledPage }
      .distinctUntilChanged()
      .collect { state.eventSink(Selected(it)) }
  }

  val currentServiceMeta = state.serviceMetas[pagerState.currentPage]
  val title = stringResource(currentServiceMeta.name)
  val systemUiController = rememberSystemBarColorController()

  val dynamicTheme = LocalDynamicTheme.current
  val dayOnlyColorCache = rememberColorCache(state.serviceMetas, dayOnly = !dynamicTheme)
  val tabLayoutColor = remember(dayOnlyColorCache) { Animatable(dayOnlyColorCache[0]) }
  var isAnimatingColor by remember { mutableStateOf(false) }
  val surfaceColor = MaterialTheme.colorScheme.surface
  var scrimColor by remember { mutableStateOf(surfaceColor) }

  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
  if (!dynamicTheme) {
    // Set the status bar color to match the top app bar when it's collapsed
    LaunchedEffect(scrollBehavior) {
      snapshotFlow { scrollBehavior.state.collapsedFraction }
        .collect { fraction ->
          scrimColor = lerp(surfaceColor, tabLayoutColor.value, fraction)
          if (fraction == 1.0f) {
            systemUiController.statusBarDarkContentEnabled = !scrimColor.isDark
          } else {
            systemUiController.statusBarDarkContentEnabled = !surfaceColor.isDark
          }
        }
    }

    // Transition the color from one
    LaunchedEffect(pagerState) {
      snapshotFlow { pagerState.currentPageOffsetFraction }
        .filterNot { isAnimatingColor }
        .collect { offset ->
          val position = pagerState.currentPage
          val settledPosition = pagerState.settledPage
          val nextPosition = position + offset.sign.toInt()
          val pageDiff = position - settledPosition
          val targetPage =
            if (pageDiff != 0) {
              position
            } else {
              position + offset.sign.toInt()
            }

          // Coerce because HorizontalPager sometimes jumps multiple pages and it makes the rest
          // of
          // this code sad
          // https://issuetracker.google.com/issues/264602921
          val adjustedOffset =
            (((nextPosition - position) * offset.absoluteValue + position) - settledPosition)
              .absoluteValue
              .coerceIn(0f, 1f)
          val color =
            lerp(dayOnlyColorCache[settledPosition], dayOnlyColorCache[targetPage], adjustedOffset)
          tabLayoutColor.animateTo(color, tween(0))
        }
    }
  }

  val nestedScrollModifier = remember {
    modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
  }
  val serviceMetas by rememberUpdatedState(state.serviceMetas)
  val eventSink by rememberUpdatedState(state.eventSink)
  Scaffold(
    modifier = nestedScrollModifier,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      TopAppBar(
        title = { Text(title) },
        scrollBehavior = scrollBehavior,
        colors =
          topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = scrimColor),
        actions = {
          // TODO wire with Syllabus
          if (state.changelogAvailable) {
            ChangelogButton { eventSink(ShowChangelog) }
          }

          AnimatedVisibility(state.bookmarksCount > 0, enter = fadeIn(), exit = fadeOut()) {
            Wigglable(state.bookmarksCount, shouldWiggle = { old, new -> new > old }) {
              IconButton(onClick = { eventSink(OpenBookmarks) }) {
                Icon(imageVector = Icons.Filled.Bookmark, contentDescription = "Bookmarks")
              }
            }
          }
          IconButton(onClick = { eventSink(OpenSettings) }) {
            Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
          }
        },
      )
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding)
    ) {
      val scrollToTop = remember { MutableScrollToTop() }
      val contentColor =
        if (dynamicTheme) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
      val containerColor =
        if (dynamicTheme) MaterialTheme.colorScheme.primaryContainer else tabLayoutColor.value
      val selectedTabIndex = pagerState.settledPage
      PrimaryScrollableTabRow(
        // Our selected tab is our current page
        selectedTabIndex = selectedTabIndex,
        contentColor = contentColor,
        containerColor = containerColor,
        // Good lord M3's default values for this are ugly
        edgePadding = 0.dp,
        divider = {},
        indicator = {
          TabRowDefaults.PrimaryIndicator(
            color = contentColor,
            modifier = Modifier.tabIndicatorOffset(selectedTabIndex, matchContentSize = true),
            width = Dp.Unspecified,
          )
        },
      ) {
        // Add tabs for all of our pages
        val coroutineScope = rememberStableCoroutineScope()
        serviceMetas.forEachIndexed { index, serviceMeta ->
          Tab(
            icon = {
              Icon(
                imageVector = ImageVector.vectorResource(serviceMeta.icon),
                contentDescription = stringResource(serviceMeta.name),
              )
            },
            selected = pagerState.currentPage == index,
            onClick = {
              coroutineScope.launch {
                if (index == pagerState.currentPage) {
                  scrollToTop.emit()
                } else {
                  isAnimatingColor = true
                  val tabLayoutDeferred =
                    if (!dynamicTheme) {
                      async { tabLayoutColor.animateTo(dayOnlyColorCache[index]) }
                    } else {
                      CompletableDeferred(Unit)
                    }
                  awaitAll(async { pagerState.animateScrollToPage(index) }, tabLayoutDeferred)
                  isAnimatingColor = false
                }
              }
            },
          )
        }
      }
      var contentComposed by remember { mutableStateOf(false) }

      HorizontalPager(
        modifier = Modifier.weight(1f),
        beyondViewportPageCount = 1,
        key = { serviceMetas[it].id },
        state = pagerState,
        verticalAlignment = Alignment.Top,
      ) { page ->
        contentComposed = true
        CompositionLocalProvider(
          LocalScrollToTop provides scrollToTop.takeIf { pagerState.currentPage == page }
        ) {
          CircuitContent(
            screen = ServiceScreen(serviceMetas[page].id),
            onNavEvent = { eventSink(NestedNavEvent(it)) },
          )
        }
      }
      ReportDrawnWhen { contentComposed }
    }
  }
}

@JvmInline
private value class ColorCache(private val colors: Array<Color>) {
  operator fun get(index: Int): Color = colors[index]

  operator fun set(index: Int, color: Color) {
    colors[index] = color
  }
}

@Composable
private fun rememberColorCache(
  serviceMetas: ImmutableList<ServiceMeta>,
  dayOnly: Boolean,
): ColorCache {
  // TODO consolidate this with dynamicAwareColor
  val context = LocalContext.current
  val primaryColor = MaterialTheme.colorScheme.primary
  val dynamicTheme = LocalDynamicTheme.current
  return remember(context, serviceMetas) {
    val contextToUse =
      if (dayOnly) {
        context.toDayContext()
      } else {
        context
      }
    val colors =
      Array(serviceMetas.size) { index ->
        @ColorRes val colorRes = serviceMetas[index].themeColor
        if (dynamicTheme) {
          primaryColor
        } else {
          Color(contextToUse.getColor(colorRes))
        }
      }
    ColorCache(colors)
  }
}

private val Color.isDark: Boolean
  get() = luminance() <= 0.5f

@Composable
internal fun ChangelogButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
  IconButton(onClick = onClick, modifier = modifier) {
    Icon(
      imageVector = ImageVector.vectorResource(AppScaffoldR.drawable.baseline_redeem_24),
      contentDescription = stringResource(AppScaffoldR.string.changes),
    )
  }
}
