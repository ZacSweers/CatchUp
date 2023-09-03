package catchup.app.di

import android.app.Activity
import catchup.di.AppScope
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.multibindings.Multibinds

@ContributesTo(AppScope::class)
@Module
interface UiMultibindings {
  @Multibinds fun activityProviders(): Map<Class<out Activity>, @JvmSuppressWildcards Activity>
}
