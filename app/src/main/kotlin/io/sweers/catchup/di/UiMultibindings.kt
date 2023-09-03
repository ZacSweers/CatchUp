package io.sweers.catchup.di

import android.app.Activity
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.multibindings.Multibinds
import catchup.di.AppScope

@ContributesTo(AppScope::class)
@Module
interface UiMultibindings {
  @Multibinds fun activityProviders(): Map<Class<out Activity>, @JvmSuppressWildcards Activity>
}
