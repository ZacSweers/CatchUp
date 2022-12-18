package dev.zacsweers.catchup.service

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.squareup.anvil.annotations.ContributesBinding
import dev.zacsweers.catchup.di.AppScope
import io.sweers.catchup.base.ui.ViewModelAssistedFactory
import javax.inject.Inject
import javax.inject.Provider

/** A factory that will provide [ViewModels][ViewModel] using their Dagger provider. */
@ContributesBinding(AppScope::class)
class CatchUpViewModelProviderFactory
@Inject
constructor(
  private val assistedProviders:
    Map<Class<out ViewModel>, @JvmSuppressWildcards ViewModelAssistedFactory<out ViewModel>>,
  private val modelProviders: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>
) : ViewModelProvider.Factory {

  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    val modelProvider =
      modelProviders[modelClass]
        ?: throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    @Suppress("UNCHECKED_CAST") return modelProvider.get() as T
  }

  override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
    val modelProvider =
      assistedProviders[modelClass]
        ?: throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    @Suppress("UNCHECKED_CAST") return modelProvider.create(extras.createSavedStateHandle()) as T
  }
}
