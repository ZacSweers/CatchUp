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

package io.sweers.catchup.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.f2prateek.rx.preferences.RxSharedPreferences;
import com.squareup.moshi.ArrayMapJsonAdapter;
import com.squareup.moshi.Moshi;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import io.reactivex.schedulers.Schedulers;
import io.sweers.catchup.data.adapters.ArrayCollectionJsonAdapter;
import io.sweers.catchup.data.adapters.UnescapeJsonAdapter;
import io.sweers.catchup.injection.qualifiers.ApplicationContext;
import io.sweers.catchup.injection.qualifiers.NetworkInterceptor;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

/**
 * I was never able to get this working with dagger in kotlin ಠ_ಠ. It always
 */
@Module
public abstract class DataModule {

  private static final Interceptor NOOP_INTERCEPTOR = chain -> chain.proceed(chain.request());
  private static final long HTTP_RESPONSE_CACHE = 10 * 1024 * 1024;
  private static final int HTTP_TIMEOUT_S = 30;

  @Provides static Cache provideCache(@ApplicationContext Context context) {
    // Temporary pending https://github.com/apollographql/apollo-android/pull/421
    //if (Looper.myLooper() == Looper.getMainLooper()) {
    //  throw new IllegalStateException("Cache initialized on main thread.");
    //}
    return new Cache(context.getCacheDir(), HTTP_RESPONSE_CACHE);
  }

  @Provides static OkHttpClient provideOkHttpClient(Cache cache,
      Set<Interceptor> interceptors,
      @NetworkInterceptor Set<Interceptor> networkInterceptors) {
    // Temporary pending https://github.com/apollographql/apollo-android/pull/421
    //if (Looper.myLooper() == Looper.getMainLooper()) {
    //  throw new IllegalStateException("HTTP client initialized on main thread.");
    //}

    OkHttpClient.Builder builder =
        new OkHttpClient.Builder().connectTimeout(HTTP_TIMEOUT_S, TimeUnit.SECONDS)
            .readTimeout(HTTP_TIMEOUT_S, TimeUnit.SECONDS)
            .writeTimeout(HTTP_TIMEOUT_S, TimeUnit.SECONDS)
            .cache(cache);

    builder.networkInterceptors()
        .addAll(networkInterceptors);
    builder.interceptors()
        .addAll(interceptors);

    return builder.build();
  }

  @Provides static Moshi provideMoshi() {
    return new Moshi.Builder().add(AutoValueMoshiAdapterFactory.create())
        .add(UnescapeJsonAdapter.FACTORY)
        .add(ArrayMapJsonAdapter.FACTORY)
        .add(ArrayCollectionJsonAdapter.FACTORY)
        .build();
  }

  @Provides static RxJava2CallAdapterFactory provideRxJavaCallAdapterFactory() {
    return RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io());
  }

  @Provides
  public static SharedPreferences provideSharedPreferences(@ApplicationContext Context context) {
    return context.getSharedPreferences("catchup", Context.MODE_PRIVATE);
  }

  @Provides
  public static RxSharedPreferences provideRxSharedPreferences(SharedPreferences sharedPreferences) {
    return RxSharedPreferences.create(sharedPreferences);
  }

  /**
   * Stub to force multibindings to at least one element, which allows for potentially empty variant
   * provides
   */
  @Provides @NetworkInterceptor @IntoSet static Interceptor provideStubNetworkInterceptor() {
    return NOOP_INTERCEPTOR;
  }

  /**
   * Stub to force multibindings to at least one element, which allows for potentially empty variant
   * provides
   */
  @Provides @IntoSet static Interceptor provideStubInterceptor() {
    return NOOP_INTERCEPTOR;
  }
}
