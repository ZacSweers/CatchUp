package catchup.app

import android.app.Activity
import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provider
import dev.zacsweers.metro.Provides
import kotlin.reflect.KClass

@DependencyGraph(AppScope::class)
interface AppGraph {

  val activityProviders: Map<KClass<out Activity>, Provider<Activity>>

  fun inject(application: CatchUpApplication)

  @DependencyGraph.Factory
  fun interface Factory {
    fun create(@Provides application: Application): AppGraph
  }
}
