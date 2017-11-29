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

package io.sweers.catchup.data

import android.content.Context
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import io.sweers.catchup.data.gemoji.GemojiDao
import io.sweers.catchup.data.gemoji.GemojiDatabase
import io.sweers.catchup.injection.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
internal object GithubEmojiModule {

  @Provides
  @JvmStatic
  @Singleton
  internal fun provideGemojiDatabase(@ApplicationContext context: Context, moshi: Moshi):
      GemojiDatabase {
    return GemojiDatabase(context, "gemoji.db", moshi)
  }

  @Provides
  @JvmStatic
  @Singleton
  internal fun provideGemojiDao(gemojiDatabase: GemojiDatabase): GemojiDao {
    return gemojiDatabase.GemojiDao()
  }
}
