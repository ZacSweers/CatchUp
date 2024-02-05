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
package catchup.app.changes

import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import catchup.app.CatchUpPreferences
import catchup.appconfig.AppConfig
import catchup.di.AppScope
import catchup.di.SingleIn
import dev.zacsweers.catchup.app.scaffold.R
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Stable
@SingleIn(AppScope::class)
class ChangelogHelper
@Inject
constructor(
  // TODO datastore this
  private val catchUpPreferences: CatchUpPreferences,
  private val appConfig: AppConfig,
) {

  fun changelogAvailable(context: Context): Flow<Boolean> {
    return catchUpPreferences.lastVersion.map { lastVersion ->
      // Check if version name changed and if there's a changelog
      if (lastVersion != appConfig.versionName) {
        // Write the new version in
        catchUpPreferences.edit { it[CatchUpPreferences.Keys.lastVersion] = appConfig.versionName }
        if (lastVersion == null) {
          // This was the first load it seems, so ignore it
          return@map false
        } else if (context.getString(R.string.changelog_text).isNotEmpty()) {
          return@map true
        }
      }
      false
    }
  }

  @Composable
  fun Content(modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
      // TODO kinda gross but shrug
      val context = LocalContext.current
      val icon =
        remember(context) {
          (AppCompatResources.getDrawable(context, R.mipmap.ic_launcher) as AdaptiveIconDrawable)
            .toBitmap()
        }
      Image(
        bitmap = icon.asImageBitmap(),
        contentDescription = "CatchUp icon",
        modifier = Modifier.size(56.dp).align(Alignment.CenterHorizontally),
      )
      Text(
        appConfig.versionName,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
      )
      Spacer(Modifier.height(16.dp))
      // TODO parse markdown, make it clickable
      Text(
        stringResource(R.string.changelog_text),
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}
