package io.sweers.catchup.ui.about

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.squareup.anvil.annotations.ContributesMultibinding
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.multibindings.StringKey
import catchup.appconfig.AppConfig
import catchup.deeplink.DeepLinkable
import catchup.di.AppScope
import io.sweers.catchup.R
import java.util.Locale
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber

@Parcelize
data class AboutScreen(val selectedTab: AboutScreenComponent = AboutScreenComponent.DEFAULT) :
  Screen {

  @ContributesMultibinding(AppScope::class, boundType = DeepLinkable::class)
  @StringKey("about")
  object DeepLinker : DeepLinkable {
    override fun createScreen(queryParams: ImmutableMap<String, List<String?>>) =
      AboutScreen(AboutScreenComponent.componentFor(queryParams["tab"]?.first()))
  }

  data class State(
    val initialPage: Int,
    val version: String,
  ) : CircuitUiState

  enum class AboutScreenComponent(
    val screen: Screen,
    @StringRes val titleRes: Int,
  ) {
    Licenses(LicensesScreen, R.string.licenses),
    Changelog(ChangelogScreen, R.string.changelog);

    companion object {
      internal val DEFAULT = Licenses

      fun componentFor(path: String?): AboutScreenComponent {
        return when (path?.lowercase(Locale.US)) {
          "licenses" -> Licenses
          "changelog" -> Changelog
          else -> {
            Timber.d("Unknown path $path, defaulting to $DEFAULT")
            DEFAULT
          }
        }
      }
    }
  }
}

class AboutPresenter
@AssistedInject
constructor(@Assisted val screen: AboutScreen, private val appConfig: AppConfig) :
  Presenter<AboutScreen.State> {
  @Composable
  override fun present() = AboutScreen.State(screen.selectedTab.ordinal, appConfig.versionName)

  @CircuitInject(AboutScreen::class, AppScope::class)
  @AssistedFactory
  interface Factory {
    fun create(screen: AboutScreen): AboutPresenter
  }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@CircuitInject(AboutScreen::class, AppScope::class)
@Composable
fun About(state: AboutScreen.State, modifier: Modifier = Modifier) {
  Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0, 0, 0, 0)) {
    paddingValues ->
    CollapsingAboutHeader(
      versionName = state.version,
      modifier = modifier.padding(paddingValues).fillMaxSize(),
    ) {
      Column {
        val components = remember { AboutScreen.AboutScreenComponent.entries.toImmutableList() }
        val pagerState = rememberPagerState(initialPage = state.initialPage) { 2 }
        TabRow(
          // Our selected tab is our current page
          selectedTabIndex = pagerState.currentPage,
        ) {
          // Add tabs for all of our pages
          val coroutinesScope = rememberCoroutineScope()
          components.forEach { component ->
            val index = component.ordinal
            val titleRes = component.titleRes
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
          key = { it },
          state = pagerState,
          verticalAlignment = Alignment.Top,
        ) { page ->
          CircuitContent(components[page].screen)
        }
      }
    }
  }
}
