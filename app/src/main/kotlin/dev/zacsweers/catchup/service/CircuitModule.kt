package dev.zacsweers.catchup.service

import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.LocalCircuit
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
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
    ): Circuit {
      return Circuit.Builder()
        .addPresenterFactories(presenterFactories)
        .addUiFactories(uiFactories)
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
          val circuit = LocalCircuit.current
          BasicText(
            """
              Route not available: ${screen.javaClass.name}.
              Presenter: ${circuit?.presenter(screen, navigator)?.javaClass}
              UI: ${circuit?.ui(screen)?.javaClass}
              All presenterFactories: ${circuit?.newBuilder()?.presenterFactories}
              All uiFactories: ${circuit?.newBuilder()?.uiFactories}
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
