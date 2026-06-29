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
package catchup.util.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

fun createFileShareIntent(context: Context, file: File, intentType: String): Intent {
  val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

  return Intent(Intent.ACTION_SEND).apply {
    type = intentType
    putExtra(Intent.EXTRA_STREAM, fileUri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
}
