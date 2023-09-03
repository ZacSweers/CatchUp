/*
 * Copyright (C) 2019. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package catchup.appconfig

import androidx.annotation.ChecksSdkIntAtLeast

interface AppConfig {
  val isDebug: Boolean
  val applicationId: String
  val buildType: String
  val versionCode: Long
  val versionName: String
  val timestamp: String
  val sdkInt: Int
  val metadata: Map<Any, Any?>
}

interface EmptyAppConfig : AppConfig {
  override val isDebug: Boolean
    get() = throw NotImplementedError()

  override val applicationId: String
    get() = throw NotImplementedError()

  override val buildType: String
    get() = throw NotImplementedError()

  override val versionCode: Long
    get() = throw NotImplementedError()

  override val versionName: String
    get() = throw NotImplementedError()

  override val timestamp: String
    get() = throw NotImplementedError()

  override val sdkInt: Int
    get() = throw NotImplementedError()

  override val metadata: Map<Any, Any?>
    get() = throw NotImplementedError()
}

@Suppress("unused")
fun <T> AppConfig.requireMetadata(key: Any): T {
  return requireNotNull(readMetadata(key))
}

@Suppress("UNCHECKED_CAST", "unused")
fun <T> AppConfig.readMetadata(key: Any): T? {
  return metadata[key] as T?
}

@ChecksSdkIntAtLeast(parameter = 1) // Parameter 1 because the receiver is parameter 0
fun AppConfig.isSdkAtLeast(version: Int): Boolean {
  return sdkInt >= version
}

// Parameter 1 because the receiver is parameter 0
@ChecksSdkIntAtLeast(parameter = 1, lambda = 2)
inline fun <T> AppConfig.sdk(level: Int, func: () -> T): T? {
  return if (sdkInt >= level) {
    func()
  } else {
    null
  }
}
