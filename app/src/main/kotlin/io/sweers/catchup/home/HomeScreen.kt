package io.sweers.catchup.home

import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
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
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
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
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.slack.circuit.CircuitContent
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.NavEvent
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.onNavEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dev.zacsweers.catchup.di.AppScope
import dev.zacsweers.catchup.service.ServiceScreen
import io.sweers.catchup.CatchUpPreferences
import io.sweers.catchup.changes.ChangelogHelper
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.ui.activity.SettingsScreen
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
object HomeScreen : Screen {
  data class State(
    val serviceMetas: ImmutableList<ServiceMeta>,
    val eventSink: (Event) -> Unit = {}
  ) : CircuitUiState
  sealed interface Event : CircuitUiEvent {
    object OpenSettings : Event
    data class NestedNavEvent(val navEvent: NavEvent) : Event
  }
}

class HomePresenter
@AssistedInject
constructor(
  @Assisted private val navigator: Navigator,
  private val serviceMetaMap: Map<String, ServiceMeta>,
  private val sharedPrefs: SharedPreferences,
  private val catchUpPreferences: CatchUpPreferences,
  // TODO bind with toolbar
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
    val serviceMetas =
      remember(currentOrder) {
        // TODO make enabledPrefKey live
        check(serviceMetaMap.isNotEmpty()) { "No services found!" }
        serviceMetaMap.values
          .filter(ServiceMeta::enabled)
          .filter { sharedPrefs.getBoolean(it.enabledPreferenceKey, true) }
          .sortedBy { currentOrder.indexOf(it.id) }
          .toImmutableList()
      }

    return HomeScreen.State(
      serviceMetas = serviceMetas,
    ) { event ->
      when (event) {
        HomeScreen.Event.OpenSettings -> {
          navigator.goTo(SettingsScreen)
        }
        is HomeScreen.Event.NestedNavEvent -> {
          navigator.onNavEvent(event.navEvent)
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
  val eventSink = state.eventSink
  val pagerState = key(state.serviceMetas) { rememberPagerState() }
  val currentServiceMeta = state.serviceMetas[pagerState.currentPage]
  val title = stringResource(currentServiceMeta.name)
  val systemUiController = rememberSystemUiController()

  val surfaceColor = MaterialTheme.colorScheme.surface
  // TODO this isn't updating when serviceMeta order changes
  val colorCache = rememberColorCache(state.serviceMetas)
  val tabLayoutColor = remember(colorCache) { Animatable(colorCache[0]) }
  var isAnimatingColor by remember { mutableStateOf(false) }
  var scrimColor by remember { mutableStateOf(surfaceColor) }

  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
  LaunchedEffect(scrollBehavior) {
    snapshotFlow { scrollBehavior.state.collapsedFraction }
      .collect { fraction ->
        scrimColor =
          lerp(surfaceColor, tabLayoutColor.value, scrollBehavior.state.collapsedFraction)
        if (fraction == 1.0f) {
          systemUiController.statusBarDarkContentEnabled = !scrimColor.isDark
        } else {
          systemUiController.statusBarDarkContentEnabled = !surfaceColor.isDark
        }
      }
  }
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
        val color = lerp(colorCache[settledPosition], colorCache[targetPage], adjustedOffset)
        tabLayoutColor.animateTo(color, tween(0))
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
          IconButton(
            onClick = { eventSink(HomeScreen.Event.OpenSettings) },
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
      ScrollableTabRow(
        // Our selected tab is our current page
        selectedTabIndex = pagerState.settledPage,
        contentColor = Color.White,
        containerColor = tabLayoutColor.value,
        // Good lord M3's default values for this are ugly
        edgePadding = 0.dp,
        divider = {},
        indicator = { tabPositions ->
          TabRowDefaults.Indicator(
            Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
            color = Color.White
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
                tint = Color.White,
              )
            },
            selected = pagerState.currentPage == index,
            onClick = {
              if (index != pagerState.currentPage) {
                coroutineScope.launch {
                  isAnimatingColor = true
                  awaitAll(
                    async { pagerState.animateScrollToPage(index) },
                    async { tabLayoutColor.animateTo(colorCache[index]) },
                  )
                  isAnimatingColor = false
                }
              }
            },
          )
        }
      }
      HorizontalPager(
        modifier = Modifier.weight(1f),
        pageCount = state.serviceMetas.size,
        beyondBoundsPageCount = 1,
        key = { state.serviceMetas[it].id },
        state = pagerState,
        verticalAlignment = Alignment.Top,
        // Explicitly defined to cover for https://issuetracker.google.com/issues/264729364
        pageNestedScrollConnection =
          PagerDefaults.pageNestedScrollConnection(Orientation.Horizontal)
      ) { page ->
        CircuitContent(
          screen = ServiceScreen(state.serviceMetas[page].id),
          onNavEvent = { eventSink(HomeScreen.Event.NestedNavEvent(it)) }
        )
      }
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
fun rememberColorCache(serviceMetas: ImmutableList<ServiceMeta>): ColorCache {
  val context = LocalContext.current
  return remember(context, serviceMetas) {
    val dayOnlyContext =
      context.createConfigurationContext(
        Configuration().apply { uiMode = Configuration.UI_MODE_NIGHT_NO }
      )
    val colors =
      Array(serviceMetas.size) { index ->
        val color = dayOnlyContext.getColor(serviceMetas[index].themeColor)
        Color(color)
      }
    ColorCache(colors)
  }
}

private val Color.isDark: Boolean
  get() = luminance() <= 0.5f
