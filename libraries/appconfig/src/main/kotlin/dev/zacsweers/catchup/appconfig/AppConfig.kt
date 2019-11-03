/*
 * Copyright (c) 2019 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.zacsweers.catchup.appconfig

interface AppConfig {
  val isDebug: Boolean
  val applicationId: String
  val buildType: String
  val flavor: String
  val versionCode: Long
  val versionName: String
  val timestamp: String
  val metadata: Map<Any, Any?>
}

fun <T> AppConfig.requireMetadata(key: Any): T {
  return requireNotNull(readMetadata(key))
}

@Suppress("UNCHECKED_CAST")
fun <T> AppConfig.readMetadata(key: Any): T? {
  return metadata[key] as T?
}
