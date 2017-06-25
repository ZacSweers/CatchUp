/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.util

import android.content.Context
import timber.log.Timber
import java.io.File

fun Context.clearCache(): Long {
  return cleanDir(applicationContext.cacheDir)
}

private fun cleanDir(dir: File): Long {
  var bytesDeleted: Long = 0
  val files = dir.listFiles()

  for (file in files) {
    val length = file.length()
    if (file.delete()) {
      Timber.d("Deleted file")
      bytesDeleted += length
    }
  }
  return bytesDeleted
}
