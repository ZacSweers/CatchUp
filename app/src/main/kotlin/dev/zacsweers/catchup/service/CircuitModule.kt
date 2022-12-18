package dev.zacsweers.catchup.service

import androidx.lifecycle.ViewModel
import com.slack.circuit.CircuitConfig
import com.slack.circuit.Presenter
import com.slack.circuit.Ui
import com.slack.circuit.backstack.BackStackRecordLocalProviderViewModel
import com.slack.circuit.retained.Continuity
import com.squareup.anvil.annotations.ContributesTo
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.Multibinds
import dev.zacsweers.catchup.di.AppScope
import io.sweers.catchup.base.ui.ViewModelKey

@ContributesTo(AppScope::class)
@Module
interface CircuitModule {
  @Multibinds fun presenterFactories(): Set<Presenter.Factory>

  @Multibinds fun viewFactories(): Set<Ui.Factory>

  @ViewModelKey(BackStackRecordLocalProviderViewModel::class)
  @IntoMap
  @Binds
  fun BackStackRecordLocalProviderViewModel.bindBackStackRecordLocalProviderViewModel(): ViewModel

  @ViewModelKey(Continuity::class) @IntoMap @Binds fun Continuity.bindContinuity(): ViewModel

  companion object {
    @Provides
    fun provideBackStackRecordLocalProviderViewModel(): BackStackRecordLocalProviderViewModel {
      return BackStackRecordLocalProviderViewModel()
    }

    @Provides
    fun provideContinuity(): Continuity {
      return Continuity()
    }

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
