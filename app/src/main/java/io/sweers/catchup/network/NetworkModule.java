package io.sweers.catchup.network;

import android.os.Looper;

import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.squareup.moshi.Moshi;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.sweers.catchup.app.CatchUpApplication;
import io.sweers.catchup.model.HackerNewsStory;
import okhttp3.Cache;
import okhttp3.OkHttpClient;

@Module
public class NetworkModule {

  private static final long HTTP_RESPONSE_CACHE = 10 * 1024 * 1024;
  private static final int HTTP_TIMEOUT_S = 30;

  @Provides @Singleton Cache provideCache(CatchUpApplication application) {
    return new Cache(application.getCacheDir(), HTTP_RESPONSE_CACHE);
  }

  @Provides @Singleton OkHttpClient provideOkHttpClient(Cache cache) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      throw new IllegalStateException("HTTP client initialized on main thread.");
    }

    OkHttpClient.Builder builder = new OkHttpClient.Builder()
        .connectTimeout(HTTP_TIMEOUT_S, TimeUnit.SECONDS)
        .readTimeout(HTTP_TIMEOUT_S, TimeUnit.SECONDS)
        .writeTimeout(HTTP_TIMEOUT_S, TimeUnit.SECONDS)
        .cache(cache);

    configureOkHttpClientForVariant(builder);
    return builder.build();
  }

  protected void configureOkHttpClientForVariant(OkHttpClient.Builder builder) {
    // Override in variants
    builder.addNetworkInterceptor(new StethoInterceptor());
  }

  @Provides @Singleton Moshi provideMoshi() {
    return new Moshi.Builder()
        .add(HackerNewsStory.typeAdapterFactory())
        .build();
  }

}
