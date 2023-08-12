package io.sweers.catchup.home

import android.content.res.Configuration
import androidx.activity.compose.ReportDrawnWhen
import androidx.annotation.ColorRes
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.foundation.NavEvent
import com.slack.circuit.foundation.onNavEvent
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Screen
import com.slack.circuit.runtime.presenter.Presenter
import com.squareup.anvil.annotations.ContributesMultibinding
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.multibindings.StringKey
import dev.zacsweers.catchup.circuit.BottomSheetOverlay
import dev.zacsweers.catchup.compose.LocalDynamicTheme
import dev.zacsweers.catchup.compose.LocalScrollToTop
import dev.zacsweers.catchup.compose.MutableScrollToTop
import dev.zacsweers.catchup.compose.rememberStableCoroutineScope
import dev.zacsweers.catchup.deeplink.DeepLinkable
import dev.zacsweers.catchup.di.AppScope
import dev.zacsweers.catchup.service.ServiceScreen
import io.sweers.catchup.CatchUpPreferences
import io.sweers.catchup.R
import io.sweers.catchup.changes.ChangelogHelper
import io.sweers.catchup.service.api.LocalServiceThemeColor
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.ui.activity.SettingsScreen
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@ContributesMultibinding(AppScope::class, boundType = DeepLinkable::class)
@StringKey("home")
@Parcelize
object HomeScreen : Screen, DeepLinkable {
  override fun createScreen(queryParams: ImmutableMap<String, List<String?>>): Screen = HomeScreen

  data class State(
    val serviceMetas: ImmutableList<ServiceMeta>,
    val changelogAvailable: Boolean,
    val eventSink: (Event) -> Unit = {}
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    data object OpenSettings : Event

    data object ShowChangelog : Event

    data class NestedNavEvent(val navEvent: NavEvent) : Event
  }
}

class HomePresenter
@AssistedInject
constructor(
  @Assisted private val navigator: Navigator,
  private val serviceMetaMap: Map<String, ServiceMeta>,
  private val catchUpPreferences: CatchUpPreferences,
  private val changelogHelper: ChangelogHelper,
) : Presenter<HomeScreen.State> {

  @CircuitInject(HomeScreen::class, AppScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(navigator: Navigator): HomePresenter
  }

  @Composable
  override fun present(): HomeScreen.State {
    val currentOrder by
      remember { catchUpPreferences.servicesOrder }.collectAsState(initial = persistentListOf())
    val serviceMetas by
      produceState(initialValue = persistentListOf(), currentOrder) {
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
            .sortedBy { currentOrder.indexOf(it.id) }
            .toImmutableList()
      }
    val context = LocalContext.current
    val changelogAvailable by changelogHelper.changelogAvailable(context).collectAsState(false)

    val scope = rememberStableCoroutineScope()
    val overlayHost = LocalOverlayHost.current
    return HomeScreen.State(
      serviceMetas = serviceMetas,
      changelogAvailable = changelogAvailable,
    ) { event ->
      when (event) {
        HomeScreen.Event.OpenSettings -> {
          navigator.goTo(SettingsScreen)
        }
        is HomeScreen.Event.NestedNavEvent -> {
          navigator.onNavEvent(event.navEvent)
        }
        HomeScreen.Event.ShowChangelog -> {
          scope.launch {
            overlayHost.show(
              BottomSheetOverlay(
                model = Unit,
                content = { _, _ -> changelogHelper.Content() },
                onDismiss = { Unit }
              )
            )
          }
        }
      }
    }
  }
}

@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalFoundationApi::class,
  ExperimentalLayoutApi::class
)
@Composable
@CircuitInject(HomeScreen::class, AppScope::class)
fun Home(state: HomeScreen.State, modifier: Modifier = Modifier) {
  if (state.serviceMetas.isEmpty()) return // Not loaded yet
  val pagerState = key(state.serviceMetas) { rememberPagerState { state.serviceMetas.size } }
  val currentServiceMeta = state.serviceMetas[pagerState.currentPage]
  val title = stringResource(currentServiceMeta.name)
  val systemUiController = rememberSystemUiController()

  val dynamicTheme = LocalDynamicTheme.current
  // TODO this isn't updating when serviceMeta order changes
  val colorCache = rememberColorCache(state.serviceMetas, dayOnly = false)
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

          // Coerce because HorizontalPager sometimes jumps multiple pages and it makes the rest of
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
  Scaffold(
    modifier = nestedScrollModifier,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    containerColor = Color.Transparent,
    topBar = {
      TopAppBar(
        title = { Text(title) },
        scrollBehavior = scrollBehavior,
        colors = topAppBarColors(scrolledContainerColor = scrimColor),
        actions = {
          // TODO wire with Syllabus
          if (state.changelogAvailable) {
            IconButton(
              onClick = { state.eventSink(HomeScreen.Event.ShowChangelog) },
            ) {
              Icon(
                imageVector = ImageVector.vectorResource(R.drawable.tips_and_updates),
                contentDescription = stringResource(R.string.changes),
              )
            }
          }
          IconButton(
            onClick = { state.eventSink(HomeScreen.Event.OpenSettings) },
          ) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "Settings",
            )
          }
        }
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
      ScrollableTabRow(
        // Our selected tab is our current page
        selectedTabIndex = pagerState.settledPage,
        contentColor = contentColor,
        containerColor = containerColor,
        // Good lord M3's default values for this are ugly
        edgePadding = 0.dp,
        divider = {},
        indicator = { tabPositions ->
          SecondaryIndicator(
            Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
            color = contentColor
          )
        },
      ) {
        // Add tabs for all of our pages
        val coroutineScope = rememberCoroutineScope()
        state.serviceMetas.forEachIndexed { index, serviceMeta ->
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
        beyondBoundsPageCount = 1,
        key = { state.serviceMetas[it].id },
        state = pagerState,
        verticalAlignment = Alignment.Top,
      ) { page ->
        contentComposed = true
        CompositionLocalProvider(
          LocalScrollToTop provides scrollToTop.takeIf { pagerState.currentPage == page },
          LocalServiceThemeColor provides colorCache[page],
        ) {
          CircuitContent(
            screen = ServiceScreen(state.serviceMetas[page].id),
            onNavEvent = { state.eventSink(HomeScreen.Event.NestedNavEvent(it)) }
          )
        }
      }
      ReportDrawnWhen { contentComposed }
    }
  }
}

@JvmInline
value class ColorCache(private val colors: Array<Color>) {
  operator fun get(index: Int): Color = colors[index]

  operator fun set(index: Int, color: Color) {
    colors[index] = color
  }
}

@Composable
fun rememberColorCache(
  serviceMetas: ImmutableList<ServiceMeta>,
  dayOnly: Boolean,
): ColorCache {
  val context = LocalContext.current
  val primaryColor = MaterialTheme.colorScheme.primary
  val dynamicTheme = LocalDynamicTheme.current
  return remember(context, serviceMetas) {
    val contextToUse =
      if (dayOnly) {
        val config =
          Configuration(context.resources.configuration).apply {
            uiMode =
              uiMode and Configuration.UI_MODE_NIGHT_MASK.inv() or Configuration.UI_MODE_NIGHT_NO
          }
        context.createConfigurationContext(config)
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
