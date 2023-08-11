package dev.zacsweers.catchup

import android.content.Intent
import android.net.Uri
import com.slack.circuit.runtime.Screen
import com.squareup.anvil.annotations.ContributesBinding
import dev.zacsweers.catchup.di.AppScope
import dev.zacsweers.catchup.di.SingleIn
import io.sweers.catchup.home.HomeScreen
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber

interface DeepLinkable {
  fun createScreen(queryParams: Map<String, String?>): Screen?
}

interface DeepLinkHandler {
  // "catchup://home/settings/about/?tab=changelog"
  fun parse(uri: Uri): ImmutableList<Screen>
}

fun DeepLinkHandler.parse(intent: Intent): ImmutableList<Screen> {
  // -a android.intent.action.VIEW -d "catchup://home/settings/about/?tab=changelog"
  // io.sweers.catchup
  return intent.takeIf { intent.action == Intent.ACTION_VIEW }?.data?.let(::parse)
    ?: persistentListOf()
}

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class DeepLinkHandlerImpl
@Inject
constructor(private val routes: @JvmSuppressWildcards Map<String, DeepLinkable>) : DeepLinkHandler {
  override fun parse(uri: Uri): ImmutableList<Screen> {
    val queryParams = uri.queryParameterNames.associateWith { uri.getQueryParameter(it) }
    return buildList {
        // TODO Home doesn't actually appear in the uri segemnts since it's considered the domain
        add(HomeScreen)
        for (segment in uri.pathSegments) {
          val screen = routeFor(segment, queryParams)
          if (screen != null) {
            add(screen)
          } else {
            // TODO if any segments are null should we just break and return home?
            Timber.w("Unknown path segment $segment")
          }
        }
      }
      .toImmutableList()
  }

  private fun routeFor(segment: String, queryParams: Map<String, String?>) =
    routes[segment]?.createScreen(queryParams)
}
