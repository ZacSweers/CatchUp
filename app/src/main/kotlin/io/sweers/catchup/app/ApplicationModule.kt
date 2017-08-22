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

package io.sweers.catchup.app

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import dagger.Binds
import dagger.Module
import dagger.Provides
import io.sweers.catchup.BuildConfig
import io.sweers.catchup.R
import io.sweers.catchup.injection.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
abstract class ApplicationModule {

  @Binds
  @ApplicationContext
  @Singleton
  abstract fun provideApplicationContext(application: Application): Context

  @Module
  companion object {

    @Provides
    @JvmStatic
    @Singleton
    fun provideRemoteConfig(@ApplicationContext context: Context): FirebaseRemoteConfig {
      // Ugly but a hack to detect if we have firebase configs available. If not, we give it a shell
      // of options so the app still runs.
      val res = context.resources
      val packageId = res.getResourcePackageName(R.string.common_google_play_services_unknown_issue)
      val id = res.getIdentifier("google_app_id", "string", packageId)
      val firebaseOptions = if (id != 0 && res.getString(id) != null) {
        FirebaseOptions.fromResource(context)
      } else {
        FirebaseOptions.Builder()
            .setApplicationId(BuildConfig.APPLICATION_ID)
            .build()
      }
      FirebaseApp.initializeApp(context, firebaseOptions, BuildConfig.APPLICATION_ID)
      return FirebaseRemoteConfig.getInstance()
          .apply {
            setConfigSettings(FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build())
          }
    }
  }
}
