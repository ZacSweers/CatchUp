package io.sweers.catchup.app

import android.app.Activity
import android.app.Application
import com.squareup.anvil.annotations.MergeComponent
import dagger.BindsInstance
import dagger.Component
import dev.zacsweers.catchup.di.AppScope
import dev.zacsweers.catchup.di.SingleIn
import javax.inject.Provider

@MergeComponent(AppScope::class)
@SingleIn(AppScope::class)
interface ApplicationComponent {

  val activityProviders: Map<Class<out Activity>, @JvmSuppressWildcards Provider<Activity>>

  fun inject(application: CatchUpApplication)

  @Component.Factory
  fun interface Factory {
    fun create(@BindsInstance application: Application): ApplicationComponent
  }
}
