package dev.zacsweers.catchup.service

import com.slack.circuit.CircuitConfig
import com.slack.circuit.Presenter
import com.slack.circuit.Ui
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
        .build()
    }
  }
}
