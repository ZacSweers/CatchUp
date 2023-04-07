package dev.zacsweers.catchup.service

import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.slack.circuit.foundation.CircuitConfig
import com.slack.circuit.foundation.LocalCircuitConfig
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Screen
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import dagger.multibindings.Multibinds
import dev.zacsweers.catchup.di.AppScope

@ContributesTo(AppScope::class)
@Module
interface CircuitModule {
  @Multibinds fun presenterFactories(): Set<Presenter.Factory>

  @Multibinds fun viewFactories(): Set<Ui.Factory>

  companion object {
    @Provides
    fun provideCircuit(
      presenterFactories: @JvmSuppressWildcards Set<Presenter.Factory>,
      uiFactories: @JvmSuppressWildcards Set<Ui.Factory>,
    ): CircuitConfig {
      return CircuitConfig.Builder()
        .apply {
          for (factory in presenterFactories) {
            addPresenterFactory(factory)
          }
          for (factory in uiFactories) {
            addUiFactory(factory)
          }
        }
        .setOnUnavailableContent { screen, modifier ->
          val navigator =
            object : Navigator {
              override fun goTo(screen: Screen) {}

              override fun pop(): Screen? {
                return null
              }

              override fun resetRoot(newRoot: Screen): List<Screen> {
                return emptyList()
              }
            }
          BasicText(
            """
              Route not available: ${screen.javaClass.name}.
              Presenter: ${LocalCircuitConfig.current?.presenter(screen, navigator)?.javaClass}
              UI: ${LocalCircuitConfig.current?.ui(screen)?.javaClass}
              """
              .trimIndent(),
            modifier.background(Color.Red),
            style = TextStyle(color = Color.Yellow)
          )
        }
        .build()
    }
  }
}
