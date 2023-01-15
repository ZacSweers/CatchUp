package io.sweers.catchup.ui.about

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.slack.circuit.CircuitContent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.catchup.appconfig.AppConfig
import dev.zacsweers.catchup.di.AppScope
import io.sweers.catchup.R
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
object AboutScreen : Screen {
  data class State(val version: String) : CircuitUiState
}

@CircuitInject(AboutScreen::class, AppScope::class)
class AboutPresenter @Inject constructor(private val appConfig: AppConfig) :
  Presenter<AboutScreen.State> {
  @Composable override fun present() = AboutScreen.State(appConfig.versionName)
}

private val SCREENS = listOf(LicensesScreen, ChangelogScreen)
private val SCREEN_TITLES = intArrayOf(R.string.licenses, R.string.changelog)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@CircuitInject(AboutScreen::class, AppScope::class)
@Composable
fun About(state: AboutScreen.State) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    containerColor = Color.Transparent,
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = { CollapsingAboutHeader(state.version, scrollBehavior = scrollBehavior) }
  ) { paddingValues ->
    Column(Modifier.padding(paddingValues)) {
      val pagerState = rememberPagerState()
      TabRow(
        // Our selected tab is our current page
        selectedTabIndex = pagerState.currentPage,
      ) {
        // Add tabs for all of our pages
        val coroutinesScope = rememberCoroutineScope()
        SCREEN_TITLES.forEachIndexed { index, titleRes ->
          Tab(
            text = { Text(stringResource(titleRes)) },
            selected = pagerState.currentPage == index,
            onClick = {
              if (index != pagerState.currentPage) {
                coroutinesScope.launch { pagerState.animateScrollToPage(index) }
              }
            },
          )
        }
      }

      HorizontalPager(
        modifier = Modifier.weight(1f),
        pageCount = 2,
        key = { it },
        state = pagerState,
        verticalAlignment = Alignment.Top,
        // Explicitly defined to cover for https://issuetracker.google.com/issues/264729364
        pageNestedScrollConnection =
          PagerDefaults.pageNestedScrollConnection(Orientation.Horizontal)
      ) { page ->
        CircuitContent(SCREENS[page])
      }
    }
  }
}
