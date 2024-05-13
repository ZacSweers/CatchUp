package catchup.deeplink

import android.content.Intent
import android.net.Uri
import catchup.di.AppScope
import catchup.di.SingleIn
import com.slack.circuit.runtime.screen.Screen
import com.squareup.anvil.annotations.ContributesBinding
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber

/**
 * A simple handler for deep links.
 *
 * @see parse for primary documentation.
 */
interface DeepLinkHandler {
  /**
   * Parses an [HttpUrl] into a list of [Screen]s that can be used as a backstack.
   *
   * Guaranteed to always have at least one element, returns null if the url is invalid.
   *
   * ## Example
   *
   * The given url `https://catchup.zacsweers.dev/home/settings/about/?tab=changelog` would resolve
   * to a list of [Screen]s like:
   * - `HomeScreen`
   * - `SettingsScreen`
   * - `AboutScreen` where its default tab is `ChangelogScreen`
   */
  fun parse(url: HttpUrl): ImmutableList<Screen>?
}

/**
 * Parses an [intent] into a list of [Screen]s using [Intent.getData] that can be used as a
 * backstack.
 *
 * Only used if the [Intent.getAction] is [Intent.ACTION_VIEW].
 */
fun DeepLinkHandler.parse(intent: Intent): ImmutableList<Screen>? {
  /*
    Example intent:
    adb shell am start \
    -W -a android.intent.action.VIEW \
    -d "catchup://catchup.zacsweers.dev/home/settings/about/?tab=changelog" dev.zacsweers.catchup
  */
  return intent.takeIf { intent.action == Intent.ACTION_VIEW }?.data?.toHttpUrl()?.let(::parse)
}

private fun Uri.toHttpUrl(): HttpUrl? {
  val uriString = toString()

  // Note HttpUrl doesn't support custom schemes, so we coerce it to https
  val scheme = scheme ?: "https"
  val httpSchemeString = uriString.replaceFirst(scheme, "https")

  return httpSchemeString.toHttpUrlOrNull()
}

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class DeepLinkHandlerImpl
@Inject
constructor(private val routes: @JvmSuppressWildcards Map<String, DeepLinkable>) : DeepLinkHandler {
  override fun parse(url: HttpUrl): ImmutableList<Screen> {
    val queryParams =
      url.queryParameterNames.associateWith { url.queryParameterValues(it) }.toImmutableMap()
    return buildList {
        for (segment in url.pathSegments) {

          // Not sure why these are sometimes blank
          if (segment.isBlank()) continue

          // Find a screen and add it
          val screen = screenFor(segment, queryParams)
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

  private fun screenFor(segment: String, queryParams: ImmutableMap<String, List<String?>>) =
    routes[segment]?.createScreen(queryParams)
}
