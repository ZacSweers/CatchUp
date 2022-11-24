package io.sweers.catchup.di

import android.app.Activity
import androidx.fragment.app.Fragment
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.multibindings.Multibinds
import dev.zacsweers.catchup.di.AppScope

@ContributesTo(AppScope::class)
@Module
interface UiMultibindings {
  @Multibinds fun activityProviders(): Map<Class<out Activity>, @JvmSuppressWildcards Activity>
  @Multibinds fun fragmentProviders(): Map<Class<out Fragment>, @JvmSuppressWildcards Fragment>
}
