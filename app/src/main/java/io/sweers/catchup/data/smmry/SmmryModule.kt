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

package io.sweers.catchup.data.smmry

import com.bluelinelabs.conductor.Controller
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjector
import dagger.multibindings.IntoMap
import io.sweers.catchup.BuildConfig
import io.sweers.catchup.injection.ControllerKey
import io.sweers.catchup.ui.controllers.SmmryController
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

@Module(subcomponents = arrayOf(SmmryController.Component::class))
abstract class SmmryModule {
  @Binds
  @IntoMap
  @ControllerKey(SmmryController::class)
  internal abstract fun bindSmmryControllerInjectorFactory(
      builder: SmmryController.Component.Builder): AndroidInjector.Factory<out Controller>

  @Module
  companion object {

    @Provides @JvmStatic internal fun provideSmmryService(client: Lazy<OkHttpClient>,
        moshi: Moshi,
        rxJavaCallAdapterFactory: RxJava2CallAdapterFactory): SmmryService {
      return Retrofit.Builder().baseUrl(SmmryService.ENDPOINT)
          .callFactory { request ->
            client.get()
                .newCall(request)
          }
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .validateEagerly(BuildConfig.DEBUG)
          .build()
          .create(SmmryService::class.java)
    }
  }
}
