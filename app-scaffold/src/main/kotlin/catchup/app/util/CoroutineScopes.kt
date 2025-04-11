package catchup.app.util

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob

@SingleIn(AppScope::class) @Inject class MainAppCoroutineScope : CoroutineScope by MainScope()

@SingleIn(AppScope::class)
@Inject
class BackgroundAppCoroutineScope :
  CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Default)
