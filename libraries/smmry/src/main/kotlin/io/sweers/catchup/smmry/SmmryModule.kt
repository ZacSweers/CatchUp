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
package io.sweers.catchup.smmry

import android.content.Context
import androidx.room.Room
import com.squareup.moshi.Moshi
import dagger.Lazy
import dagger.Module
import dagger.Provides
import io.sweers.catchup.libraries.retrofitconverters.delegatingCallFactory
import io.sweers.catchup.smmry.model.SmmryDao
import io.sweers.catchup.smmry.model.SmmryDatabase
import io.sweers.catchup.smmry.model.SmmryResponseFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Qualifier

@Module
object SmmryModule {

  @Qualifier
  annotation class ForSmmry

  @Provides
  @JvmStatic
  @ForSmmry
  internal fun provideSmmryMoshi(moshi: Moshi): Moshi {
    return moshi.newBuilder()
        .add(SmmryResponseFactory.getInstance())
        .build()
  }

  @Provides
  @JvmStatic
  internal fun provideSmmryService(
    client: Lazy<OkHttpClient>,
    @ForSmmry moshi: Moshi
  ): SmmryService {
    return Retrofit.Builder().baseUrl(SmmryService.ENDPOINT)
        .delegatingCallFactory(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
//        .validateEagerly(BuildConfig.DEBUG) // TODO can't do this in libraries
        .build()
        .create(SmmryService::class.java)
  }

  @Provides
  @JvmStatic
  internal fun provideDatabase(context: Context): SmmryDatabase {
    return Room.databaseBuilder(context.applicationContext,
        SmmryDatabase::class.java,
        "smmry.db")
        .fallbackToDestructiveMigration()
        .build()
  }

  @Provides
  @JvmStatic
  internal fun provideSmmryDao(database: SmmryDatabase): SmmryDao = database.dao()
}
