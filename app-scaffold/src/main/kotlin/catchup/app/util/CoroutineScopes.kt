package catchup.app.util

import catchup.di.AppScope
import catchup.di.SingleIn
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob

@SingleIn(AppScope::class)
class MainAppCoroutineScope @Inject constructor() : CoroutineScope by MainScope()

@SingleIn(AppScope::class)
class BackgroundAppCoroutineScope @Inject constructor() :
  CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Default)
