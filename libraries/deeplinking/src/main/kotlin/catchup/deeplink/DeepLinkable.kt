/*
 * Copyright (C) 2026. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package catchup.deeplink

import com.slack.circuit.runtime.screen.Screen
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.StringKey
import kotlinx.collections.immutable.ImmutableMap
import okhttp3.HttpUrl

/**
 * Marker interface to indicate that a given [Screen] can be deep linked to.
 *
 * For object screens, they can simply implement this and return themselves as an instance.
 *
 * ## Configuration
 *
 * ```kotlin
 * @ContributesMultibinding(AppScope::class, binding = binding<DeepLinkable>)
 * @StringKey("home") // The segment/route
 * @Parcelize
 * object HomeScreen : Screen, DeepLinkable {
 *   override fun createScreen(queryParams: ImmutableMap<String, List<String?>): Screen = HomeScreen
 * }
 * ```
 *
 * For class screens (e.g. `data class`), they can define a nested object that implements this and
 * creates the instance to return.
 *
 * ```kotlin
 * @Parcelize
 * data class AboutScreen(val selectedTab: AboutScreenComponent = AboutScreenComponent.DEFAULT) : Screen {
 *   @ContributesMultibinding(AppScope::class)
 *   @StringKey("about")
 *   object DeepLinker : DeepLinkable {
 *     override fun createScreen(queryParams: ImmutableMap<String, List<String?>) =
 *       AboutScreen(AboutScreenComponent.componentFor(queryParams["tab"]))
 *   }
 * }
 * ```
 *
 * ## Contributing to DI
 *
 * To contribute a [DeepLinkable] to DI, use a combination of [ContributesIntoMap] and [StringKey]
 * annotations. The key should be the path segment this screen is associated with.
 *
 * Example:
 * ```kotlin
 * @ContributesIntoMap(AppScope::class, binding = binding<DeepLinkable>)
 * @StringKey("home")
 * @Parcelize
 * object HomeScreen : Screen, DeepLinkable
 * ```
 */
interface DeepLinkable {
  /**
   * Creates a [Screen] instance for this [DeepLinkable]. The [queryParams] are offered from the
   * original [HttpUrl] and can be used to customize creation of the screen.
   *
   * Note that the values of the query params are a list of _all_ values for that key found in the
   * url. For example, `https://catchup.zacsweers.dev/home/settings/about/?tab=changelog&tab=foo`
   * would have a `tab` key with values `["changelog", "foo"]`.
   */
  fun createScreen(queryParams: ImmutableMap<String, List<String?>>): Screen?
}
