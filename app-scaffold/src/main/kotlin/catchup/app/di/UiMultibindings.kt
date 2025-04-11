package catchup.app.di

import android.app.Activity
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds

@ContributesTo(AppScope::class)
interface UiMultibindings {
  @Multibinds fun activityProviders(): Map<Class<out Activity>, Activity>
}
