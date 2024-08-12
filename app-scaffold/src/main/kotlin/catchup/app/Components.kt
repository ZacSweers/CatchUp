package catchup.app

import android.app.Activity
import android.app.Application
import catchup.di.AppScope
import catchup.di.SingleIn
import com.squareup.anvil.annotations.MergeComponent
import dagger.BindsInstance
import javax.inject.Provider

@MergeComponent(AppScope::class)
@SingleIn(AppScope::class)
interface ApplicationComponent {

  val activityProviders: Map<Class<out Activity>, @JvmSuppressWildcards Provider<Activity>>

  fun inject(application: CatchUpApplication)

  @MergeComponent.Factory
  fun interface Factory {
    fun create(@BindsInstance application: Application): ApplicationComponent
  }
}
