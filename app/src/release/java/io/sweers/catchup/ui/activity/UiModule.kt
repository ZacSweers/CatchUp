package io.sweers.catchup.ui.activity

import dagger.Module
import dagger.Provides
import io.sweers.catchup.ui.ViewContainer

@Module
object UiModule {
  @Provides @JvmStatic internal fun provideViewContainer(): ViewContainer {
    return ViewContainer.DEFAULT
  }
}
