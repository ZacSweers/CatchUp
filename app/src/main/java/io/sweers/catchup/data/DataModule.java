package io.sweers.catchup.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;

import com.f2prateek.rx.preferences.RxSharedPreferences;
import com.ryanharter.auto.value.moshi.AutoValueMoshiAdapterFactory;
import com.squareup.moshi.Moshi;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.sweers.catchup.injection.qualifiers.ApplicationContext;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import rx.schedulers.Schedulers;

@Module
public class DataModule {

  private static final long HTTP_RESPONSE_CACHE = 10 * 1024 * 1024;
  private static final int HTTP_TIMEOUT_S = 30;

  @Provides
  @Singleton
  Cache provideCache(@ApplicationContext Context context) {
    return new Cache(context.getCacheDir(), HTTP_RESPONSE_CACHE);
  }

  @Provides
  @Singleton
  OkHttpClient provideOkHttpClient(
      @ApplicationContext Context context,
      Cache cache) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      throw new IllegalStateException("HTTP client initialized on main thread.");
    }

    OkHttpClient.Builder builder = new OkHttpClient.Builder()
        .connectTimeout(HTTP_TIMEOUT_S, TimeUnit.SECONDS)
        .readTimeout(HTTP_TIMEOUT_S, TimeUnit.SECONDS)
        .writeTimeout(HTTP_TIMEOUT_S, TimeUnit.SECONDS)
        .cache(cache);

    configureOkHttpClientForVariant(context, builder);
    return builder.build();
  }

  protected void configureOkHttpClientForVariant(
      @ApplicationContext Context context,
      OkHttpClient.Builder builder) {
    // Override in variants
  }

  @Provides
  @Singleton
  Moshi provideMoshi() {
    return new Moshi.Builder()
        .add(new AutoValueMoshiAdapterFactory())
        .build();
  }

  @Provides
  @Singleton
  RxJavaCallAdapterFactory provideRxJavaCallAdapterFactory() {
    return RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io());
  }

  @Provides
  @Singleton
  public SharedPreferences provideSharedPreferences(@ApplicationContext Context context) {
    return context.getSharedPreferences("catchup", Context.MODE_PRIVATE);
  }

  @Provides
  @Singleton
  public RxSharedPreferences provideRxSharedPreferences(SharedPreferences sharedPreferences) {
    return RxSharedPreferences.create(sharedPreferences);
  }
}
