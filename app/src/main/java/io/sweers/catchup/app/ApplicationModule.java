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

package io.sweers.catchup.app;

import android.app.Application;
import android.content.Context;
import com.google.firebase.FirebaseApp;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import io.sweers.catchup.BuildConfig;
import io.sweers.catchup.injection.qualifiers.ApplicationContext;

@Module
public abstract class ApplicationModule {

  @Binds @ApplicationContext
  public abstract Context provideApplicationContext(Application application);

  @Provides
  public static FirebaseRemoteConfig provideRemoteConfig(@ApplicationContext Context context) {
    FirebaseApp.initializeApp(context);
    FirebaseRemoteConfig config = FirebaseRemoteConfig.getInstance();
    FirebaseRemoteConfigSettings configSettings =
        new FirebaseRemoteConfigSettings.Builder().setDeveloperModeEnabled(BuildConfig.DEBUG)
            .build();
    config.setConfigSettings(configSettings);
    return config;
  }
}
